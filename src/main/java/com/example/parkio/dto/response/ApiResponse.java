package com.example.parkio.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
// import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Success", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return ok("Created successfully", data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
