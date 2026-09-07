
import io.github.izanMG.boe.internal.http.*;
import io.github.izanmg.boe.internal.http.BoeHttpClient;
import io.github.izanMG.boe.BoeException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BoeHttpClientTest {

    private MockWebServer servidor;
    private BoeHttpClient cliente;

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 2);

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        cliente = new BoeHttpClient(
                servidor.url("/").toString(), Duration.ofSeconds(5));
    }

    @AfterEach
    void apagarServidor() throws IOException {
        servidor.shutdown();
    }

    @Test
    @DisplayName("Una respuesta 200 devuelve el cuerpo")
    void respuesta200() {
        servidor.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":{\"code\":\"200\"}}"));

        Optional<String> cuerpo = cliente.descargarSumario(FECHA);

        assertTrue(cuerpo.isPresent());
        assertTrue(cuerpo.get().contains("\"code\":\"200\""));
    }

    @Test
    @DisplayName("Un 404 significa que ese dia no hubo boletin, no un error")
    void respuesta404() {
        servidor.enqueue(new MockResponse().setResponseCode(404));

        Optional<String> cuerpo = cliente.descargarSumario(FECHA);

        assertTrue(cuerpo.isEmpty());
    }

    @Test
    @DisplayName("Un 500 lanza BoeException con el codigo en el mensaje")
    void respuesta500() {
        servidor.enqueue(new MockResponse().setResponseCode(500));

        BoeException e = assertThrows(BoeException.class,
                () -> cliente.descargarSumario(FECHA));

        assertTrue(e.getMessage().contains("500"));
    }

    @Test
    @DisplayName("Un 400 tambien lanza excepcion")
    void respuesta400() {
        servidor.enqueue(new MockResponse().setResponseCode(400));

        assertThrows(BoeException.class, () -> cliente.descargarSumario(FECHA));
    }

    @Test
    @DisplayName("La peticion va a la ruta correcta con la cabecera correcta")
    void peticionBienFormada() throws InterruptedException {
        servidor.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        cliente.descargarSumario(FECHA);

        RecordedRequest peticion = servidor.takeRequest();
        assertEquals("/20260902", peticion.getPath());
        assertEquals("GET", peticion.getMethod());
        assertEquals("application/json", peticion.getHeader("Accept"));
    }

    @Test
    @DisplayName("La url base con barra final no produce doble barra")
    void urlBaseConBarraFinal() throws InterruptedException {
        servidor.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        cliente.descargarSumario(FECHA);

        assertEquals("/20260902", servidor.takeRequest().getPath());
    }
}