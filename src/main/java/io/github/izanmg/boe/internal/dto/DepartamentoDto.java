package io.github.izanmg.boe.internal.dto;


import java.util.List;

public record DepartamentoDto(
        String codigo,
        String nombre,
        List<EpigrafeDto> epigrafe,
        List<ItemDto> item,
        TextoDto texto
) {}

