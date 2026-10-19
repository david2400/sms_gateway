package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.Paso;

import java.util.List;

/** Persistencia de {@code sms_ruta_paso}. */
public interface RutaPasoAdminPort {
    List<Paso> findByRuta(Long rutaId);
    /** Reemplaza todos los pasos de la ruta (borra los anteriores en la misma transacción). */
    List<Paso> replace(Long rutaId, List<Paso> pasos);
    void deleteByRuta(Long rutaId);
    /** Ids de las rutas donde se usa la cuenta. */
    List<Long> rutasQueUsanCuenta(Long cuentaId);
}
