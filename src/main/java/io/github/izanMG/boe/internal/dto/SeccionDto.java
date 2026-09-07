package io.github.izanMG.boe.internal.dto;

import java.util.List;

public record SeccionDto(
        String codigo,
        String nombre,
        List<DepartamentoDto> departamento,
        TextoDto texto
) {}
