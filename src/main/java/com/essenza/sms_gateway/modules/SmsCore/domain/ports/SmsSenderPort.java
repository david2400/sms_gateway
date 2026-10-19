package com.essenza.sms_gateway.modules.SmsCore.domain.ports;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;

/**
 * SPI que implementa CADA módulo de proveedor (Inalambria, AwsSns, Mock...).
 * <p>
 * Reglas para implementarlo:
 * <ul>
 *   <li>Hacer UN solo intento por llamada: los reintentos y el fallback los hace el motor de SmsCore.</li>
 *   <li>No lanzar excepciones: devolver {@link SendOutcome#fail} con el HTTP status (o null si no hubo respuesta).</li>
 *   <li>Ser stateless: toda la configuración llega en {@link ProviderAccount}.</li>
 * </ul>
 */
public interface SmsSenderPort {

    /**
     * Código del proveedor; debe coincidir con {@code sms_proveedor.codigo} (ej. "INALAMBRIA").
     */
    String providerCode();

    /**
     * Envía el mensaje con la cuenta indicada (un intento).
     */
    SendOutcome send(SmsDispatch message, ProviderAccount account);
}
