package est.projet.springroles.auth;

import est.projet.springroles.config.JwtService;
import est.projet.springroles.models.Role;
import est.projet.springroles.models.Token;
import est.projet.springroles.models.TokenType;
import est.projet.springroles.models.User;
import est.projet.springroles.repos.TokenRepo;
import est.projet.springroles.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepo userRepo;
    private final TokenRepo tokenRepo ;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest registerRequest){
        var user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())
                .build();
        var savedUser = userRepo.save(user);
        var jwtToken = jwtService.generateToken(user);
        savedUserToken(jwtToken, savedUser);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
    }



    public AuthenticationResponse authenticate(AuthenticationRequest registerRequest){
        System.out.println(registerRequest.getEmail() + "*" + registerRequest.getPassword() );
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getEmail(),
                        registerRequest.getPassword()
                )
        );
        var user = userRepo.findByEmail(registerRequest.getEmail()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        var jwtToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        savedUserToken(jwtToken , user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
    }

    private void savedUserToken(String jwtToken, User savedUser) {
        var token = Token.builder()
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .user(savedUser)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepo.save(token);
    }
    private void revokeAllUserTokens(User user){
        var validUserToken = tokenRepo.findAllValidTokenByUser(user.getId());

        if (validUserToken.isEmpty()){
            return;
        }
        validUserToken.forEach(t->{
            t.setRevoked(true);
            t.setExpired(true);
        });
        tokenRepo.saveAll(validUserToken);

    }
}
