package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.mappers.inalambria;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Serializador personalizado para que Object[] se serialice como [] en lugar de [Ljava.lang.Object;@xxxxx
 */
@Component
public class EmptyArraySerializer extends JsonSerializer<String[]> {

    @Override
    public void serialize(String[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        gen.writeEndArray();
    }

    /**
     * Crea un módulo Jackson con este serializador
     */
    public static SimpleModule createModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(String[].class, new EmptyArraySerializer());
        return module;
    }
}
