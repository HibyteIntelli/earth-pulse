package com.earthpulse.www.exception;

import java.util.Objects;

public class UnsupportedImageTypeException extends RuntimeException {
    public UnsupportedImageTypeException(String contentType) {
        super(Objects.toString(contentType, "unknown") + " is not a supported image type. Use image/jpeg, image/png, or image/webp.");
    }
}
