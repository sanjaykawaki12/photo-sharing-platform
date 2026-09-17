package com.trizenai.photoshare.service;

import com.trizenai.photoshare.config.JwtUtil;
import com.trizenai.photoshare.dto.AuthDtos.AuthResponse;
import com.trizenai.photoshare.dto.AuthDtos.LoginRequest;
import com.trizenai.photoshare.dto.AuthDtos.RegisterRequest;
import com.trizenai.photoshare.exception.ApiExceptions.BadRequestException;
import com.trizenai.photoshare.exception.ApiExceptions.ConflictException;
import com.trizenai.photoshare.model.Role;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    // =========================================================
    // REGISTER
    // =========================================================

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(
                    "An account with this email already exists"
            );
        }

        Role role;

        try {

            role = Role.valueOf(
                    request.role()
                            .trim()
                            .toUpperCase()
            );

        } catch (IllegalArgumentException ex) {

            throw new BadRequestException(
                    "Role must be either ADMIN or TEAM_MEMBER"
            );
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email().toLowerCase())
                .passwordHash(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .role(role)
                .build();

        user = userRepository.save(user);

        String token =
                jwtUtil.generateToken(
                        user.getEmail(),
                        user.getId(),
                        user.getRole().name()
                );

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public AuthResponse login(LoginRequest request) {

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.password()
                    )
            );

        } catch (BadCredentialsException ex) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid email or password"
                                )
                        );

        /*
         * IMPORTANT:
         *
         * Login role must match the role selected
         * on the login page.
         *
         * ADMIN account -> ADMIN login only
         * TEAM_MEMBER account -> TEAM_MEMBER login only
         */

        if (request.role() == null
                || request.role().isBlank()) {

            throw new BadRequestException(
                    "Please select a login role"
            );
        }

        Role selectedRole;

        try {

            selectedRole =
                    Role.valueOf(
                            request.role()
                                    .trim()
                                    .toUpperCase()
                    );

        } catch (IllegalArgumentException ex) {

            throw new BadRequestException(
                    "Role must be either ADMIN or TEAM_MEMBER"
            );
        }

        if (user.getRole() != selectedRole) {

            throw new BadCredentialsException(
                    "This account does not belong to the selected role"
            );
        }

        String token =
                jwtUtil.generateToken(
                        user.getEmail(),
                        user.getId(),
                        user.getRole().name()
                );

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}