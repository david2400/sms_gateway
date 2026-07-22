package com.essenza.sms_gateway.modules.SMSService.domain.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servicio para generación segura de PIN y salt
 * Implementa mejores prácticas de seguridad para generación de credenciales
 */
@Slf4j
@Service
public class PinGeneratorService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_PIN_LENGTH = 6;
    private static final int DEFAULT_SALT_LENGTH = 16;

    /**
     * Genera un PIN numérico aleatorio
     * @param length Longitud del PIN (por defecto 6)
     * @return PIN generado
     */
    public String generatePin(int length) {
        if (length < 4 || length > 8) {
            length = DEFAULT_PIN_LENGTH;
        }

        StringBuilder pin = new StringBuilder();
        for (int i = 0; i < length; i++) {
            pin.append(SECURE_RANDOM.nextInt(10));
        }

        String generatedPin = pin.toString();
        
        while (isPredictablePin(generatedPin)) {
            pin = new StringBuilder();
            for (int i = 0; i < length; i++) {
                pin.append(SECURE_RANDOM.nextInt(10));
            }
            generatedPin = pin.toString();
        }

        log.debug("[PIN-GENERATOR] PIN generado de {} dígitos", length);
        return generatedPin;
    }

    /**
     * Genera un PIN de longitud por defecto (6 dígitos)
     * @return PIN generado
     */
    public String generatePin() {
        return generatePin(DEFAULT_PIN_LENGTH);
    }

    /**
     * Genera un salt aleatorio para hashing
     * @return Salt en formato Base64
     */
    public String generateSalt() {
        byte[] salt = new byte[DEFAULT_SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        log.debug("[PIN-GENERATOR] Salt generado de {} bytes", DEFAULT_SALT_LENGTH);
        return saltBase64;
    }

    /**
     * Genera un salt de longitud específica
     * @param length Longitud en bytes
     * @return Salt en formato Base64
     */
    public String generateSalt(int length) {
        if (length < 8) {
            length = DEFAULT_SALT_LENGTH;
        }
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashea el PIN con el salt usando SHA-256
     * @param pin PIN a hashear
     * @param salt Salt para el hash
     * @return PIN hasheado en formato Base64
     */
    public String hashPin(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = pin + salt;
            byte[] hash = digest.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("[PIN-GENERATOR] Error al hashear PIN: {}", e.getMessage());
            throw new RuntimeException("Error al hashear PIN", e);
        }
    }

    /**
     * Genera PIN, salt y hash en una sola operación
     * @return Array con [pin, salt, hashedPin]
     */
    public PinData generatePinWithSalt() {
        String pin = generatePin();
        String salt = generateSalt();
        String hashedPin = hashPin(pin, salt);
        
        log.info("[PIN-GENERATOR] PIN y salt generados exitosamente");
        return new PinData(pin, salt, hashedPin);
    }

    /**
     * Verifica si un PIN es demasiado predecible
     */
    private boolean isPredictablePin(String pin) {
        return pin.matches("(\\d)\\1{3,}") ||
               pin.equals("123456") ||
               pin.equals("654321") ||
               pin.equals("111111") ||
               pin.equals("000000") ||
               pin.equals("1234567") ||
               pin.equals("12345678");
    }

    /**
     * Clase interna para retornar datos del PIN
     */
    public static class PinData {
        private final String pin;
        private final String salt;
        private final String hashedPin;

        public PinData(String pin, String salt, String hashedPin) {
            this.pin = pin;
            this.salt = salt;
            this.hashedPin = hashedPin;
        }

        public String getPin() {
            return pin;
        }

        public String getSalt() {
            return salt;
        }

        public String getHashedPin() {
            return hashedPin;
        }
    }
}
