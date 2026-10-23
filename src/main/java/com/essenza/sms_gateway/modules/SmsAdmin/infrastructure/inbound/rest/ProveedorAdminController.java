package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ListarProveedoresUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ObtenerProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.CrearProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ActualizarProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.CambiarEstadoProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor.ListarProveedoresImplementadosUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.ProveedorServicio.ListarServiciosProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.application.input.ProveedorServicio.ReemplazarServiciosProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.EstadoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;
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
import java.util.Set;

/** Administración de proveedores ({@code sms_proveedor}). */
@RestController
@RequestMapping("/v1/sms/admin/proveedores")
@RequiredArgsConstructor
@Tag(name = "SMS Admin - Proveedores", description = "Alta, modificación y activación de proveedores SMS")
public class ProveedorAdminController {

    private final ListarProveedoresUseCase listarProveedores;
    private final ObtenerProveedorUseCase obtenerProveedor;
    private final CrearProveedorUseCase crearProveedor;
    private final ActualizarProveedorUseCase actualizarProveedor;
    private final CambiarEstadoProveedorUseCase cambiarEstadoProveedor;
    private final ListarProveedoresImplementadosUseCase listarImplementados;
    private final ListarServiciosProveedorUseCase listarServicios;
    private final ReemplazarServiciosProveedorUseCase reemplazarServicios;

    @Operation(summary = "Listar proveedores")
    @GetMapping
    public List<ProveedorResponse> listar() {
        return listarProveedores.listar();
    }

    @Operation(summary = "Proveedores con módulo implementado", description = "Códigos que el motor sabe enviar (SmsSenderPort registrados)")
    @GetMapping("/implementados")
    public Set<String> implementados() {
        return listarImplementados.implementados();
    }

    @Operation(summary = "Obtener proveedor")
    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return obtenerProveedor.obtener(id);
    }

    @Operation(summary = "Crear proveedor")
    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearProveedor.crear(request));
    }

    @Operation(summary = "Actualizar proveedor")
    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        return actualizarProveedor.actualizar(id, request);
    }

    @Operation(summary = "Activar / desactivar proveedor",
            description = "Desactivado, el motor salta todas sus cuentas. No hay borrado porque el log de envíos lo referencia.")
    @PatchMapping("/{id}/estado")
    public ProveedorResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return cambiarEstadoProveedor.cambiarEstado(id, request.isEliminado());
    }

    @Operation(summary = "Servicios del proveedor", description = "Tipos de mensaje que atiende (SmsProveedorServicio)")
    @GetMapping("/{id}/servicios")
    public ServiciosProveedorResponse servicios(@PathVariable Long id) {
        return listarServicios.listar(id);
    }

    @Operation(summary = "Reemplazar servicios del proveedor",
            description = "Lista completa de tipos de mensaje que atiende. El motor rechaza (422 SERVICIO_NO_SOPORTADO) los envíos "
                    + "de otros tipos por este proveedor y lo salta en el fallback. 409 si se quita un tipo que usan rutas "
                    + "con cuentas del proveedor.")
    @PutMapping("/{id}/servicios")
    public ServiciosProveedorResponse reemplazarServicios(@PathVariable Long id, @Valid @RequestBody ServiciosProveedorRequest request) {
        return reemplazarServicios.reemplazar(id, request);
    }
}
