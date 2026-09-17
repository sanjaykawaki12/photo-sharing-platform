package com.trizenai.photoshare.dto;

import com.trizenai.photoshare.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    // =========================================================
    // REGISTER REQUEST
    // =========================================================

    public record RegisterRequest(

            @NotBlank
            String name,

            @Email
            @NotBlank
            String email,

            @Size(
                    min = 6,
                    message = "Password must be at least 6 characters"
            )
            String password,

            @NotBlank
            String role

    ) {}


    // =========================================================
    // LOGIN REQUEST
    // =========================================================

    public record LoginRequest(

            @Email
            @NotBlank
            String email,

            @NotBlank
            String password,

            @NotBlank(
                    message = "Please select a login role"
            )
            String role

    ) {}


    // =========================================================
    // AUTH RESPONSE
    // =========================================================

    public record AuthResponse(

            String token,

            Long userId,

            String name,

            String email,

            Role role

    ) {}
}