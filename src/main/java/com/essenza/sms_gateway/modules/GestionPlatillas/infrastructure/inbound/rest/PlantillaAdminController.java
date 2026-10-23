package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.inbound.rest;

import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.ActualizarPlantillaUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.CambiarEstadoPlantillaUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.CrearPlantillaUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.ListarPlantillasUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.application.input.ObtenerPlantillaUseCase;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.EstadoPlantillaRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaRequest;
import com.essenza.sms_gateway.modules.GestionPlatillas.domain.dto.PlantillaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/** Administración de plantillas SMS ({@code SmsPlantilla} + {@code SmsPlantillaVariable}). */
@RestController
@RequestMapping("/v1/sms/admin/plantillas")
@RequiredArgsConstructor
@Tag(name = "SMS Admin - Plantillas", description = "Plantillas de texto con variables {{nombre}} tipadas y validadas")
public class PlantillaAdminController {

    private final ListarPlantillasUseCase listarPlantillas;
    private final ObtenerPlantillaUseCase obtenerPlantilla;
    private final CrearPlantillaUseCase crearPlantilla;
    private final ActualizarPlantillaUseCase actualizarPlantilla;
    private final CambiarEstadoPlantillaUseCase cambiarEstadoPlantilla;

    @Operation(summary = "Listar plantillas", description = "Filtros opcionales: activo, tipo_mensaje")
    @GetMapping
    public List<PlantillaResponse> listar(@RequestParam(name = "activo", required = false) Boolean activo,
                                          @RequestParam(name = "tipo_mensaje", required = false) String tipoMensaje) {
        return listarPlantillas.listar(activo, tipoMensaje);
    }

    @Operation(summary = "Obtener plantilla por id")
    @GetMapping("/{id}")
    public PlantillaResponse obtener(@PathVariable Long id) {
        return obtenerPlantilla.obtener(id);
    }

    @Operation(summary = "Obtener plantilla por código")
    @GetMapping("/codigo/{codigo}")
    public PlantillaResponse obtenerPorCodigo(@PathVariable String codigo) {
        return obtenerPlantilla.obtenerPorCodigo(codigo);
    }

    @Operation(summary = "Crear plantilla",
            description = "Valida sintaxis {{variable}}, que toda variable usada esté declarada y viceversa, patrones y valores "
                    + "por defecto. Sin 'variables' se crean desde el contenido (TEXTO, obligatorias). 400 con la lista de errores.")
    @PostMapping
    public ResponseEntity<PlantillaResponse> crear(@Valid @RequestBody PlantillaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearPlantilla.crear(request));
    }

    @Operation(summary = "Actualizar plantilla", description = "Reemplaza contenido y variables; el código no cambia y la versión sube en 1")
    @PutMapping("/{id}")
    public PlantillaResponse actualizar(@PathVariable Long id, @Valid @RequestBody PlantillaRequest request) {
        return actualizarPlantilla.actualizar(id, request);
    }

    @Operation(summary = "Activar / desactivar plantilla", description = "eliminado=true desactiva (los envíos con ella responden 422)")
    @PatchMapping("/{id}/estado")
    public PlantillaResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoPlantillaRequest request) {
        return cambiarEstadoPlantilla.cambiarEstado(id, request.getEliminado());
    }
}
