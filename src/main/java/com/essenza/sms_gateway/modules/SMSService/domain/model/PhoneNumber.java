package com.essenza.sms_gateway.modules.SMSService.domain.model;

import lombok.Value;

import java.util.regex.Pattern;

/**
 * Value Object: Número de Teléfono
 */
@Value
public class PhoneNumber {
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"  // E.164 format
    );
    
    String number;
    String countryCode;
    
    public PhoneNumber(String number) {
        this.number = normalize(number);
        this.countryCode = extractCountryCode(this.number);
    }
    
    /**
     * Normaliza el número de teléfono
     */
    private String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        
        // Remover caracteres no numéricos excepto +
        String normalized = phone.replaceAll("[^0-9+]", "");
        
        // Si no tiene código de país, agregar +57 (Colombia)
        if (!normalized.startsWith("+")) {
            if (normalized.startsWith("57")) {
                normalized = "+" + normalized;
            } else {
                normalized = "+57" + normalized;
            }
        }
        
        return normalized;
    }
    
    /**
     * Extrae el código de país
     */
    private String extractCountryCode(String phone) {
        if (phone.startsWith("+")) {
            // Códigos de país pueden ser de 1-3 dígitos
            if (phone.length() >= 3) {
                return phone.substring(0, 3);
            }
        }
        return "+57"; // Colombia por defecto
    }
    
    /**
     * Valida que el número sea válido
     */
    public boolean isValid() {
        return PHONE_PATTERN.matcher(number).matches();
    }
    
    @Override
    public String toString() {
        return number;
    }
}
