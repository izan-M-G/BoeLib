package io.github.izanMG.boe.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SumarioDiarioDto(
        String identificador,
        @JsonProperty("url_pdf") UrlPdfDto urlPdf) {
}
