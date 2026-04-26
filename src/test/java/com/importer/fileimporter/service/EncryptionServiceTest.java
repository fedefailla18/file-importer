package com.importer.fileimporter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    public void setup() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secret", "my-test-secret-key-32-chars-long!");
        encryptionService.init();
    }

    @Test
    public void testEncryptDecrypt() {
        String original = "my-binance-secret";
        String encrypted = encryptionService.encrypt(original);
        assertNotEquals(original, encrypted);
        
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }
}
