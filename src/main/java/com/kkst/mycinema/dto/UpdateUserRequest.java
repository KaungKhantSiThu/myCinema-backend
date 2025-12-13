package com.kkst.mycinema.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to update user profile")
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "New full name", example = "John Smith")
    private String name;

    @Size(min = 6, max = 20, message = "Phone must be between 6 and 20 characters")
    @Schema(description = "New phone number", example = "+1234567890")
    private String phoneNumber;
}
