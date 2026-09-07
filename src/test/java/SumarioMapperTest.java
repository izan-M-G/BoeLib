
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.izanmg.boe.Diario;
import io.github.izanmg.boe.Disposicion;
import io.github.izanmg.boe.Seccion;
import io.github.izanmg.boe.Sumario;
import io.github.izanmg.boe.internal.dto.*;
import io.github.izanmg.boe.internal.mapper.SumarioMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SumarioMapperTest {

    private RespuestaBoeDto respuesta;

    @BeforeEach
    void cargarFixture() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        InputStream is = getClass().getResourceAsStream("/fixtures/dos-diarios.json");
        respuesta = mapper.readValue(is, RespuestaBoeDto.class);
    }

    private DiarioDto diario(int indice) {
        return respuesta.data().sumario().diario().get(indice);
    }

    // ---------- estructura general ----------

    @Test
    @DisplayName("El fichero tiene dos diarios: 217 y 216")
    void hayDosDiarios() {
        var diarios = respuesta.data().sumario().diario();
        assertEquals(2, diarios.size());
        assertEquals("217", diarios.get(0).numero());
        assertEquals("216", diarios.get(1).numero());
    }

    @Test
    @DisplayName("Una seccion suelta y una lista de secciones se leen igual")
    void seccionObjetoYSeccionLista() {
        assertEquals(1, diario(0).seccion().size());
        assertEquals(7, diario(1).seccion().size());
    }

    // ---------- departamentosDe ----------
    @Test
    @DisplayName("aSumario extrae la fecha y los dos diarios")
    void aSumarioDevuelveFechaYDiarios() {
        Sumario sumario = SumarioMapper.aSumario(respuesta);

        assertEquals(LocalDate.of(2026, 9, 2), sumario.fecha());

        assertEquals(2, sumario.diarios().size());

        Diario primero = sumario.diarios().get(0);
        assertEquals("217", primero.numero());
        assertEquals("BOE-S-2026-217", primero.identificador());
        assertTrue(primero.urlPdf().toString().endsWith("BOE-S-2026-217.pdf"));

        assertEquals("216", sumario.diarios().get(1).numero());
    }
    @Test
    @DisplayName("Departamentos por la via normal")
    void departamentosDirectos() {
        SeccionDto seccion = seccionPorCodigo(diario(1), "1");
        List<DepartamentoDto> deptos = SumarioMapper.departamentosDe(seccion);

        assertEquals(2, deptos.size());
        assertEquals("JEFATURA DEL ESTADO", deptos.get(0).nombre());
    }

    // ---------- aSumario ----------

    @Test
    @DisplayName("La disposicion mas enterrada del diario 217 llega completa")
    void disposicionPorElCaminoDeTexto() {
        Sumario sumario = SumarioMapper.aSumario(respuesta);
        Diario d217 = sumario.diarios().get(0);

        assertEquals(1, d217.disposiciones().size());

        Disposicion d = d217.disposiciones().get(0);
        assertEquals("BOE-A-2026-18508", d.identificador());
        assertEquals(Seccion.OTRAS_DISPOSICIONES, d.seccion());
        assertEquals("PRESIDENCIA DEL GOBIERNO", d.departamento());
        assertEquals("Situación de interés para la seguridad nacional",
                d.epigrafe().orElseThrow());
    }

    @Test
    @DisplayName("Una disposicion con epigrafe conserva todos sus datos")
    void disposicionConEpigrafe() {
        Sumario sumario = SumarioMapper.aSumario(respuesta);

        Disposicion d = sumario.diarios().get(1).disposiciones().stream()
                .filter(x -> "BOE-A-2026-18429".equals(x.identificador()))
                .findFirst()
                .orElseThrow();

        assertEquals(Seccion.DISPOSICIONES_GENERALES, d.seccion());
        assertEquals("JEFATURA DEL ESTADO", d.departamento());
        assertEquals("Medidas urgentes", d.epigrafe().orElseThrow());
        assertTrue(d.titulo().startsWith("Real Decreto-ley 22/2026"));
        assertTrue(d.urlPdf().toString().contains("BOE-A-2026-18429"));
    }

    @Test
    @DisplayName("Los anuncios sin epigrafe llegan con el Optional vacio")
    void disposicionSinEpigrafe() {
        Sumario sumario = SumarioMapper.aSumario(respuesta);

        Disposicion anuncio = sumario.diarios().get(1).disposiciones().stream()
                .filter(x -> "BOE-B-2026-28404".equals(x.identificador()))
                .findFirst()
                .orElseThrow();

        assertEquals(Seccion.ANUNCIOS_OFICIALES, anuncio.seccion());
        assertEquals("MINISTERIO DE TRANSPORTES Y MOVILIDAD SOSTENIBLE",
                anuncio.departamento());
        assertTrue(anuncio.epigrafe().isEmpty());
    }

    @Test
    @DisplayName("No se pierde ninguna disposicion por el camino")
    void seRecogenTodasLasDisposiciones() {
        Sumario sumario = SumarioMapper.aSumario(respuesta);

        assertEquals(1, sumario.diarios().get(0).disposiciones().size());
        assertEquals(144, sumario.diarios().get(1).disposiciones().size());

        long sinEpigrafe = sumario.diarios().get(1).disposiciones().stream()
                .filter(d -> d.epigrafe().isEmpty())
                .count();

        assertEquals(65, sinEpigrafe);
    }

    @Test
    @DisplayName("Ninguna disposicion queda con seccion desconocida")
    void todasLasSeccionesReconocidas() {
        Sumario sumario = SumarioMapper.aSumario(respuesta);

        boolean hayDesconocida = sumario.diarios().stream()
                .flatMap(d -> d.disposiciones().stream())
                .anyMatch(d -> d.seccion() == Seccion.DESCONOCIDA);

        assertFalse(hayDesconocida);
    }

    @Test
    @DisplayName("Departamentos envueltos en texto")
    void departamentosDentroDeTexto() {
        SeccionDto seccion = diario(0).seccion().get(0);

        assertNull(seccion.departamento(), "esta seccion no trae departamento directo");

        List<DepartamentoDto> deptos = SumarioMapper.departamentosDe(seccion);
        assertEquals(1, deptos.size());
        assertEquals("PRESIDENCIA DEL GOBIERNO", deptos.get(0).nombre());
    }

    // ---------- epigrafesDe ----------

    @Test
    @DisplayName("Epigrafes por la via normal")
    void epigrafesDirectos() {
        DepartamentoDto depto = SumarioMapper
                .departamentosDe(seccionPorCodigo(diario(1), "1"))
                .get(0);

        assertEquals(1, SumarioMapper.epigrafesDe(depto).size());
    }

    @Test
    @DisplayName("Epigrafes envueltos en texto")
    void epigrafesDentroDeTexto() {
        DepartamentoDto depto = SumarioMapper
                .departamentosDe(diario(0).seccion().get(0))
                .get(0);

        assertNull(depto.epigrafe(), "este departamento no trae epigrafe directo");

        var epigrafes = SumarioMapper.epigrafesDe(depto);
        assertEquals(1, epigrafes.size());
        assertEquals("Situación de interés para la seguridad nacional",
                epigrafes.get(0).nombre());
    }

    // ---------- itemsDirectosDe ----------

    @Test
    @DisplayName("Un item suelto y una lista de items se leen igual")
    void itemsDirectosObjetoYLista() {
        var deptos = SumarioMapper.departamentosDe(seccionPorCodigo(diario(1), "5B"));

        assertEquals(1, SumarioMapper.itemsDirectosDe(deptos.get(0)).size());
        assertEquals(2, SumarioMapper.itemsDirectosDe(deptos.get(1)).size());
    }

    @Test
    @DisplayName("Un departamento con epigrafes no tiene items directos")
    void sinItemsDirectos() {
        DepartamentoDto depto = SumarioMapper
                .departamentosDe(seccionPorCodigo(diario(1), "1"))
                .get(0);

        assertTrue(SumarioMapper.itemsDirectosDe(depto).isEmpty());
    }

    // ---------- itemsDe ----------

    @Test
    @DisplayName("El camino completo del diario 217 llega hasta la disposicion")
    void caminoCompletoPorTexto() {
        var depto = SumarioMapper.departamentosDe(diario(0).seccion().get(0)).get(0);
        var epigrafe = SumarioMapper.epigrafesDe(depto).get(0);
        var items = SumarioMapper.itemsDe(epigrafe);

        assertEquals(1, items.size());
        assertEquals("BOE-A-2026-18508", items.get(0).identificador());
    }

    // ---------- auxiliar ----------

    private SeccionDto seccionPorCodigo(DiarioDto diario, String codigo) {
        return diario.seccion().stream()
                .filter(s -> codigo.equals(s.codigo()))
                .findFirst()
                .orElseThrow();
    }
}