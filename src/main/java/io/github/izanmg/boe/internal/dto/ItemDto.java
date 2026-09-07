package io.github.izanmg.boe.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemDto(
        String identificador,
        String control,
        String titulo,
        @JsonProperty("url_pdf") UrlPdfDto urlPdf,
        @JsonProperty("url_html") String urlHtml,
        @JsonProperty("url_xml") String urlXml
) {}