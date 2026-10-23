package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.AdminErrorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/** Errores de los endpoints de administración (solo aplica a este módulo). */
@Slf4j
@RestControllerAdvice(basePackages = "com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.rest")
public class SmsAdminExceptionHandler {

    @ExceptionHandler(AdminNotFoundException.class)
    public ResponseEntity<AdminErrorResponse> notFound(AdminNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(AdminConflictException.class)
    public ResponseEntity<AdminErrorResponse> conflict(AdminConflictException e) {
        return error(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage(), List.of());
    }

    @ExceptionHandler(AdminValidationException.class)
    public ResponseEntity<AdminErrorResponse> validation(AdminValidationException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDACION", e.getMessage(), e.getDetalles());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AdminErrorResponse> invalid(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDACION", "Datos inválidos", detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<AdminErrorResponse> unreadable(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDACION", "Request mal formado: revise el JSON y los valores (ej. tipo_mensaje)", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<AdminErrorResponse> integrity(DataIntegrityViolationException e) {
        log.warn("[SMS-ADMIN] Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "CONFLICTO", "El cambio viola una restricción de la base de datos (código o ruta duplicada)", List.of());
    }

    private static ResponseEntity<AdminErrorResponse> error(HttpStatus status, String codigo, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status).body(new AdminErrorResponse(codigo, mensaje, detalles, LocalDateTime.now()));
    }
}
