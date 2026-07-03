package com.api.llm.exception;

public class InvalidEventIdException extends RuntimeException {
    public InvalidEventIdException(String message) {
        super(message);
    }
}
