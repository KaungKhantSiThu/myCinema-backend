package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "User registration request")
public record RegisterRequest(
                @NotBlank(message = "Name is required") @Size(min = 2, max = 100, message = "Name must be 2-100 characters") @Schema(description = "Full Name", example = "Jane Doe") String name,

                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Schema(description = "Email address", example = "jane.doe@example.com") String email,

                @NotBlank(message = "Password is required") @Size(min = 8, max = 128, message = "Password must be 8-128 characters") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)") @Schema(description = "Password (strong)", example = "P@ssw0rd123!") String password,

                // Optional role field - defaults to USER if not provided
                // Valid values: "USER", "ADMIN"
                @Schema(description = "Role (optional, defaults to USER)", example = "USER") String role) {
}
