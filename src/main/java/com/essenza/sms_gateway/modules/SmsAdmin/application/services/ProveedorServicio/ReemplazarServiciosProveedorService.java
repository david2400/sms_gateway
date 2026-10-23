package com.essenza.sms_gateway.modules.SmsAdmin.application.services.ProveedorServicio;

import com.essenza.sms_gateway.modules.SmsAdmin.application.input.ProveedorServicio.ReemplazarServiciosProveedorUseCase;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorResponse;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Proveedor;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.ProveedorServicioAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.RoutingCacheAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.TipoMensajeAdminPort;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.services.AdminFinder;
import com.essenza.sms_gateway.modules.SmsAdmin.shared.AdminCodigos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reemplaza los servicios de un proveedor.
 * <ul>
 *   <li>Códigos inexistentes o inactivos → 400 con la lista.</li>
 *   <li>Quitar un servicio que usan rutas con cuentas del proveedor → 409 con las rutas (primero quite esas cuentas
 *       de las rutas), para no dejar rutas que el motor ya no podría usar.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReemplazarServiciosProveedorService implements ReemplazarServiciosProveedorUseCase {

    private final AdminFinder finder;
    private final TipoMensajeAdminPort tipos;
    private final ProveedorServicioAdminPort servicios;
    private final RoutingCacheAdminPort routingCache;

    @Override
    public ServiciosProveedorResponse reemplazar(Long proveedorId, ServiciosProveedorRequest request) {
        Proveedor proveedor = finder.proveedor(proveedorId);

        Map<Long, TipoMensaje> nuevos = new LinkedHashMap<>();
        List<String> invalidos = new ArrayList<>();
        for (String codigo : request.getTiposMensaje()) {
            String c = AdminCodigos.normalizar(codigo);
            Optional<TipoMensaje> tipo = c == null ? Optional.empty() : tipos.findByCodigo(c);
            if (tipo.isEmpty() || Boolean.TRUE.equals(tipo.get().getEliminado())) {
                invalidos.add(codigo);
            } else {
                nuevos.put(tipo.get().getId(), tipo.get());
            }
        }
        if (!invalidos.isEmpty()) {
            throw new AdminValidationException("Tipos de mensaje inexistentes o inactivos: " + invalidos, invalidos);
        }

        List<String> enUso = new ArrayList<>();
        for (TipoMensaje actual : servicios.tiposDe(proveedorId)) {
            if (!nuevos.containsKey(actual.getId())) {
                servicios.rutasQueUsan(proveedorId, actual.getId())
                        .forEach(r -> enUso.add(actual.getCodigo() + " → ruta " + r));
            }
        }
        if (!enUso.isEmpty()) {
            throw new AdminConflictException("No se pueden quitar servicios que usan rutas con cuentas de "
                    + proveedor.getCodigo() + ": " + String.join("; ", enUso)
                    + ". Quite primero esas cuentas de las rutas (PUT /rutas/{id}/pasos)");
        }

        servicios.reemplazar(proveedorId, nuevos.keySet());
        log.info("[SMS-ADMIN] Servicios de {}: {}", proveedor.getCodigo(),
                nuevos.values().stream().map(TipoMensaje::getCodigo).toList());
        routingCache.invalidarAlConfirmar();
        return new ServiciosProveedorResponse(proveedor.getId(), proveedor.getCodigo(),
                nuevos.values().stream().map(TipoMensaje::getCodigo).sorted().toList());
    }
}
