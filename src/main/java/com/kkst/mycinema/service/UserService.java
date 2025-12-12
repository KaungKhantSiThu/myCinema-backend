package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.UpdateUserRequest;
import com.kkst.mycinema.dto.UserResponse;
import com.kkst.mycinema.exception.UserNotFoundException;
import com.kkst.mycinema.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateProfile(String email, UpdateUserRequest request) {
        log.info("Updating profile for user: {}", email);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        // Note: Phone number update skipped as it's not in the entity yet.
        // We can add it in a future migration.

        userRepository.save(user);
        log.info("Profile updated successfully for user: {}", email);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        log.info("Fetching current user info for: {}", email);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
