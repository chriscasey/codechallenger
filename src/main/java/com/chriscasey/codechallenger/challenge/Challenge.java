package com.chriscasey.codechallenger.challenge;

public record Challenge(
        String title,
        String prompt,
        String answer,
        int difficulty
) {}
