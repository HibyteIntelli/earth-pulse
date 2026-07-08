package com.earthpulse.www.exception;

public class WatchLimitExceededException extends RuntimeException {
    public WatchLimitExceededException(int max) {
        super("Watch limit of " + max + " reached. Delete an existing watch before creating a new one.");
    }
}
