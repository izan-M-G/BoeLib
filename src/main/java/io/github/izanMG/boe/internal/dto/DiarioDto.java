package io.github.izanMG.boe.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DiarioDto(
        String numero,
        String indice,
        @JsonProperty("sumario_diario") SumarioDiarioDto sumarioDiario,
        List<SeccionDto> seccion
) {
}
