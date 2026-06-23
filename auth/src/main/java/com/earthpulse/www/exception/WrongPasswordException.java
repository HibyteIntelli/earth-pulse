package com.earthpulse.www.exception;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException() {
        super("Current password is incorrect");
    }
}
