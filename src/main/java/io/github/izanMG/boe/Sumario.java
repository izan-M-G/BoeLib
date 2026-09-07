package io.github.izanMG.boe;

import java.time.LocalDate;
import java.util.List;

public record Sumario(LocalDate fecha , List<Diario> diarios) {
}
