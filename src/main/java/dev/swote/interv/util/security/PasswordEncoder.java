package dev.swote.interv.util.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;

import dev.swote.interv.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class PasswordEncoder {
    private String pepper;

    private final Random random = new SecureRandom();

    @Value("${public-data-contest.security.saltSize}")
    private int saltSize;

    @PostConstruct
    protected void init() throws IOException {
        pepper =  FileUtil.readAll(Objects.requireNonNull(getClass().getResourceAsStream("/security/pepper")), StandardCharsets.US_ASCII);
    }

    public String getNextSalt() {
        byte[] salt = new byte[saltSize];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hash(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(password.getBytes());
        messageDigest.update(salt.getBytes());
        messageDigest.update(pepper.getBytes());
        return Base64.getEncoder().encodeToString(messageDigest.digest());
    }

    public boolean isExpectedPassword(String password, String expectedHash, String salt) throws NoSuchAlgorithmException {
        return hash(password,salt).equals(expectedHash);
    }
}