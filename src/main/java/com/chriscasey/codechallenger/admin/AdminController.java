package com.chriscasey.codechallenger.admin;

import com.chriscasey.codechallenger.admin.dto.UserChallengeResponse;
import com.chriscasey.codechallenger.challenge.mapper.CodeChallengeMapper;
import com.chriscasey.codechallenger.challenge.CodeChallengeRepository;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;
import com.chriscasey.codechallenger.auth.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CodeChallengeRepository challengeRepository;

    @GetMapping("/users")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Get all users with their challenges", description = "Admin-only endpoint")
    public ResponseEntity<List<UserChallengeResponse>> getAllUsersWithChallenges() {
        List<UserChallengeResponse> responses = userRepository.findAll().stream()
                .map(user -> {
                    List<CodeChallengeResponse> challenges = challengeRepository.findAllByUser(user).stream()
                            .map(CodeChallengeMapper::toResponse)
                            .toList();

                    return new UserChallengeResponse(user.getId(), user.getEmail(), challenges);
                })
                .toList();

        return ResponseEntity.ok(responses);
    }
}
