package com.chriscasey.codechallenger.exception;

public class LlmParseException extends RuntimeException {

    private final String rawPayload;

    public LlmParseException(String message, String rawPayload, Throwable cause) {
        super(message, cause);
        this.rawPayload = rawPayload;
    }

    public String getRawPayload() {
        return rawPayload;
    }
}