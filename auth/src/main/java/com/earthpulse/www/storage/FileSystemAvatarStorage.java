package com.earthpulse.www.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class FileSystemAvatarStorage implements AvatarStorage {

    private final Path storageDir;
    private final String publicBaseUrl;

    public FileSystemAvatarStorage(
            @Value("${app.avatar.storage-dir}") String storageDir,
            @Value("${app.avatar.public-base-url}") String publicBaseUrl) {
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(storageDir);
    }

    @Override
    public String store(UUID userId, byte[] bytes, String ext) {
        delete(userId);
        String filename = userId + "." + ext;
        Path target = resolve(filename);
        try {
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store avatar", e);
        }
        return publicBaseUrl + "/" + filename;
    }

    @Override
    public Resource load(String filename) {
        Path file = resolve(filename);
        FileSystemResource resource = new FileSystemResource(file);
        return resource.exists() ? resource : null;
    }

    @Override
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
