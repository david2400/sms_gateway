package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.config;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaErrorResponse;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaConflictoException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaNoEncontradaException;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception.PlantillaValidacionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Errores de la administración de plantillas (solo este módulo). Los errores de envío/previsualización
 * ({@code SmsContenidoException}) los traduce SmsCore.
 */
@RestControllerAdvice(basePackages = "com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.inbound.rest")
public class GestionPlantillasExceptionHandler {

    @ExceptionHandler(PlantillaNoEncontradaException.class)
    public ResponseEntity<PlantillaErrorResponse> notFound(PlantillaNoEncontradaException e) {
        return error(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage(), List.of());
    }

    @ExceptionHandler(PlantillaConflictoException.class)
    public ResponseEntity<PlantillaErrorResponse> conflict(PlantillaConflictoException e) {
        return error(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage(), List.of());
    }

    @ExceptionHandler(PlantillaValidacionException.class)
    public ResponseEntity<PlantillaErrorResponse> validation(PlantillaValidacionException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDACION", e.getMessage(), e.getDetalles());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PlantillaErrorResponse> invalid(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDACION", "Datos inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PlantillaErrorResponse> unreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDACION", "Request mal formado: revise el JSON (ej. tipo_dato)", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<PlantillaErrorResponse> integrity(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "CONFLICTO", "El cambio viola una restricción de la base de datos (código o variable duplicada)", List.of());
    }

    private static ResponseEntity<PlantillaErrorResponse> error(HttpStatus status, String codigo, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status).body(new PlantillaErrorResponse(codigo, mensaje, detalles, LocalDateTime.now()));
    }
}
