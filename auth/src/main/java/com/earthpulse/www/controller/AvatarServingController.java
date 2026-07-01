package com.earthpulse.www.controller;

import com.earthpulse.www.storage.AvatarStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/avatars")
@RequiredArgsConstructor
public class AvatarServingController {

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[0-9a-fA-F-]+\\.(jpg|png|webp)$");

    private final AvatarStorage avatarStorage;

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = avatarStorage.load(filename);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = resolveMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    private MediaType resolveMediaType(String filename) {
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
