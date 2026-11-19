package com.communitynexus.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NexusException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public NexusException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static NexusException notFound(String what) {
        return new NexusException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " not found");
    }

    public static NexusException badRequest(String code, String message) {
        return new NexusException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static NexusException conflict(String code, String message) {
        return new NexusException(HttpStatus.CONFLICT, code, message);
    }
}