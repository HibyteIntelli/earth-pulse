package com.earthpulse.www.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Service
public class BannedPasswordService {

    private static final Logger log = LoggerFactory.getLogger(BannedPasswordService.class);
    private static final int EXPECTED_INSERTIONS = 15_000_000;
    private static final double FALSE_POSITIVE_PROBABILITY = 0.001;

    private final BloomFilter<CharSequence> bannedPasswords;

    public BannedPasswordService() {
        log.info("Loading banned passwords into bloom filter");
        var filter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY
        );

        var resource = new ClassPathResource("banned_passwords/rockyou.txt");
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            long count = 0;
            while ((line = reader.readLine()) != null) {
                filter.put(line);
                count++;
            }
            log.info("Banned passwords bloom filter ready ({} entries loaded).", count);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load banned_passwords/rockyou.txt from classpath", e);
        }

        this.bannedPasswords = filter;
    }

    public boolean isBanned(String password) {
        return bannedPasswords.mightContain(password);
    }
}
