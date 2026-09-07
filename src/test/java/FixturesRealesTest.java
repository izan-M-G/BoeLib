
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.izanMG.boe.Diario;
import io.github.izanMG.boe.Disposicion;
import io.github.izanMG.boe.Seccion;
import io.github.izanMG.boe.Sumario;
import io.github.izanMG.boe.internal.dto.RespuestaBoeDto;
import io.github.izanMG.boe.internal.json.JsonConfig;
import io.github.izanMG.boe.internal.mapper.SumarioMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FixturesRealesTest {

    private static final ObjectMapper MAPPER = JsonConfig.mapper();

    private Sumario cargar(String fichero) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + fichero)) {
            assertNotNull(is, "No se encuentra el fixture " + fichero);
            return SumarioMapper.aSumario(MAPPER.readValue(is, RespuestaBoeDto.class));
        }
    }

    private List<Disposicion> todasLasDisposiciones(Sumario sumario) {
        return sumario.diarios().stream()
                .flatMap(d -> d.disposiciones().stream())
                .toList();
    }

    // ---------- dia normal ----------

    @Test
    @DisplayName("Un dia corriente: un solo diario y las ocho secciones")
    void diaSimple() throws IOException {
        Sumario sumario = cargar("dia-simple.json");

        assertEquals(1, sumario.diarios().size());

        Set<Seccion> secciones = todasLasDisposiciones(sumario).stream()
                .map(Disposicion::seccion)
                .collect(Collectors.toSet());

        assertEquals(8, secciones.size());
        assertTrue(secciones.contains(Seccion.ADMINISTRACION_DE_JUSTICIA),
                "la seccion 4 aparece en este fixture");
        assertFalse(secciones.contains(Seccion.DESCONOCIDA));
    }

    @Test
    @DisplayName("Ninguna disposicion llega sin identificador ni titulo")
    void ningunaDisposicionIncompleta() throws IOException {
        for (Disposicion d : todasLasDisposiciones(cargar("dia-simple.json"))) {
            assertNotNull(d.identificador());
            assertFalse(d.identificador().isBlank());
            assertNotNull(d.titulo());
            assertFalse(d.titulo().isBlank());
            assertNotNull(d.departamento());
            assertNotNull(d.urlPdf());
        }
    }

    @Test
    @DisplayName("Los anuncios no tienen epigrafe y las disposiciones generales si")
    void epigrafeSegunLaSeccion() throws IOException {
        List<Disposicion> todas = todasLasDisposiciones(cargar("dia-simple.json"));

        assertTrue(todas.stream()
                .filter(d -> d.seccion() == Seccion.DISPOSICIONES_GENERALES)
                .allMatch(d -> d.epigrafe().isPresent()));

        assertTrue(todas.stream()
                .filter(d -> d.seccion() == Seccion.ANUNCIOS_OFICIALES)
                .anyMatch(d -> d.epigrafe().isEmpty()));
    }

    // ---------- 1990 ----------

    @Test
    @DisplayName("El formato de 1990 es el mismo que el de hoy")
    void fechaAntigua() throws IOException {
        Sumario sumario = cargar("fecha-antigua.json");

        assertEquals(LocalDate.of(1990, 1, 2), sumario.fecha());
        assertEquals(1, sumario.diarios().size());
        assertFalse(todasLasDisposiciones(sumario).isEmpty());

        assertTrue(todasLasDisposiciones(sumario).stream()
                .noneMatch(d -> d.seccion() == Seccion.DESCONOCIDA));
    }

    // ---------- boletin minimo ----------

    @Test
    @DisplayName("Un extraordinario de domingo con una sola seccion no rompe nada")
    void domingoConBoletin() throws IOException {
        Sumario sumario = cargar("domingo-con-boletin.json");

        assertEquals(LocalDate.of(2020, 3, 29), sumario.fecha());
        assertEquals(1, sumario.diarios().size());

        List<Disposicion> todas = todasLasDisposiciones(sumario);
        assertFalse(todas.isEmpty());

        assertTrue(todas.stream()
                .allMatch(d -> d.seccion() == Seccion.DISPOSICIONES_GENERALES));
    }

    // ---------- comparativa ----------

    @Test
    @DisplayName("Todos los fixtures se leen sin lanzar nada")
    void todosLosFixturesSeLeen() {
        for (String f : List.of("dia-simple.json", "dos-diarios.json",
                "fecha-antigua.json", "domingo-con-boletin.json")) {
            assertDoesNotThrow(() -> cargar(f), "fallo leyendo " + f);
        }
    }
    @Test
    void imprimirTotales() throws IOException {
        for (String f : List.of("dia-simple.json", "fecha-antigua.json",
                "domingo-con-boletin.json")) {
            System.out.println(f + " -> " + todasLasDisposiciones(cargar(f)).size());
        }
    }
}