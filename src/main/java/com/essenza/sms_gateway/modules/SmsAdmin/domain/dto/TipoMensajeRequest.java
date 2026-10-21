package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Alta o modificación de tipo de mensaje. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tipo de mensaje y sus capacidades")
public class TipoMensajeRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_]{2,30}", message = "Código: 2 a 30 letras, números o _")
    @Schema(description = "Código único; es el valor de tipo_mensaje en rutas y envíos (se guarda en mayúsculas)", example = "OTP")
    private String codigo;

    @Size(max = 255)
    @Schema(example = "Código de verificación de un solo uso")
    private String descripcion;

    @NotNull
    @Schema(description = "Canal por el que sale el mensaje", example = "SMS")
    private CanalMensaje canal;

    @Schema(description = "El mensaje lleva un código/PIN de un solo uso (por defecto false)", example = "true")
    private Boolean otp;

    @Schema(description = "Apto para envíos masivos (por defecto false)", example = "false")
    private Boolean masivo;

    @Schema(description = "Activo (por defecto true al crear)", example = "true")
    private Boolean activo;
}
