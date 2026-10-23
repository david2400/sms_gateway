package com.essenza.sms_gateway.modules.SmsCore.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsErrorResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Traduce {@link SmsContenidoException} para el endpoint central y los de cada proveedor:
 * <ul>
 *   <li>VALIDACION, VARIABLES_INVALIDAS → 400</li>
 *   <li>PLANTILLA_NO_ENCONTRADA, PLANTILLA_MAL_CONFIGURADA → 422</li>
 * </ul>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.essenza.sms_gateway.modules")
public class SmsContenidoExceptionHandler {

    @ExceptionHandler(SmsContenidoException.class)
    public ResponseEntity<SmsErrorResponse> contenido(SmsContenidoException e) {
        HttpStatus status = SmsContenidoException.PLANTILLA_NO_ENCONTRADA.equals(e.getCodigo())
                || SmsContenidoException.PLANTILLA_MAL_CONFIGURADA.equals(e.getCodigo())
                ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_REQUEST;
        log.warn("[SMS-CORE] Request rechazado ({}): {}", e.getCodigo(), e.getMessage());
        return ResponseEntity.status(status).body(SmsErrorResponse.builder()
                .codigoError(e.getCodigo())
                .detalle(e.getMessage())
                .plantilla(e.getPlantilla())
                .errores(e.getErrores())
                .fecha(LocalDateTime.now())
                .build());
    }
}
