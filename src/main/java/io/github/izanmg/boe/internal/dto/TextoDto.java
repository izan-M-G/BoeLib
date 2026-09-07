package io.github.izanmg.boe.internal.dto;

import java.util.List;

public record TextoDto(
        List<DepartamentoDto> departamento,
        List<EpigrafeDto> epigrafe,
        List<ItemDto> item
) {}
