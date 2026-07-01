package com.earthpulse.www.storage;

import org.springframework.core.io.Resource;

import java.util.UUID;

public interface AvatarStorage {
    String store(UUID userId, byte[] bytes, String ext);
    Resource load(String filename);
    void delete(UUID userId);
}
