package com.essenza.sms_gateway.modules.SmsAdmin.domain.services;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminValidationException;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Cuenta;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Ruta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de los pasos de una ruta:
 * <ul>
 *   <li>Orden y cuenta no se repiten.</li>
 *   <li>En una ruta de proveedor todas las cuentas son de ese proveedor (la central puede mezclar).</li>
 *   <li>Valores por defecto: 3 intentos, 500 ms de espera, no eliminado.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PasoRutaValidator {

    public static final int DEFAULT_MAX_INTENTOS = 3;
    public static final int DEFAULT_ESPERA_MS = 500;

    private final AdminFinder finder;

    /**
     * Valida los pasos y completa rutaId y valores por defecto (modifica y devuelve la misma lista).
     */
    public List<Paso> validar(Ruta ruta, List<Paso> pasos) {
        Set<Integer> ordenes = new HashSet<>();
        Set<Long> cuentasUsadas = new HashSet<>();
        for (Paso paso : pasos) {
            if (!ordenes.add(paso.getOrden())) {
                throw new AdminValidationException("El orden " + paso.getOrden() + " está repetido");
            }
            if (!cuentasUsadas.add(paso.getCuentaId())) {
                throw new AdminValidationException("La cuenta " + paso.getCuentaId() + " está repetida en la ruta");
            }
            Cuenta cuenta = finder.cuentaReferenciada(paso.getCuentaId());
            if (!ruta.isCentral() && !ruta.getProveedorId().equals(cuenta.getProveedorId())) {
                throw new AdminValidationException("La cuenta " + cuenta.getCodigo() + " es de " + cuenta.getProveedorCodigo()
                        + " y la ruta es de " + ruta.getProveedorCodigo() + "; use la ruta central para mezclar proveedores");
            }
            paso.setRutaId(ruta.getId());
            if (paso.getMaxIntentos() == null) {
                paso.setMaxIntentos(DEFAULT_MAX_INTENTOS);
            }
            if (paso.getEsperaMs() == null) {
                paso.setEsperaMs(DEFAULT_ESPERA_MS);
            }
            if (paso.getEliminado() == null) {
                paso.setEliminado(Boolean.FALSE);
            }
        }
        return pasos;
    }
}
