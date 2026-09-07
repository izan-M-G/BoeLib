package io.github.izanmg.boe.internal.http;

import io.github.izanmg.boe.BoeException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class BoeHttpClient {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final long ESPERA_BASE_MS = 500;

    private final HttpClient httpClient;
    private final String urlBase;
    private final Duration timeout;
    private final int reintentos;

    public BoeHttpClient(String urlBase, Duration timeout, int reintentos) {
        this.urlBase = normalizar(urlBase);
        this.timeout = timeout;
        this.reintentos = reintentos;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Descarga el sumario de una fecha.
     *
     * @return el JSON en crudo, o vacio si ese dia no hubo boletin (404).
     * @throws BoeException si falla la red o el BOE devuelve un error.
     */
    public Optional<String> descargarSumario(LocalDate fecha) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/" + fecha.format(FORMATO_FECHA)))
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET()
                .build();

        IOException ultimoFalloDeRed = null;
        int ultimoCodigo = 0;

        for (int intento = 0; intento <= reintentos; intento++) {

            if (intento > 0) {
                esperar(intento);
            }

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            } catch (IOException e) {
                ultimoFalloDeRed = e;
                continue;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BoeException("Consulta al BOE interrumpida", e);
            }

            int codigo = response.statusCode();

            if (codigo == 200) {
                return Optional.of(response.body());
            }

            if (codigo == 404) {
                return Optional.empty();
            }

            if (codigo >= 500) {
                ultimoCodigo = codigo;
                ultimoFalloDeRed = null;
                continue;
            }

            // 4xx: la peticion es incorrecta, reintentar no cambiaria nada
            throw new BoeException(
                    "La API del BOE respondio " + codigo + " para la fecha " + fecha);
        }

        String motivo = ultimoFalloDeRed != null
                ? "fallo de red"
                : "el BOE respondio " + ultimoCodigo;

        throw new BoeException(
                "No se pudo consultar el BOE del " + fecha + " tras "
                        + (reintentos + 1) + " intentos: " + motivo,
                ultimoFalloDeRed);
    }

    /** Espera creciente: 500 ms, 1 s, 2 s... para no machacar un servidor caido. */
    private void esperar(int intento) {
        try {
            Thread.sleep(ESPERA_BASE_MS * (long) Math.pow(2, intento - 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BoeException("Espera entre reintentos interrumpida", e);
        }
    }

    private static String normalizar(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}