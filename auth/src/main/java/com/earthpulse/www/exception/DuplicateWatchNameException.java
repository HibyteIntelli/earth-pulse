package com.earthpulse.www.exception;

public class DuplicateWatchNameException extends RuntimeException {
    public DuplicateWatchNameException(String name) {
        super("A watch named \"" + name + "\" already exists for this account.");
    }
}
