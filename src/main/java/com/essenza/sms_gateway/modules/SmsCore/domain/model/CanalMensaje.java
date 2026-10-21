package com.essenza.sms_gateway.modules.SmsCore.domain.model;

/**
 * Canal físico por el que sale un mensaje. Un {@link MessageType} tiene exactamente un canal;
 * nuevos canales (ej. WhatsApp, EMAIL) se agregan aquí sin cambiar la persistencia.
 */
public enum CanalMensaje {
    SMS,
    WHATSAPP
}
