package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.UpdateUserRequest;
import com.kkst.mycinema.dto.UserResponse;
import com.kkst.mycinema.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Returns information about the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User info retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        var userEmail = authentication.getName();
        var userResponse = userService.getCurrentUser(userEmail);
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Updates user profile information (Name)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, String>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        var userEmail = authentication.getName();
        userService.updateProfile(userEmail, request);

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }
}
