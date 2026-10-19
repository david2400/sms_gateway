package com.essenza.sms_gateway.modules.SmsCore.domain.model;

/**
 * Tipo de mensaje SMS. Junto con el proveedor define la ruta (tabla {@code sms_ruta}).
 * El nombre del enum es el valor guardado en {@code sms_ruta.tipo_mensaje}.
 */
public enum MessageType {
    OTP,
    HABEAS_DATA,
    TEMPLATE,
    PERSONALIZED,
    NORMAL,
    MASIVO
}
