package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.PasoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de los pasos de una ruta:
 * <ul>
 *   <li>Orden y cuenta no se repiten.</li>
 *   <li>En una ruta de proveedor todas las cuentas son de ese proveedor (la central puede mezclar).</li>
 *   <li>Valores por defecto: 3 intentos, 500 ms de espera, activo.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PasoRutaValidator {

    public static final int DEFAULT_MAX_INTENTOS = 3;
    public static final int DEFAULT_ESPERA_MS = 500;

    private final AdminFinder finder;

    public List<Paso> validar(Ruta ruta, List<PasoRequest> requests) {
        Set<Integer> ordenes = new HashSet<>();
        Set<Long> cuentasUsadas = new HashSet<>();
        List<Paso> pasos = new ArrayList<>();
        for (PasoRequest r : requests) {
            if (!ordenes.add(r.getOrden())) {
                throw new AdminValidationException("El orden " + r.getOrden() + " está repetido");
            }
            if (!cuentasUsadas.add(r.getCuentaId())) {
                throw new AdminValidationException("La cuenta " + r.getCuentaId() + " está repetida en la ruta");
            }
            Cuenta cuenta = finder.cuentaReferenciada(r.getCuentaId());
            if (!ruta.isCentral() && !ruta.getProveedorId().equals(cuenta.getProveedorId())) {
                throw new AdminValidationException("La cuenta " + cuenta.getCodigo() + " es de " + cuenta.getProveedorCodigo()
                        + " y la ruta es de " + ruta.getProveedorCodigo() + "; use la ruta central para mezclar proveedores");
            }
            pasos.add(Paso.builder()
                    .rutaId(ruta.getId())
                    .orden(r.getOrden())
                    .cuentaId(cuenta.getId())
                    .maxIntentos(r.getMaxIntentos() != null ? r.getMaxIntentos() : DEFAULT_MAX_INTENTOS)
                    .esperaMs(r.getEsperaMs() != null ? r.getEsperaMs() : DEFAULT_ESPERA_MS)
                    .activo(r.getActivo() == null || r.getActivo())
                    .build());
        }
        return pasos;
    }
}
