package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Value;

/**
 * Resultado de buscar una ruta pedida explícitamente en el request ({@code ruta}: código o id de {@code CanalRuta}).
 */
@Value
public class RouteLookup {

    public enum Status {
        /** Existe y está activa: {@link #plan} tiene sus pasos activos en orden. */
        FOUND,
        /** No existe una ruta de SMS con ese código o id. */
        NOT_FOUND,
        /** Existe pero está desactivada ({@code eliminado = 1}). */
        INACTIVE
    }

    String reference;
    Status status;
    RoutePlan plan;

    public static RouteLookup found(String reference, RoutePlan plan) {
        return new RouteLookup(reference, Status.FOUND, plan);
    }

    public static RouteLookup notFound(String reference) {
        return new RouteLookup(reference, Status.NOT_FOUND, null);
    }

    public static RouteLookup inactive(String reference) {
        return new RouteLookup(reference, Status.INACTIVE, null);
    }

    public boolean isFound() {
        return status == Status.FOUND;
    }
}
