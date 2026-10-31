package com.essenza.sms_gateway.modules.SmsCore.shared;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Traduce el resultado de un envío a la respuesta HTTP. Lo reutilizan los
 * controllers de cada módulo de proveedor para responder igual que el central.
 * <ul>
 *   <li>ENVIADO → 200</li>
 *   <li>VALIDACION → 400</li>
 *   <li>RUTA_NO_CONFIGURADA → 422</li>
 *   <li>CUENTA_NO_VALIDA → 422 (la cuenta pedida no existe, está inactiva o es de otro proveedor)</li>
 *   <li>SERVICIO_NO_SOPORTADO → 422 (el proveedor no tiene habilitado el tipo de mensaje)</li>
 *   <li>RUTA_NO_ENCONTRADA, RUTA_INACTIVA, RUTA_TIPO_NO_COINCIDE, RUTA_NO_COMBINABLE → 422 (ruta pedida en el request)</li>
 *   <li>TODAS_LAS_CUENTAS_FALLARON → 502</li>
 * </ul>
 */
public final class SmsHttpStatusMapper {

    private SmsHttpStatusMapper() {
    }

    public static ResponseEntity<SmsEnvioResponse> toResponse(DispatchResult result) {
        return ResponseEntity.status(statusOf(result)).body(SmsEnvioResponse.from(result));
    }

    public static HttpStatus statusOf(DispatchResult result) {
        if (result.isSent()) {
            return HttpStatus.OK;
        }
        if (DispatchResult.ERROR_VALIDACION.equals(result.getErrorCode())) {
            return HttpStatus.BAD_REQUEST;
        }
        if (DispatchResult.ERROR_RUTA_NO_CONFIGURADA.equals(result.getErrorCode())
                || DispatchResult.ERROR_CUENTA_NO_VALIDA.equals(result.getErrorCode())
                || DispatchResult.ERROR_SERVICIO_NO_SOPORTADO.equals(result.getErrorCode())
                || DispatchResult.ERROR_RUTA_NO_ENCONTRADA.equals(result.getErrorCode())
                || DispatchResult.ERROR_RUTA_INACTIVA.equals(result.getErrorCode())
                || DispatchResult.ERROR_RUTA_TIPO_NO_COINCIDE.equals(result.getErrorCode())
                || DispatchResult.ERROR_RUTA_NO_COMBINABLE.equals(result.getErrorCode())) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        return HttpStatus.BAD_GATEWAY;
    }
}
