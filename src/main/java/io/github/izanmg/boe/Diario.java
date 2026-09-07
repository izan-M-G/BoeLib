package io.github.izanmg.boe;

import java.util.List;

public record Diario(
        String numero,
        String identificador,
        String urlPdf,
        List<Disposicion> disposiciones
) {
}
