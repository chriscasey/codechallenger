package com.chriscasey.codechallenger.exception;

public class SubmissionRateLimitException extends RuntimeException {
    private final long remainingMinutes;

    public SubmissionRateLimitException(long remainingMinutes) {
        super(String.format("You must wait %d more minutes before submitting another answer for this challenge", remainingMinutes));
        this.remainingMinutes = remainingMinutes;
    }

    public long getRemainingMinutes() {
        return remainingMinutes;
    }
}

