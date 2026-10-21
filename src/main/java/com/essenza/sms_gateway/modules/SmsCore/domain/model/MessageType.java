package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Getter;

import java.util.Optional;

/**
 * Tipo de mensaje SMS. Junto con el proveedor define la ruta (tabla {@code sms_ruta}).
 * El nombre del enum es el valor guardado en {@code sms_ruta.tipo_mensaje}.
 *
 * <p>Cada tipo declara sus capacidades en código (canal, OTP, masivo): no se infieren
 * del nombre ni de registros en BD, así un tipo nuevo solo requiere una línea aquí.
 * Ej.: {@code WHATSAPP_OTP(CanalMensaje.WHATSAPP, true, false, "...")}.</p>
 */
@Getter
public enum MessageType {

    OTP(CanalMensaje.SMS, true, false, "Código de verificación de un solo uso"),
    HABEAS_DATA(CanalMensaje.SMS, false, false, "Notificación judicial / habeas data"),
    TEMPLATE(CanalMensaje.SMS, false, false, "Mensaje basado en plantilla del proveedor"),
    PERSONALIZED(CanalMensaje.SMS, false, false, "Mensaje con contenido personalizado por destinatario"),
    NORMAL(CanalMensaje.SMS, false, true, "Mensaje estándar / masivo"),
    MASIVO(CanalMensaje.SMS, false, true, "Envío masivo");

    /** Canal físico por el que sale el mensaje. */
    private final CanalMensaje canal;

    /** true si el mensaje lleva un código/PIN de un solo uso. */
    private final boolean otp;

    /** true si es apto para envíos masivos. */
    private final boolean masivo;

    private final String descripcion;

    MessageType(CanalMensaje canal, boolean otp, boolean masivo, String descripcion) {
        this.canal = canal;
        this.otp = otp;
        this.masivo = masivo;
        this.descripcion = descripcion;
    }

    public boolean isSms() {
        return canal == CanalMensaje.SMS;
    }

    public boolean isWhatsapp() {
        return canal == CanalMensaje.WHATSAPP;
    }

    /** Resuelve un código (valor de {@code sms_ruta.tipo_mensaje}) sin importar mayúsculas ni espacios. */
    public static Optional<MessageType> fromCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(MessageType.valueOf(codigo.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
