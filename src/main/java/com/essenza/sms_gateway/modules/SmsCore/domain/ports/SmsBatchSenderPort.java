package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;

import java.util.List;

/**
 * SPI OPCIONAL para proveedores con API de envío masivo nativa (ej. Inalambria {@code /mtmessage/massive}).
 * <p>
 * Si el proveedor de un paso de la ruta lo implementa, el motor masivo envía los mensajes en lotes
 * (una llamada HTTP por lote); si no, envía uno por uno con {@link SmsSenderPort}.
 * Mismas reglas que {@link SmsSenderPort}: un intento por llamada, no lanzar excepciones, stateless.
 * El resultado aplica a TODO el lote.
 */
public interface SmsBatchSenderPort {

    /** Debe coincidir con {@code sms_proveedor.codigo}. */
    String providerCode();

    /**
     * Tipos que la API masiva sabe enviar (por defecto solo canal SMS). Para otros tipos el motor envía uno a uno
     * con {@link SmsSenderPort}, que decide el canal.
     */
    default boolean supports(MessageType type) {
        return type != null && type.isSms();
    }

    /** Máximo de mensajes por llamada para la cuenta (el motor parte los envíos grandes). */
    int maxBatchSize(ProviderAccount account);

    /**
     * Envía el lote en una sola llamada (un intento).
     *
     * @param batchId identificador del lote (trazabilidad con el proveedor)
     */
    SendOutcome sendBatch(List<SmsDispatch> messages, ProviderAccount account, String batchId);
}
