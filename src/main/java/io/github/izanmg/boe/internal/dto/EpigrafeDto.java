package io.github.izanmg.boe.internal.dto;

import java.util.List;

public record EpigrafeDto(
        String nombre,
        List<ItemDto> item
) {}
