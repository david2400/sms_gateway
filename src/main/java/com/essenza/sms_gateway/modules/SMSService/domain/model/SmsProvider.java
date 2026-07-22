package com.essenza.sms_gateway.modules.SMSService.domain.model;

/**
 * Enumeración de proveedores de SMS disponibles
 */
public enum SmsProvider {
    
    INALAMBRIA("inalambria", "Inalambria", "Inalambria API"),
    GO4CLIENTS("go4clients", "Go4Clients", "Go4Clients Cloud API"),
    TWILIO("twilio", "Twilio", "Twilio API"),
    AWS_SNS("aws-sns", "AWS SNS", "Amazon SNS"),
    MOCK("mock", "Mock", "Mock Provider for testing");
    
    private final String code;
    private final String name;
    private final String description;
    
    SmsProvider(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Obtiene el proveedor desde el código
     */
    public static SmsProvider fromCode(String code) {
        for (SmsProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        return MOCK; // Default a mock si no se encuentra
    }
    
    /**
     * Obtiene el proveedor desde el nombre
     */
    public static SmsProvider fromName(String name) {
        for (SmsProvider provider : values()) {
            if (provider.name.equalsIgnoreCase(name)) {
                return provider;
            }
        }
        return MOCK; // Default a mock si no se encuentra
    }
}
