package com.essenza.sms_gateway.modules.SmsAdmin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lista completa de pasos; reemplaza los actuales de la ruta")
public class PasosRequest {
    @NotNull
    @Valid
    private List<PasoRequest> pasos;
}
