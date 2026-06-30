package com.api.llm.exception;

public class InvalidEventIdEception extends RuntimeException {
    public InvalidEventIdEception(String message) {
        super(message);
    }
}
