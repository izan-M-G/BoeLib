package io.github.izanMG.boe.internal.mapper;

import io.github.izanMG.boe.Diario;
import io.github.izanMG.boe.Disposicion;
import io.github.izanMG.boe.Seccion;
import io.github.izanMG.boe.Sumario;
import io.github.izanMG.boe.internal.dto.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class SumarioMapper {
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private SumarioMapper() {
        // clase de utilidades, no se instancia
    }

    /**
     * Devuelve la lista tal cual, o una lista vacia si era null.
     * Jackson deja los campos ausentes a null, no a lista vacia.
     */
    private static <T> List<T> noNula(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    /**
     * Los departamentos de una seccion pueden venir en 'departamento'
     * o envueltos en 'texto' cuando el BOE publica uno solo.
     */
    public static List<DepartamentoDto> departamentosDe(SeccionDto seccion) {
        List<DepartamentoDto> resultado = new ArrayList<>(noNula(seccion.departamento()));

        if (seccion.texto() != null) {
            resultado.addAll(noNula(seccion.texto().departamento()));
        }

        return resultado;
    }

    /**
     * Misma dualidad que los departamentos: 'epigrafe' o 'texto.epigrafe'.
     */
    public static List<EpigrafeDto> epigrafesDe(DepartamentoDto departamento) {
        List<EpigrafeDto> resultado = new ArrayList<>(noNula(departamento.epigrafe()));

        if (departamento.texto() != null) {
            resultado.addAll(noNula(departamento.texto().epigrafe()));
        }

        return resultado;
    }

    /**
     * Items que cuelgan del departamento sin pasar por epigrafe,
     * tipicos de la seccion V (anuncios). No llevan epigrafe asociado.
     */
    public static List<ItemDto> itemsDirectosDe(DepartamentoDto departamento) {
        List<ItemDto> resultado = new ArrayList<>(noNula(departamento.item()));

        if (departamento.texto() != null) {
            resultado.addAll(noNula(departamento.texto().item()));
        }

        return resultado;
    }

    /**
     * Items de un epigrafe. Aqui no hay envoltorio 'texto'.
     */
    public static List<ItemDto> itemsDe(EpigrafeDto epigrafe) {
        return List.copyOf(noNula(epigrafe.item()));
    }


    public static Sumario aSumario(RespuestaBoeDto respuesta) {

        LocalDate fecha = LocalDate.parse(
                respuesta.data().sumario().metadatos().fechaPublicacion(), FORMATO_FECHA);

        List<Diario> diarios = new ArrayList<>();

        for (DiarioDto diarioDto : respuesta.data().sumario().diario()) {

            List<Disposicion> disposiciones = new ArrayList<>();

            for (SeccionDto seccionDto : diarioDto.seccion()) {

                Seccion seccion = Seccion.desdeCodigo(seccionDto.codigo());

                for (DepartamentoDto departamentoDto : departamentosDe(seccionDto)) {

                    String nombreDepartamento = departamentoDto.nombre();

                    for (ItemDto itemDto : itemsDirectosDe(departamentoDto)) {
                        disposiciones.add(
                                aDisposicion(itemDto, seccion, nombreDepartamento, null));
                    }

                    for (EpigrafeDto epigrafeDto : epigrafesDe(departamentoDto)) {
                        for (ItemDto itemDto : itemsDe(epigrafeDto)) {
                            disposiciones.add(
                                    aDisposicion(itemDto, seccion, nombreDepartamento,
                                            epigrafeDto.nombre()));
                        }
                    }
                }
            }

            diarios.add(new Diario(
                    diarioDto.numero(),
                    diarioDto.sumarioDiario().identificador(),
                    diarioDto.sumarioDiario().urlPdf().texto(),
                    disposiciones
            ));
        }

        return new Sumario(fecha, diarios);
    }

    private static  Disposicion aDisposicion(ItemDto itemDto , Seccion seccion , String nombreDepartamento , String nombreEpigrafe){
        return new Disposicion(
                itemDto.identificador(),
                itemDto.titulo(),
                seccion,
                nombreDepartamento,
                Optional.ofNullable(nombreEpigrafe),
                itemDto.urlPdf().texto(),
                itemDto.urlHtml()
        );
    }


}