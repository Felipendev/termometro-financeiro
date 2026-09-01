package br.com.felipe.termometro.classificacao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * O cartão de contexto da fila de revisão (RN-12).
 *
 * <p>A descrição sozinha não classifica; o contexto classifica. O que o usuário precisa ver para
 * lembrar o que foi uma compra de três semanas atrás: quando foi, que dia da semana, que horas,
 * quanto custou, e <b>quantas outras iguais existem</b> — porque é o tamanho do grupo que
 * transforma uma decisão em quarenta.
 *
 * @param similaresNoGrupo quantas transações serão reclassificadas junto se ele aplicar ao grupo
 */
public record ContextoDeRevisao(
        UUID id,
        String descricao,
        String descricaoOriginal,
        String grupoDeSimilaridade,
        Dinheiro valor,
        LocalDate data,
        DayOfWeek diaDaSemana,
        @Nullable PeriodoDoDia periodo,
        boolean horaConfiavel,
        int similaresNoGrupo,
        Dinheiro ticketMedioDoGrupo,
        List<SugestaoDeCategoria> sugestoes) {

    public ContextoDeRevisao {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        Objects.requireNonNull(grupoDeSimilaridade, "grupo não pode ser nulo");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(data, "data não pode ser nula");
        sugestoes = List.copyOf(sugestoes);
    }

    public Optional<PeriodoDoDia> periodoOpcional() {
        return Optional.ofNullable(periodo);
    }

    /** Frase pronta para a tela: "sábado à noite · R$ 68,00 · 6 outras iguais". */
    public String resumo() {
        StringBuilder texto = new StringBuilder(diaDaSemanaEmPortugues());
        periodoOpcional().ifPresent(p -> texto.append(" à ").append(p.rotulo()));
        texto.append(" · ").append(valor.absoluto());
        if (similaresNoGrupo > 0) {
            texto.append(" · ").append(similaresNoGrupo)
                 .append(similaresNoGrupo == 1 ? " outra igual" : " outras iguais")
                 .append(" (ticket médio ").append(ticketMedioDoGrupo.absoluto()).append(")");
        }
        return texto.toString();
    }

    private String diaDaSemanaEmPortugues() {
        return switch (diaDaSemana) {
            case MONDAY -> "segunda";
            case TUESDAY -> "terça";
            case WEDNESDAY -> "quarta";
            case THURSDAY -> "quinta";
            case FRIDAY -> "sexta";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }
}
