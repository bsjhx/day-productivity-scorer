package com.bsjhx.dayproductivityscore.api.rest;

import com.bsjhx.dayproductivityscore.domain.User;
import com.bsjhx.dayproductivityscore.infrastructure.security.JwtTokenProvider;
import com.bsjhx.dayproductivityscore.infrastructure.security.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Profile("!test")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        System.out.println("Login attempt: " + request.username());

        User user = userRepository.findByUsername(request.username())
                .orElse(null);

        if (user == null) {
            System.out.println("User not found: " + request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        System.out.println("User found: " + user.getUsername() + ", checking password...");
        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPassword());
        System.out.println("Password matches: " + passwordMatches);

        if (!passwordMatches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRoles());
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername()));
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token, String username) {}
}
