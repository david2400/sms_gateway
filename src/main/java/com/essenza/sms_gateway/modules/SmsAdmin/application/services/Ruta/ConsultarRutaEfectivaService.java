package com.essenza.sms_gateway.modules.SmsAdmin.application.services.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ConsultarRutaEfectivaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaEfectivaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorCatalogPort;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RoutePlan;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.RouteStep;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.RoutingPolicyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta la ruta con el mismo puerto que usa el motor (RoutingPolicyPort de SmsCore), así se ve
 * exactamente qué cuentas se intentarán, y advierte cuentas sin credenciales o proveedores sin módulo.
 */
@Service
@RequiredArgsConstructor
public class ConsultarRutaEfectivaService implements ConsultarRutaEfectivaUseCase {

    private final RoutingPolicyPort routingPolicy;
    private final ProveedorCatalogPort catalog;

    @Override
    public RutaEfectivaResponse efectiva(String proveedorCodigo, MessageType tipoMensaje) {
        String proveedor = proveedorCodigo == null || proveedorCodigo.isBlank()
                || AdminCodigos.CENTRAL.equalsIgnoreCase(proveedorCodigo.trim())
                ? null : AdminCodigos.normalizar(proveedorCodigo);
        RoutePlan plan = routingPolicy.resolve(proveedor, tipoMensaje);

        List<String> advertencias = new ArrayList<>();
        List<RutaEfectivaResponse.Paso> pasos = new ArrayList<>();
        for (RouteStep step : plan.getSteps() == null ? List.<RouteStep>of() : plan.getSteps()) {
            ProviderAccount cuenta = step.getAccount();
            boolean credenciales = cuenta.hasCredentials() && cuenta.getUsername() != null && !cuenta.getUsername().isBlank();
            boolean implementado = catalog.isImplementado(cuenta.getProviderCode());
            if (!credenciales) {
                advertencias.add("Cuenta " + cuenta.getCode()
                        + " sin credenciales (revise usuario y la variable de entorno de secret_ref): sus intentos fallarán");
            }
            if (!implementado) {
                advertencias.add("Proveedor " + cuenta.getProviderCode() + " no tiene módulo implementado: la cuenta "
                        + cuenta.getCode() + " se saltará");
            }
            pasos.add(RutaEfectivaResponse.Paso.builder()
                    .orden(step.getOrder())
                    .proveedor(cuenta.getProviderCode())
                    .cuenta(cuenta.getCode())
                    .maxIntentos(step.getMaxAttempts())
                    .esperaMs(step.getWaitMs())
                    .credencialesOk(credenciales)
                    .implementado(implementado)
                    .build());
        }
        if (plan.isEmpty()) {
            advertencias.add("No hay ruta activa con pasos activos: los envíos responderán 422 RUTA_NO_CONFIGURADA");
        }
        return RutaEfectivaResponse.builder()
                .proveedor(proveedor == null ? AdminCodigos.CENTRAL : proveedor)
                .tipoMensaje(tipoMensaje.name())
                .configurada(!plan.isEmpty())
                .pasos(pasos)
                .advertencias(advertencias)
                .build();
    }
}
