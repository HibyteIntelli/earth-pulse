package com.earthpulse.www.exception;

public class UnsupportedImageTypeException extends RuntimeException {
    public UnsupportedImageTypeException(String contentType) {
        super(contentType + " is not a supported image type. Use image/jpeg, image/png, or image/webp.");
    }
}
