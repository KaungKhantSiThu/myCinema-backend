package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.HallResponse;
import com.kkst.mycinema.service.HallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/halls")
@RequiredArgsConstructor
@Tag(name = "Halls", description = "Hall management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class HallController {

    private final HallService hallService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all halls", description = "Returns a list of all halls (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Halls retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HallResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<HallResponse>> getAllHalls() {
        return ResponseEntity.ok(hallService.getAllHalls());
    }
}
