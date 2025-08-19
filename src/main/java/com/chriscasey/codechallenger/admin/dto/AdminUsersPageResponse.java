package com.chriscasey.codechallenger.admin.dto;

import java.util.List;

public record AdminUsersPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sortBy,
        String sortDir,
        List<AdminUserChallengeResponse> content
) {}
