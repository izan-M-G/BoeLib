

import io.github.izanmg.boe.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BoeClientTest {

    private MockWebServer servidor;
    private BoeClient cliente;

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 2);

    @BeforeEach
    void preparar() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        cliente = BoeClient.builder()
                .urlBase(servidor.url("/").toString())
                .timeout(Duration.ofSeconds(5))
                .reintentos(2)
                .build();
    }

    @AfterEach
    void apagar() throws IOException {
        servidor.shutdown();
    }

    private String fixture() throws IOException {
        try (var is = getClass().getResourceAsStream("/fixtures/dos-diarios.json")) {
            assertNotNull(is, "No se encuentra el fixture");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("De la peticion HTTP al modelo publico, sin costuras")
    void sumarioCompleto() throws IOException {
        servidor.enqueue(new MockResponse().setResponseCode(200).setBody(fixture()));

        Sumario sumario = cliente.sumarioDe(FECHA).orElseThrow();

        assertEquals(FECHA, sumario.fecha());
        assertEquals(2, sumario.diarios().size());
        assertEquals(144, sumario.diarios().get(1).disposiciones().size());

        Disposicion d = sumario.diarios().get(0).disposiciones().get(0);
        assertEquals("BOE-A-2026-18508", d.identificador());
        assertEquals(Seccion.OTRAS_DISPOSICIONES, d.seccion());
    }
    @Test
    @DisplayName("Una fecha futura devuelve vacio, igual que un domingo sin boletin")
    void fechaFutura() {
        servidor.enqueue(new MockResponse().setResponseCode(404));

        assertTrue(cliente.sumarioDe(LocalDate.of(2027, 12, 31)).isEmpty());
    }
    @Test
    @DisplayName("Un dia sin boletin devuelve vacio, no explota")
    void diaSinBoletin() {
        servidor.enqueue(new MockResponse().setResponseCode(404));

        Optional<Sumario> sumario = cliente.sumarioDe(FECHA);

        assertTrue(sumario.isEmpty());
    }

    @Test
    @DisplayName("Dos fallos del servidor y a la tercera funciona")
    void reintentaTrasError500() throws IOException {
        servidor.enqueue(new MockResponse().setResponseCode(500));
        servidor.enqueue(new MockResponse().setResponseCode(503));
        servidor.enqueue(new MockResponse().setResponseCode(200).setBody(fixture()));

        Sumario sumario = cliente.sumarioDe(FECHA).orElseThrow();

        assertEquals(2, sumario.diarios().size());
        assertEquals(3, servidor.getRequestCount());
    }

    @Test
    @DisplayName("Si el servidor no se recupera, se agotan los reintentos")
    void agotaReintentos() {
        servidor.enqueue(new MockResponse().setResponseCode(500));
        servidor.enqueue(new MockResponse().setResponseCode(500));
        servidor.enqueue(new MockResponse().setResponseCode(500));

        BoeException e = assertThrows(BoeException.class, () -> cliente.sumarioDe(FECHA));

        assertTrue(e.getMessage().contains("3 intentos"));
        assertEquals(3, servidor.getRequestCount());
    }

    @Test
    @DisplayName("Un 404 no se reintenta")
    void noReintentaEl404() {
        servidor.enqueue(new MockResponse().setResponseCode(404));

        cliente.sumarioDe(FECHA);

        assertEquals(1, servidor.getRequestCount());
    }

    @Test
    @DisplayName("Un JSON corrupto da un error claro, no uno de Jackson")
    void jsonInvalido() {
        servidor.enqueue(new MockResponse().setResponseCode(200).setBody("esto no es json"));

        BoeException e = assertThrows(BoeException.class, () -> cliente.sumarioDe(FECHA));

        assertTrue(e.getMessage().contains("formato esperado"));
    }
}