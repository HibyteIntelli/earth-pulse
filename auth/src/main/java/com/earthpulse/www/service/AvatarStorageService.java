package com.earthpulse.www.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private final Path storageDir;
    private final String publicBaseUrl;

    public AvatarStorageService(
            @Value("${app.avatar.storage-dir}") String storageDir,
            @Value("${app.avatar.public-base-url}") String publicBaseUrl) {
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(storageDir);
    }

    /** Stores the avatar and returns the public URL to reach it. */
    public String store(UUID userId, byte[] bytes, String ext) {
        String filename = userId + "." + ext;
        Path target = resolve(filename);
        Path tmp = storageDir.resolve(userId + "_upload.tmp");
        try {
            Files.write(tmp, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            delete(userId);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw new RuntimeException("Failed to store avatar", e);
        }
        return publicBaseUrl + "/" + filename;
    }

    @Nullable
    public Resource load(String filename) {
        Path file = resolve(filename);
        FileSystemResource resource = new FileSystemResource(file);
        return resource.exists() ? resource : null;
    }

    public void delete(UUID userId) {
        String prefix = userId + ".";
        try (var stream = Files.list(storageDir)) {
            stream.filter(p -> p.getFileName().toString().startsWith(prefix))
                  .forEach(p -> {
                      try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                  });
        } catch (IOException ignored) {}
    }

    private Path resolve(String filename) {
        Path resolved = storageDir.resolve(filename).normalize();
        if (!resolved.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid filename");
        }
        return resolved;
    }
}
