package com.earthpulse.www.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collector;

@Service
public class BannedPasswordService {

    private static final Logger log = LoggerFactory.getLogger(BannedPasswordService.class);
    private static final int EXPECTED_INSERTIONS = 15_000_000;
    private static final double FALSE_POSITIVE_PROBABILITY = 0.001;

    private final BloomFilter<CharSequence> bannedPasswords;

    public BannedPasswordService(
            @Value("${app.banned-passwords.txt-path}") String txtPath,
            @Value("${app.banned-passwords.bloom-path}") String bloomPath) {
        this.bannedPasswords = loadOrBuild(Path.of(txtPath), Path.of(bloomPath));
    }

    private BloomFilter<CharSequence> loadOrBuild(Path txtPath, Path bloomPath) {
        if (Files.exists(bloomPath)) {
            try (var in = new BufferedInputStream(Files.newInputStream(bloomPath))) {
                log.info("Loading pre-built bloom filter from {}", bloomPath);
                var filter = BloomFilter.readFrom(in, Funnels.stringFunnel(StandardCharsets.UTF_8));
                log.info("Banned passwords bloom filter ready (loaded from cache).");
                return filter;
            } catch (IOException e) {
                log.warn("Failed to read serialized bloom filter, rebuilding from source: {}", e.getMessage());
            }
        }
        return buildAndSerialize(txtPath, bloomPath);
    }

    private BloomFilter<CharSequence> buildAndSerialize(Path txtPath, Path bloomPath) {
        log.info("Building banned passwords bloom filter from {} using {} threads...",
                txtPath, Runtime.getRuntime().availableProcessors());

        BloomFilter<CharSequence> filter;
        try (var lines = Files.lines(txtPath, StandardCharsets.ISO_8859_1)) {
            filter = lines.parallel().collect(Collector.of(
                    () -> BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), EXPECTED_INSERTIONS, FALSE_POSITIVE_PROBABILITY),
                    BloomFilter::put,
                    (a, b) -> { a.putAll(b); return a; }
            ));
            log.info("Banned passwords bloom filter built.");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load banned passwords file: " + txtPath, e);
        }

        try (var out = new BufferedOutputStream(Files.newOutputStream(bloomPath))) {
            filter.writeTo(out);
            log.info("Bloom filter serialized to {} for faster future startups.", bloomPath);
        } catch (IOException e) {
            log.warn("Could not serialize bloom filter to {}: {}", bloomPath, e.getMessage());
        }

        return filter;
    }

    public boolean isBanned(String password) {
        return bannedPasswords.mightContain(password);
    }
}
