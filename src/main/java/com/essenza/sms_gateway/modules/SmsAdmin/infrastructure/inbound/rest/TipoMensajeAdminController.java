package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.ActualizarTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.CambiarEstadoTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.CrearTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.EliminarTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.ListarTiposMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje.ObtenerTipoMensajeUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.EstadoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;
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

/** Administración del catálogo de tipos de mensaje ({@code sms_tipo_mensaje}). */
@RestController
@RequestMapping("/v1/sms/admin/tipos-mensaje")
@RequiredArgsConstructor
@Tag(name = "SMS Admin - Tipos de mensaje", description = "Catálogo de tipos con sus capacidades (OTP, masivo, canal)")
public class TipoMensajeAdminController {

    private final ListarTiposMensajeUseCase listarTiposMensaje;
    private final ObtenerTipoMensajeUseCase obtenerTipoMensaje;
    private final CrearTipoMensajeUseCase crearTipoMensaje;
    private final ActualizarTipoMensajeUseCase actualizarTipoMensaje;
    private final CambiarEstadoTipoMensajeUseCase cambiarEstadoTipoMensaje;
    private final EliminarTipoMensajeUseCase eliminarTipoMensaje;

    @Operation(summary = "Listar tipos de mensaje",
            description = "Filtros opcionales: canal (SMS/WHATSAPP), otp=true solo tipos OTP, masivo=true solo tipos masivos")
    @GetMapping
    public List<TipoMensajeResponse> listar(
            @RequestParam(name = "canal", required = false) CanalMensaje canal,
            @RequestParam(name = "otp", required = false) Boolean soloOtp,
            @RequestParam(name = "masivo", required = false) Boolean soloMasivo) {
        return listarTiposMensaje.listar(canal, soloOtp, soloMasivo);
    }

    @Operation(summary = "Obtener tipo de mensaje por id")
    @GetMapping("/{id}")
    public TipoMensajeResponse obtener(@PathVariable Long id) {
        return obtenerTipoMensaje.obtener(id);
    }

    @Operation(summary = "Obtener tipo de mensaje por código", description = "El código es el valor de tipo_mensaje (ej. OTP)")
    @GetMapping("/codigo/{codigo}")
    public TipoMensajeResponse obtenerPorCodigo(@PathVariable String codigo) {
        return obtenerTipoMensaje.obtenerPorCodigo(codigo);
    }

    @Operation(summary = "Crear tipo de mensaje", description = "El código se guarda en mayúsculas y no puede repetirse")
    @PostMapping
    public ResponseEntity<TipoMensajeResponse> crear(@Valid @RequestBody TipoMensajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearTipoMensaje.crear(request));
    }

    @Operation(summary = "Actualizar tipo de mensaje", description = "El código no se puede cambiar: lo referencian las rutas")
    @PutMapping("/{id}")
    public TipoMensajeResponse actualizar(@PathVariable Long id, @Valid @RequestBody TipoMensajeRequest request) {
        return actualizarTipoMensaje.actualizar(id, request);
    }

    @Operation(summary = "Activar / desactivar tipo de mensaje")
    @PatchMapping("/{id}/estado")
    public TipoMensajeResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return cambiarEstadoTipoMensaje.cambiarEstado(id, request.isEliminado());
    }

    @Operation(summary = "Eliminar tipo de mensaje", description = "No se puede eliminar si existen rutas que lo usan")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eliminarTipoMensaje.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
