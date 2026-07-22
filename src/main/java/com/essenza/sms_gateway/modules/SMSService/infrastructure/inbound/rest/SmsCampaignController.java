package com.essenza.sms_gateway.modules.SMSService.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.CreateSmsCampaignCommand;
import com.essenza.sms_gateway.modules.SMSService.domain.dto.Campanas.SmsCampaignResultQuery;
import com.essenza.sms_gateway.modules.SMSService.application.input.Campanas.CreateSmsCampaignUseCase;
import com.essenza.sms_gateway.modules.SMSService.application.input.Campanas.GetSmsCampaignResultsUseCase;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignOperationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@Tag(name = "SMS Campaign Service", description = "API para gestión de campañas SMS")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class SmsCampaignController {

    private final CreateSmsCampaignUseCase createSmsCampaignUseCase;
    private final GetSmsCampaignResultsUseCase getSmsCampaignResultsUseCase;

    @Operation(
            summary = "Crear campaña SMS",
            description = "Crea una nueva campaña SMS para envío masivo de mensajes"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Campaña creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SmsCampaignOperationResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en la creación de la campaña",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SmsCampaignOperationResult.class))
            )
    })
    @PostMapping("/campaigns")
    public ResponseEntity<SmsCampaignOperationResult> createCampaign(
            @Parameter(description = "Datos de la campaña a crear", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Solicitud de creación de campaña",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateSmsCampaignCommand.class))
            )
            @Valid @RequestBody CreateSmsCampaignCommand command) {
        
        log.info("[CAMPAIGN-API] Creando campaña: {}", command.getName());
        SmsCampaignOperationResult result = createSmsCampaignUseCase.create(command);

        if (result.isSuccess()) {
            log.info("[CAMPAIGN-API] ✅ Campaña creada. ID: {}", result.getCampaignId());
            return ResponseEntity.ok(result);
        } else {
            log.warn("[CAMPAIGN-API] ❌ Error creando campaña: {}", result.getErrorMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @Operation(
            summary = "Obtener resultados de campaña",
            description = "Retorna los resultados detallados de una campaña SMS específica"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultados obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SmsCampaignResultQuery.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Campaña no encontrada",
                    content = @Content
            )
    })
    @GetMapping("/campaigns/{campaignId}/results")
    public ResponseEntity<SmsCampaignResultQuery> getCampaignResults(
            @Parameter(description = "ID de la campaña", required = true) @PathVariable String campaignId) {
        
        log.info("[CAMPAIGN-API] Obteniendo resultados de campaña: {}", campaignId);
        SmsCampaignResultQuery result = getSmsCampaignResultsUseCase.getResults(campaignId);

        if (result != null) {
            log.info("[CAMPAIGN-API] ✅ Resultados obtenidos para campaña: {}", campaignId);
            return ResponseEntity.ok(result);
        } else {
            log.warn("[CAMPAIGN-API] ❌ No se encontraron resultados para campaña: {}", campaignId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Obtener detalles de campaña",
            description = "Retorna los detalles completos de una campaña SMS específica"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalles obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SmsCampaignResultQuery.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Campaña no encontrada",
                    content = @Content
            )
    })
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<SmsCampaignResultQuery> getCampaignDetails(
            @Parameter(description = "ID de la campaña", required = true) @PathVariable String campaignId) {
        
        log.info("[CAMPAIGN-API] Obteniendo detalles de campaña: {}", campaignId);
        SmsCampaignResultQuery result = getSmsCampaignResultsUseCase.getResults(campaignId);

        if (result != null) {
            log.info("[CAMPAIGN-API] ✅ Detalles obtenidos para campaña: {}", campaignId);
            return ResponseEntity.ok(result);
        } else {
            log.warn("[CAMPAIGN-API] ❌ No se encontró la campaña: {}", campaignId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Verificar estado del servicio de campañas",
            description = "Retorna el estado del servicio de gestión de campañas SMS"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado del servicio obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))
            )
    })
    @GetMapping("/campaigns/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
                "service", "SMS Campaign Service",
                "status", "ACTIVE",
                "timestamp", System.currentTimeMillis()
        );
        
        log.debug("[CAMPAIGN-API] Health check ejecutado");
        return ResponseEntity.ok(health);
    }
}

