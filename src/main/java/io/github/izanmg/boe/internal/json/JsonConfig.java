package io.github.izanmg.boe.internal.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonConfig {

    private static final ObjectMapper INSTANCIA = crear();

    private JsonConfig() {
    }

    /**
     * El JSON del BOE viene convertido desde XML: un nodo con un unico hijo
     * llega como objeto suelto en vez de array, de ahi SINGLE_VALUE_AS_ARRAY.
     * Y se ignoran claves nuevas para que la libreria no se rompa si el BOE
     * añade campos.
     */
    private static ObjectMapper crear() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static ObjectMapper mapper() {
        return INSTANCIA;
    }
}