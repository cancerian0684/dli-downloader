package org.shunya.dli;

public class CancelledExecutionException extends RuntimeException{
    public CancelledExecutionException(String message) {
        super(message);
    }

    public CancelledExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
