package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

/**
 * Codificación con la que el operador envía el SMS: define cuántos caracteres caben por parte.
 * Las tildes en á í ó ú (y emojis) obligan UCS-2: 70 caracteres por SMS en vez de 160.
 */
public enum SmsEncoding {
    GSM7(160, 153),
    UCS2(70, 67);

    private final int singleLimit;
    private final int multipartLimit;

    SmsEncoding(int singleLimit, int multipartLimit) {
        this.singleLimit = singleLimit;
        this.multipartLimit = multipartLimit;
    }

    public int singleLimit() {
        return singleLimit;
    }

    public int multipartLimit() {
        return multipartLimit;
    }
}
