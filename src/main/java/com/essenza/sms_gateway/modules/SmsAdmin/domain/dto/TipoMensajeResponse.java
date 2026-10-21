package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tipo de mensaje con sus capacidades. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tipo de mensaje y sus capacidades")
public class TipoMensajeResponse {

    private Long id;

    @Schema(description = "Valor usado en tipo_mensaje (rutas, envíos)", example = "OTP")
    private String codigo;

    @Schema(example = "Código de verificación de un solo uso")
    private String descripcion;

    @Schema(example = "SMS")
    private CanalMensaje canal;

    @Schema(description = "El mensaje lleva un código/PIN de un solo uso", example = "true")
    private Boolean otp;

    @Schema(description = "Apto para envíos masivos", example = "false")
    private Boolean masivo;

    @Schema(example = "true")
    private Boolean activo;
}
