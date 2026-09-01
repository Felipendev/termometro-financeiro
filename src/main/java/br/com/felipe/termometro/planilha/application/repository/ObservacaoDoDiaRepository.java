package br.com.felipe.termometro.planilha.application.repository;

import java.time.LocalDate;
import java.util.Map;

public interface ObservacaoDoDiaRepository {
    Map<LocalDate, String> buscaEntre(LocalDate de, LocalDate ate);

    void salva(LocalDate data, String texto);
}
