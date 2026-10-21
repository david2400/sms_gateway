package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.model.TipoMensaje;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;

import java.util.List;
import java.util.Optional;

/** Persistencia de {@code sms_tipo_mensaje}. */
public interface TipoMensajeAdminPort {

    /**
     * @param canal      filtro opcional por canal (SMS, WHATSAPP)
     * @param soloOtp    true = solo tipos con PIN/código de un solo uso
     * @param soloMasivo true = solo tipos aptos para envíos masivos
     */
    List<TipoMensaje> findAll(CanalMensaje canal, Boolean soloOtp, Boolean soloMasivo);
    Optional<TipoMensaje> findById(Long id);
    /** El código se busca normalizado (mayúsculas). */
    Optional<TipoMensaje> findByCodigo(String codigo);
    TipoMensaje save(TipoMensaje tipo);
    void delete(Long id);
}
