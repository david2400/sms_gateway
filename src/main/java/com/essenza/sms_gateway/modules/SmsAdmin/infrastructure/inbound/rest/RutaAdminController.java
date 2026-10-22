package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ListarRutasUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ObtenerRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.CrearRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.CambiarEstadoRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ReemplazarPasosRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.EliminarRutaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta.ConsultarRutaEfectivaUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.EstadoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.PasosRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaEfectivaResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
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

/** Administración de rutas ({@code sms_ruta}) y sus pasos ({@code sms_ruta_paso}). */
@RestController
@RequestMapping("/v1/sms/admin/rutas")
@RequiredArgsConstructor
@Tag(name = "SMS Admin - Rutas", description = "Orden de cuentas/proveedores e intentos por tipo de mensaje")
public class RutaAdminController {

    private final ListarRutasUseCase listarRutas;
    private final ObtenerRutaUseCase obtenerRuta;
    private final CrearRutaUseCase crearRuta;
    private final CambiarEstadoRutaUseCase cambiarEstadoRuta;
    private final ReemplazarPasosRutaUseCase reemplazarPasos;
    private final EliminarRutaUseCase eliminarRuta;
    private final ConsultarRutaEfectivaUseCase consultarRutaEfectiva;

    @Operation(summary = "Listar rutas", description = "central=true solo devuelve las rutas del endpoint central")
    @GetMapping
    public List<RutaResponse> listar(
            @RequestParam(name = "proveedor_id", required = false) Long proveedorId,
            @RequestParam(name = "central", defaultValue = "false") boolean central,
            @RequestParam(name = "tipo_mensaje", required = false) MessageType tipoMensaje) {
        return listarRutas.listar(proveedorId, central, tipoMensaje);
    }

    @Operation(summary = "Ruta efectiva",
            description = "Muestra la ruta tal como la usará el motor (sin inactivos) y advierte cuentas sin credenciales. "
                    + "proveedor vacío o CENTRAL = endpoint central")
    @GetMapping("/efectiva")
    public RutaEfectivaResponse efectiva(
            @RequestParam(name = "proveedor", required = false) String proveedor,
            @RequestParam(name = "tipo_mensaje") MessageType tipoMensaje) {
        return consultarRutaEfectiva.efectiva(proveedor, tipoMensaje);
    }

    @Operation(summary = "Obtener ruta con sus pasos")
    @GetMapping("/{id}")
    public RutaResponse obtener(@PathVariable Long id) {
        return obtenerRuta.obtener(id);
    }

    @Operation(summary = "Crear ruta", description = "proveedor_id null = ruta del endpoint central. Puede incluir los pasos.")
    @PostMapping
    public ResponseEntity<RutaResponse> crear(@Valid @RequestBody RutaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearRuta.crear(request));
    }

    @Operation(summary = "Reemplazar pasos", description = "Envía la lista completa en el orden deseado; reemplaza los pasos actuales")
    @PutMapping("/{id}/pasos")
    public RutaResponse pasos(@PathVariable Long id, @Valid @RequestBody PasosRequest request) {
        return reemplazarPasos.reemplazarPasos(id, request.getPasos());
    }

    @Operation(summary = "Activar / desactivar ruta")
    @PatchMapping("/{id}/estado")
    public RutaResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return cambiarEstadoRuta.cambiarEstado(id, request.isEliminado());
    }

    @Operation(summary = "Eliminar ruta", description = "Borra la ruta y sus pasos (no afecta el log de envíos)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eliminarRuta.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
