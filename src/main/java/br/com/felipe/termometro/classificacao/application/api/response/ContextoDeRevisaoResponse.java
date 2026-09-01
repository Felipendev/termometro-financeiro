package br.com.felipe.termometro.classificacao.application.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Um cartão da fila de revisão, pronto para a tela. */
public record ContextoDeRevisaoResponse(
        UUID id,
        String descricao,
        String valor,
        LocalDate data,
        String diaDaSemana,
        @Nullable String periodo,
        boolean horaConfiavel,
        String grupoDeSimilaridade,
        int similaresNoGrupo,
        String ticketMedioDoGrupo,
        String resumo,
        List<SugestaoResponse> sugestoes) {

    public record SugestaoResponse(String categoria, String grupo, String natureza,
                                   BigDecimal confianca, String motivo) {
    }
}
