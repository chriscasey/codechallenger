package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;
import com.chriscasey.codechallenger.challenge.dto.SubmitChallengeRequest;
import com.chriscasey.codechallenger.challenge.mapper.CodeChallengeMapper;
import com.chriscasey.codechallenger.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@Tag(name = "Challenges", description = "Manage and solve code challenges")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping(value = "/api/challenges", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class CodeChallengeController {

    private final CodeChallengeService service;

    @Operation(summary = "Get all challenges for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Challenges retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CodeChallengeResponse>> getAll(
            @Parameter(hidden = true) @CurrentUser User user
    ) {
        List<CodeChallengeResponse> challenges = service.getAllForUser(user).stream()
                .map(CodeChallengeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(challenges);
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
            @Valid @RequestBody SubmitChallengeRequest request,
            @Parameter(hidden = true) @CurrentUser User user // <— hide injected param
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
    public ResponseEntity<Void> skipChallenge(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser User user
    ) {
        service.skipChallenge(id, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Generate a new code challenge for the current user",
            description = "Creates a new PENDING challenge using the LLM generator. "
                    + "Optionally specify a difficulty to override the automatic rating."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Challenge generated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/generate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CodeChallengeResponse> generateChallenge(
            @Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "Optional explicit difficulty")
            @RequestParam(required = false) @Min(1) @Max(5) Integer difficulty
    ) {
        CodeChallengeResponse response = (difficulty == null)
                ? service.generateNewChallenge(user)
                : service.generateNewChallenge(user, difficulty);

        // Provide a Location header for the newly created resource
        URI location = URI.create("/api/challenges/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
}
