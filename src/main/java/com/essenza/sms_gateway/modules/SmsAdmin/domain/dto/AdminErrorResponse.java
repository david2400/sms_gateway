package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error del módulo de administración")
public class AdminErrorResponse {
    private String codigo;
    private String mensaje;
    private List<String> detalles;
    private LocalDateTime fecha;
}
