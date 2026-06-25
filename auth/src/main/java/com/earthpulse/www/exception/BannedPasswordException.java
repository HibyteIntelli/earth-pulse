package com.earthpulse.www.exception;

public class BannedPasswordException extends RuntimeException {
    public BannedPasswordException() {
        super("Password is too common and cannot be used");
    }
}
