package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;
import com.chriscasey.codechallenger.challenge.dto.SubmitChallengeRequest;
import com.chriscasey.codechallenger.challenge.mapper.CodeChallengeMapper;
import com.chriscasey.codechallenger.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class CodeChallengeController {

    private final CodeChallengeService service;

    @Operation(summary = "Get all challenges for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Challenges retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CodeChallengeResponse>> getAll(@CurrentUser User user) {
        List<CodeChallengeResponse> challenges = service.getAllForUser(user).stream()
                .map(CodeChallengeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(challenges);
    }

    /**
     * Create a new challenge for the user
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> create(@CurrentUser User user) {
        service.createChallenge(user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Submit an answer to a specific challenge")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Answer submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid answer or already completed"),
            @ApiResponse(responseCode = "404", description = "Challenge not found")
    })
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CodeChallengeResponse> submitAnswer(
            @PathVariable Long id,
            @RequestBody SubmitChallengeRequest request,
            @CurrentUser User user
    ) {
        CodeChallengeResponse response = service.submitAnswer(user, id, request.answer());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Skip a challenge and generate a new one")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Challenge skipped and new one created"),
            @ApiResponse(responseCode = "404", description = "Challenge not found"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/skip")
    public ResponseEntity<Void> skipChallenge(@PathVariable Long id, @CurrentUser User user) {
        service.skipChallenge(id, user);
        return ResponseEntity.noContent().build();
    }
}
