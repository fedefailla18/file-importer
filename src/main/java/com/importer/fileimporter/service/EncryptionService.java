package com.importer.fileimporter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${encryption.secret:my-very-secret-key-32-chars-long!}")
    private String secret;

    private SecretKeySpec secretKey;

    @PostConstruct
    public void init() {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        // AES key must be 16, 24, or 32 bytes. We'll truncate or pad.
        byte[] finalKey = new byte[32];
        System.arraycopy(key, 0, finalKey, 0, Math.min(key.length, 32));
        secretKey = new SecretKeySpec(finalKey, "AES");
    }

    public String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting: " + e.toString());
        }
    }

    public String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting: " + e.toString());
        }
    }
}
