package com.example.parkio.exception;

import org.springframework.http.HttpStatus;

public class ParkioException extends RuntimeException {

    private final HttpStatus status;

    public ParkioException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static ParkioException notFound(String message) {
        return new ParkioException(message, HttpStatus.NOT_FOUND);
    }

    public static ParkioException badRequest(String message) {
        return new ParkioException(message, HttpStatus.BAD_REQUEST);
    }

    public static ParkioException conflict(String message) {
        return new ParkioException(message, HttpStatus.CONFLICT);
    }

    public static ParkioException forbidden(String message) {
        return new ParkioException(message, HttpStatus.FORBIDDEN);
    }
}
