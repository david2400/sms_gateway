package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers;

/**
 * Tipos de cuenta para Inalambria
 */
public enum InalambriaAccountType {
    OTP("OTP"),
    TEMPLATE("TEMPLATE"), 
    PERSONALIZADA("PERSONALIZADA"),
    MASIVO("MASIVO"),
    HABEASDATA("HABEASDATA");
    
    private final String code;
    
    InalambriaAccountType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static InalambriaAccountType fromCode(String code) {
        for (InalambriaAccountType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown account type: " + code);
    }
}
