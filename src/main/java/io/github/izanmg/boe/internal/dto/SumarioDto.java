package io.github.izanmg.boe.internal.dto;

import java.util.List;

public record SumarioDto(MetadatosDto metadatos, List<DiarioDto> diario) {}
