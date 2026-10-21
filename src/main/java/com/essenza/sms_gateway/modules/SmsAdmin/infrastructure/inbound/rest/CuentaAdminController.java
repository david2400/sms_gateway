package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.ListarCuentasUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.ObtenerCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.CrearCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.ActualizarCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta.CambiarEstadoCuentaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.EstadoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administración de cuentas ({@code sms_cuenta}). */
@RestController
@RequestMapping("/v1/sms/admin/cuentas")
@RequiredArgsConstructor
@Tag(name = "SMS Admin - Cuentas", description = "Cuentas de cada proveedor (la contraseña va en una variable de entorno)")
public class CuentaAdminController {

    private final ListarCuentasUseCase listarCuentas;
    private final ObtenerCuentaUseCase obtenerCuenta;
    private final CrearCuentaUseCase crearCuenta;
    private final ActualizarCuentaUseCase actualizarCuenta;
    private final CambiarEstadoCuentaUseCase cambiarEstadoCuenta;

    @Operation(summary = "Listar cuentas", description = "Filtro opcional por proveedor")
    @GetMapping
    public List<CuentaResponse> listar(@RequestParam(name = "proveedor_id", required = false) Long proveedorId) {
        return listarCuentas.listar(proveedorId);
    }

    @Operation(summary = "Obtener cuenta")
    @GetMapping("/{id}")
    public CuentaResponse obtener(@PathVariable Long id) {
        return obtenerCuenta.obtener(id);
    }

    @Operation(summary = "Crear cuenta", description = "La contraseña va en 'clave' y se guarda cifrada (AES-256, nunca se devuelve). Alternativa: secret_ref = nombre de una variable de entorno")
    @PostMapping
    public ResponseEntity<CuentaResponse> crear(@Valid @RequestBody CuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearCuenta.crear(request));
    }

    @Operation(summary = "Actualizar cuenta")
    @PutMapping("/{id}")
    public CuentaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CuentaRequest request) {
        return actualizarCuenta.actualizar(id, request);
    }

    @Operation(summary = "Activar / desactivar cuenta",
            description = "Desactivada, el motor la salta en todas las rutas. No hay borrado porque el log de envíos la referencia.")
    @PatchMapping("/{id}/estado")
    public CuentaResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return cambiarEstadoCuenta.cambiarEstado(id, request.getActivo());
    }
}
