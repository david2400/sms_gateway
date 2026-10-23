package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Value;

/**
 * Resultado de planear un envío: la ruta a ejecutar o el motivo de negocio por el que no se puede enviar.
 */
@Value
public class PlanResolution {

    RoutePlan plan;
    /** Código de {@link DispatchResult} cuando no hay plan ({@code null} si hay plan). */
    String errorCode;
    String detail;
    /** Proveedor relacionado con el error (para la respuesta). */
    String providerCode;
    /** Cuenta relacionada con el error (para la respuesta). */
    String accountCode;

    public static PlanResolution ok(RoutePlan plan) {
        return new PlanResolution(plan, null, null, null, null);
    }

    public static PlanResolution error(String errorCode, String detail, String providerCode, String accountCode) {
        return new PlanResolution(null, errorCode, detail, providerCode, accountCode);
    }

    public boolean isOk() {
        return errorCode == null;
    }
}
