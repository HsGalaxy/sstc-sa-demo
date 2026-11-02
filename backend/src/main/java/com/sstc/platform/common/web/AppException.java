package com.sstc.platform.common.web;

/**
 * Thrown by services when a request cannot be satisfied. Mapped to an HTTP status by
 * {@link GlobalExceptionHandler}.
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    private final String code;

    public AppException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static AppException notFound(String what) {
        return new AppException(404, "NOT_FOUND", what + " not found");
    }

    public static AppException badRequest(String message) {
        return new AppException(400, "BAD_REQUEST", message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, "CONFLICT", message);
    }

    public static AppException forbidden(String message) {
        return new AppException(403, "FORBIDDEN", message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(401, "UNAUTHORIZED", message);
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
