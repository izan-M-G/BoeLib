package io.github.izanMG.boe;

import java.util.List;

public record Diario(
        String numero,
        String identificador,
        String urlPdf,
        List<Disposicion> disposiciones
) {
}
