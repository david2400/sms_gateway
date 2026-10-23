package com.essenza.sms_gateway.modules.GestionPlatillas.domain.model;

import lombok.Value;

/**
 * Resultado de aplicar las variables a una plantilla.
 */
@Value
public class RenderedSms {
    String text;
    String templateCode;
    int templateVersion;
    /** Longitud en unidades de la codificación (en GSM-7 los caracteres extendidos cuentan 2). */
    int length;
    SmsEncoding encoding;
    /** Partes (SMS facturables) que ocupará el mensaje. */
    int parts;
}
