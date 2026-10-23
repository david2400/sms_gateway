package com.essenza.sms_gateway.modules.GestionPlatillas.application.services;

import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.ConsultarPlantillaUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaDefinicionResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaPreviewResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaVariableInfo;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.RenderedSms;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports.PlantillaQueryPort;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.services.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsultarPlantillaService implements ConsultarPlantillaUseCase {

    private final PlantillaQueryPort templates;
    private final TemplateRenderer renderer;

    @Override
    public PlantillaDefinicionResponse describe(String code) {
        CompiledTemplate t = find(code);
        return PlantillaDefinicionResponse.builder()
                .codigo(t.getCode())
                .nombre(t.getName())
                .tipoMensaje(t.getMessageType())
                .version(t.getVersion())
                .contenido(t.getContent())
                .variables(t.getVariables().values().stream().map(v -> PlantillaVariableInfo.builder()
                        .nombre(v.getName())
                        .tipoDato(v.getDataType())
                        .obligatoria(v.isRequired() && (v.getDefaultValue() == null || v.getDefaultValue().isBlank()))
                        .longitudMax(v.getMaxLength())
                        .patron(v.getPattern())
                        .valorDefecto(v.getDefaultValue())
                        .descripcion(v.getDescription())
                        .build()).toList())
                .build();
    }

    @Override
    public PlantillaPreviewResponse preview(String code, Map<String, String> variables) {
        CompiledTemplate t = find(code);
        RenderedSms r = renderer.render(t, variables);
        return PlantillaPreviewResponse.builder()
                .plantilla(t.getCode())
                .version(t.getVersion())
                .tipoMensaje(t.getMessageType())
                .texto(r.getText())
                .longitud(r.getLength())
                .codificacion(r.getEncoding())
                .partes(r.getParts())
                .build();
    }

    private CompiledTemplate find(String code) {
        return templates.findActive(code).orElseThrow(() -> SmsContenidoException.noEncontrada(code.toUpperCase()));
    }
}
