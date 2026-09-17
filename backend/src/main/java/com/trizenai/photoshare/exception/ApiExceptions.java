package com.trizenai.photoshare.exception;

public class ApiExceptions {

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    /** Thrown when a customer supplies an incorrect gallery PIN. */
    public static class InvalidPinException extends RuntimeException {
        public InvalidPinException(String message) { super(message); }
    }
}
