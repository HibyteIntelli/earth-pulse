package com.earthpulse.www.exception;

import java.util.UUID;

public class WatchNotFoundException extends RuntimeException {
    public WatchNotFoundException(UUID watchId) {
        super("Watch not found: " + watchId);
    }
}
