package br.com.felipe.termometro.ingestao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Um lançamento como o banco emitiu, já com o sinal normalizado pela RN-01
 * (<b>saída negativa, entrada positiva</b>) e antes de qualquer classificação.
 *
 * @param data            data do lançamento
 * @param dataHora        instante da compra, quando a fonte fornece (RN-12); {@code null} em PDF
 * @param descricao       descrição exibível, já sem a cidade colada quando aplicável
 * @param descricaoOriginal texto exato do banco — nunca sobrescrito, é a prova de origem
 * @param valor           valor com sinal normalizado
 * @param cidade          cidade informada pela fatura, quando houver
 * @param categoriaBanco  categoria que o próprio banco atribuiu (o Itaú fornece); é <i>dica</i>,
 *                        nunca decisão — a classificação é nossa (RN-05)
 * @param secao           seção da fatura de onde veio
 * @param parcela         parcela, quando a descrição indicar
 * @param origem          fonte do dado, para desempate de duplicata
 * @param ordinal         posição entre lançamentos idênticos do mesmo dia (RN-02)
 */
public record TransacaoBruta(
        LocalDate data,
        @Nullable LocalDateTime dataHora,
        String descricao,
        String descricaoOriginal,
        Dinheiro valor,
        @Nullable String cidade,
        @Nullable String categoriaBanco,
        SecaoFatura secao,
        @Nullable Parcela parcela,
        Origem origem,
        int ordinal,
        @Nullable UUID eventoId) {

    public TransacaoBruta(LocalDate data, @Nullable LocalDateTime dataHora, String descricao,
                          String descricaoOriginal, Dinheiro valor, @Nullable String cidade,
                          @Nullable String categoriaBanco, SecaoFatura secao, @Nullable Parcela parcela,
                          Origem origem, int ordinal) {
        this(data, dataHora, descricao, descricaoOriginal, valor, cidade, categoriaBanco, secao,
                parcela, origem, ordinal, null);
    }

    public TransacaoBruta {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        Objects.requireNonNull(descricaoOriginal, "descrição original não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(secao, "seção não pode ser nula");
        Objects.requireNonNull(origem, "origem não pode ser nula");
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal não pode ser negativo: " + ordinal);
        }
    }

    public Optional<Parcela> parcelaOpcional() {
        return Optional.ofNullable(parcela);
    }

    public Optional<String> categoriaDoBanco() {
        return Optional.ofNullable(categoriaBanco);
    }

    /** Se a hora veio da fonte e pode alimentar a análise de período do dia (RN-13). */
    public boolean horaConfiavel() {
        return dataHora != null;
    }

    /** Despesa é valor negativo (RN-01). Pagamento de fatura e estorno são positivos. */
    public boolean ehDespesa() {
        return valor.ehNegativo();
    }

    public boolean compoeTotalDaFatura() {
        return secao.compoeTotal();
    }

    public TransacaoBruta comOrdinal(int novoOrdinal) {
        return new TransacaoBruta(data, dataHora, descricao, descricaoOriginal, valor, cidade,
                categoriaBanco, secao, parcela, origem, novoOrdinal, eventoId);
    }
}
