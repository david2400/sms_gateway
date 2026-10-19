package com.essenza.sms_gateway.modules.SmsCore.domain.dto;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registro del log de envíos, con sus intentos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registro del log de envíos de SMS")
public class SmsEnvioDetalleResponse {

    private UUID envioId;
    private DispatchChannel canal;
    @Schema(description = "Proveedor pedido en el endpoint propio del proveedor (null en el central)")
    private String proveedorSolicitado;
    private MessageType tipoMensaje;
    private String destino;
    private String mensaje;
    private DispatchStatus estado;
    @Schema(description = "Proveedor con el que finalmente se envió")
    private String proveedorFinal;
    @Schema(description = "Cuenta con la que finalmente se envió")
    private String cuentaFinal;
    private Integer httpStatusFinal;
    private int totalIntentos;
    private String codigoError;
    private String detalleError;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaFinalizacion;

    @Builder.Default
    private List<Intento> intentos = new ArrayList<>();

    /**
     * Un intento contra una cuenta.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Intento {
        private int pasoOrden;
        private int nroIntento;
        private String proveedor;
        private String cuenta;
        private boolean exitoso;
        private Integer httpStatus;
        private String detalle;
        private long duracionMs;
        private LocalDateTime fecha;
    }
}
