package io.github.izanmg.boe.internal.http;

import io.github.izanMG.boe.BoeException;

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

    private final HttpClient httpClient;
    private final String urlBase;
    private final Duration timeout;

    public BoeHttpClient(String urlBase, Duration timeout) {
        this.urlBase = normalizar(urlBase);
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Descarga el sumario de una fecha.
     *
     * @return el JSON en crudo, o vacio si ese dia no hubo boletin (404).
     * @throws BoeException si la peticion falla por cualquier otro motivo.
     */
    public Optional<String> descargarSumario(LocalDate fecha) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/" + fecha.format(FORMATO_FECHA)))
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> response = enviar(request, fecha);

        int codigo = response.statusCode();

        if (codigo == 200) {
            return Optional.of(response.body());
        }

        if (codigo == 404) {
            return Optional.empty();
        }

        throw new BoeException(
                "La API del BOE respondio " + codigo + " para la fecha " + fecha);
    }

    private HttpResponse<String> enviar(HttpRequest request, LocalDate fecha) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (IOException e) {
            throw new BoeException("Fallo de red consultando el BOE del " + fecha, e);

        } catch (InterruptedException e) {
            // restaurar el flag para no tragarse la interrupcion del hilo
            Thread.currentThread().interrupt();
            throw new BoeException("Consulta interrumpida", e);
        }
    }

    /**
     * MockWebServer devuelve la url base con barra final y el BOE no la lleva.
     * Sin esto saldrian rutas con doble barra.
     */
    private static String normalizar(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}