package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tipo de mensaje ({@code sms_tipo_mensaje}). El {@code codigo} es el valor que viaja en
 * {@code sms_ruta.tipo_mensaje} y en las peticiones de envío; las banderas
 * ({@code otp}, {@code masivo}) y el {@code canal} definen sus capacidades.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoMensaje {
    private Long id;
    private String codigo;
    private String descripcion;
    private CanalMensaje canal;
    private Boolean otp;
    private Boolean masivo;
    private Boolean activo;
}
