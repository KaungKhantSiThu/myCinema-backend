package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.CreateShowRequest;
import com.kkst.mycinema.dto.ShowResponse;
import com.kkst.mycinema.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shows") // NOTE: Added /api prefix to match other controllers
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin - Show Management", description = "APIs for managing shows")
public class AdminShowController {

    private final AdminService adminService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new show")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Show created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request params"),
            @ApiResponse(responseCode = "409", description = "Show overlaps with existing show"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createShow(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing show")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Show updated successfully"),
            @ApiResponse(responseCode = "404", description = "Show not found"),
            @ApiResponse(responseCode = "409", description = "Show overlaps with existing show"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ShowResponse> updateShow(@PathVariable Long id,
            @Valid @RequestBody CreateShowRequest request) {
        return ResponseEntity.ok(adminService.updateShow(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a show")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Show deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Show not found"),
            @ApiResponse(responseCode = "409", description = "Show has confirmed bookings"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        adminService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
