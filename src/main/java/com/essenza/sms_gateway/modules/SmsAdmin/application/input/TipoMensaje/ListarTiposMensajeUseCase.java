package com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.CanalMensaje;

import java.util.List;

/** Lista el catálogo de tipos de mensaje con sus capacidades. */
public interface ListarTiposMensajeUseCase {

    /**
     * @param canal      filtro opcional por canal (SMS, WHATSAPP)
     * @param soloOtp    true = solo tipos con PIN/código de un solo uso
     * @param soloMasivo true = solo tipos aptos para envíos masivos
     */
    List<TipoMensajeResponse> listar(CanalMensaje canal, Boolean soloOtp, Boolean soloMasivo);
}
