package io.github.izanMG.boe;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.izanMG.boe.internal.dto.RespuestaBoeDto;
import io.github.izanMG.boe.internal.http.BoeHttpClient;
import io.github.izanMG.boe.internal.json.JsonConfig;
import io.github.izanMG.boe.internal.mapper.SumarioMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Punto de entrada de la libreria.
 *
 * <pre>
 * BoeClient boe = BoeClient.create();
 * Optional&lt;Sumario&gt; sumario = boe.sumarioDe(LocalDate.of(2026, 9, 2));
 * </pre>
 */
public final class BoeClient {

    private static final String URL_BASE_OFICIAL =
            "https://boe.es/datosabiertos/api/boe/sumario";

    private final BoeHttpClient http;

    private BoeClient(BoeHttpClient http) {
        this.http = http;
    }

    /** Cliente con la configuracion por defecto. */
    public static BoeClient create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Devuelve todo lo publicado en el BOE una fecha concreta.
     *
     * @return vacio si ese dia no hubo boletin (por ejemplo, un domingo).
     * @throws BoeException si falla la consulta o la respuesta no se puede leer.
     */
    public Optional<Sumario> sumarioDe(LocalDate fecha) {
        return http.descargarSumario(fecha).map(this::parsear);
    }

    private Sumario parsear(String json) {
        try {
            RespuestaBoeDto dto = JsonConfig.mapper().readValue(json, RespuestaBoeDto.class);
            return SumarioMapper.aSumario(dto);
        } catch (JsonProcessingException e) {
            throw new BoeException("La respuesta del BOE no tiene el formato esperado", e);
        }
    }

    public static final class Builder {

        private String urlBase = URL_BASE_OFICIAL;
        private Duration timeout = Duration.ofSeconds(30);
        private int reintentos = 3;

        private Builder() {
        }

        /** Util para tests; por defecto apunta a la API oficial. */
        public Builder urlBase(String urlBase) {
            this.urlBase = urlBase;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder reintentos(int reintentos) {
            this.reintentos = reintentos;
            return this;
        }

        public BoeClient build() {
            return new BoeClient(new BoeHttpClient(urlBase, timeout, reintentos));
        }
    }
}