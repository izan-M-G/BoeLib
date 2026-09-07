package io.github.izanMG.boe.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadatosDto(String publicacion, @JsonProperty("fecha_publicacion") String fechaPublicacion) {
}
