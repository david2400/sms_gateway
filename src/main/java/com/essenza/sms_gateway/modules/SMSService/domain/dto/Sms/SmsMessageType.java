package com.essenza.sms_gateway.modules.SMSService.domain.dto.Sms;

/**
 * Enum de dominio para los tipos de mensaje SMS.
 * <p>
 * No conoce proveedores concretos ni sus tipos de cuenta: cada proveedor
 * (ej. Inalambria) resuelve internamente, a partir de este tipo, qué cuenta
 * o ruta interna debe usar (ver {@code InalambriaAccountOrchestrator}).
 */
public enum SmsMessageType {
    OTP("OTP", "One Time Password"),
    HABEAS_DATA("HABEAS_DATA", "Habeas Data Authorization"),
    TEMPLATE("TEMPLATE", "Plantilla with variables"),
    PERSONALIZED("PERSONALIZED", "Personalized content"),
    NORMAL("NORMAL", "Normal message");
    
    private final String code;
    private final String description;
    
    SmsMessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static SmsMessageType fromCode(String code) {
        for (SmsMessageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return NORMAL;
    }
}
