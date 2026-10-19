package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Ruta resuelta para un (proveedor, tipo de mensaje): lista ordenada de cuentas.
 * {@code providerCode == null} indica la ruta del endpoint central.
 */
@Value
@Builder
public class RoutePlan {

    Long routeId;
    String providerCode;
    MessageType messageType;
    @Singular
    List<RouteStep> steps;

    public boolean isEmpty() {
        return steps == null || steps.isEmpty();
    }

    public static RoutePlan empty(String providerCode, MessageType type) {
        return RoutePlan.builder().providerCode(providerCode).messageType(type).build();
    }
}
