package io.github.izanMG.boe;

import java.util.Optional;

public record Disposicion(
        String identificador,
        String titulo,
        Seccion seccion ,
        String departamento,
        Optional<String> epigrafe,
        String urlPdf,
        String urlHtml
) {
}
