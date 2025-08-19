package com.chriscasey.codechallenger.admin;

import com.chriscasey.codechallenger.admin.dto.AdminUserChallengeResponse;
import com.chriscasey.codechallenger.admin.dto.AdminChallengeResponse;
import com.chriscasey.codechallenger.admin.dto.AdminUsersPageResponse;
import com.chriscasey.codechallenger.challenge.mapper.CodeChallengeMapper;
import com.chriscasey.codechallenger.challenge.CodeChallenge;
import com.chriscasey.codechallenger.challenge.CodeChallengeRepository;
import com.chriscasey.codechallenger.auth.User;
import com.chriscasey.codechallenger.auth.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CodeChallengeRepository challengeRepository;

    @GetMapping("/users")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Get all users with their challenges (paginated)", description = "Admin-only endpoint")
    public ResponseEntity<AdminUsersPageResponse> getAllUsersWithChallenges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<User> usersPage = userRepository.findAll(pageable);
        List<User> users = usersPage.getContent();

        if (users.isEmpty()) {
            AdminUsersPageResponse empty = new AdminUsersPageResponse(
                    page, size, usersPage.getTotalElements(), usersPage.getTotalPages(), sortBy, sortDir, List.of()
            );
            return ResponseEntity.ok(empty);
        }

        // Single batch query for all challenges belonging to these users
        List<CodeChallenge> allChallenges = challengeRepository.findByUserIn(users);

        // Group challenges by user id
        Map<Long, List<AdminChallengeResponse>> byUserId = allChallenges.stream()
                .collect(Collectors.groupingBy(
                        ch -> ch.getUser().getId(),
                        Collectors.mapping(CodeChallengeMapper::toAdminResponse, Collectors.toList())
                ));

        List<AdminUserChallengeResponse> content = users.stream()
                .map(u -> new AdminUserChallengeResponse(
                        u.getId(),
                        u.getEmail(),
                        byUserId.getOrDefault(u.getId(), Collections.emptyList())
                ))
                .toList();

        AdminUsersPageResponse body = new AdminUsersPageResponse(
                usersPage.getNumber(),
                usersPage.getSize(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages(),
                sortBy,
                sortDir,
                content
        );

        return ResponseEntity.ok(body);
    }
}
