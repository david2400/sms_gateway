package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración técnica de Inalambria ({@code sms.inalambria.*}). Las credenciales NO van aquí:
 * se toman de la cuenta ({@code SmsCuenta}, clave cifrada).
 */
@Data
@ConfigurationProperties(prefix = "sms.inalambria")
public class InalambriaProperties {

    private Massive massive = new Massive();

    private Whatsapp whatsapp = new Whatsapp();

    private Bidireccional bidireccional = new Bidireccional();

    /** SMS bidireccional desde código corto: {@code POST /momessage}. */
    @Data
    public static class Bidireccional {
        /** URL del endpoint. Se puede sobrescribir por cuenta con {@code parametros_json.bidireccionalUrl}. */
        private String url = "https://rest.inalambria.com/momessage";
    }

    /** WhatsApp (plantillas de Meta): {@code POST /MultiChannelInput/SendMessageMetaWhatsApp}. */
    @Data
    public static class Whatsapp {
        /** URL del endpoint. Se puede sobrescribir por cuenta con {@code parametros_json.whatsappUrl}. */
        private String url = "https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp";
    }

    /** API REST masiva: {@code POST /mtmessage/massive} (docs.inalambria.com, "Mensaje SMS Unidireccional (Batch)"). */
    @Data
    public static class Massive {
        /** URL del endpoint masivo. Se puede sobrescribir por cuenta con {@code parametros_json.massiveUrl}. */
        private String url = "https://rest.inalambria.com/mtmessage/massive";
        /**
         * Destinatarios por petición. Inalambria no documenta un máximo; 500 por defecto.
         * Se puede sobrescribir por cuenta con {@code parametros_json.massiveChunkSize}.
         */
        private int chunkSize = 500;
    }
}
