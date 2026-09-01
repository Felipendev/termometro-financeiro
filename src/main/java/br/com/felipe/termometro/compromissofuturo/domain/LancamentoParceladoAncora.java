package br.com.felipe.termometro.compromissofuturo.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Uma parcela real, já sincronizada, como a ingestão a persistiu — o insumo bruto do gerador.
 * "Âncora" porque, dentro de uma série (mesma conta + mesmo estabelecimento normalizado + mesmo
 * total de parcelas), só a de maior {@code parcelaNumero} importa: é a parcela mais recente
 * vista, e as que faltam depois dela é que viram {@link CompromissoFuturo}.
 *
 * @param descricaoNormalizada a mesma chave de {@code Normalizador.chaveDeEstabelecimento} —
 *                             já vem sem o sufixo "N/T" (RN-02), então duas parcelas da mesma
 *                             compra caem na mesma chave por construção
 */
public record LancamentoParceladoAncora(String identificadorConta, String descricaoNormalizada,
                                         String descricao, @Nullable String categoria,
                                         Competencia competencia, Dinheiro valor, int parcelaNumero,
                                         int parcelaTotal) {

    public LancamentoParceladoAncora {
        Objects.requireNonNull(identificadorConta, "identificador da conta não pode ser nulo");
        Objects.requireNonNull(descricaoNormalizada, "descrição normalizada não pode ser nula");
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (parcelaNumero < 1 || parcelaTotal < 2 || parcelaNumero > parcelaTotal) {
            throw new IllegalArgumentException(
                    "parcela inválida: " + parcelaNumero + "/" + parcelaTotal);
        }
    }

    ChaveDeSerie chave() {
        return new ChaveDeSerie(identificadorConta, descricaoNormalizada, parcelaTotal);
    }
}
