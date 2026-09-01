package br.com.felipe.termometro.compromissofuturo.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * RN-04: uma parcela futura de uma compra já contratada — não é gasto do mês, é saída fixa que
 * vai cair independente de qualquer decisão. Gerado por {@link GeradorDeCompromissosFuturos} a
 * partir da parcela mais recente vista na ingestão, nunca digitado manualmente.
 *
 * <p>{@code id} é atribuído pela infra na gravação (segue o mesmo padrão de {@code Divida}); o
 * domínio trabalha sem ele até a persistência.
 *
 * @param categoria dica herdada da parcela âncora — a mesma limitação de
 *                   {@code TransacaoJpaEntity.categoria}: string de propósito, sem FK
 * @param confirmado sempre {@code true} nesta versão — todo compromisso vem de uma transação
 *                    real já sincronizada, nunca de uma estimativa (schema da spec permite
 *                    compromissos não confirmados, cenário fora de escopo aqui)
 */
public record CompromissoFuturo(@Nullable UUID id, String identificadorConta, String descricao,
                                 String descricaoNormalizada, @Nullable String categoria,
                                 Competencia competencia, Dinheiro valor, int parcelaNumero,
                                 int parcelaTotal, boolean confirmado) {

    public CompromissoFuturo {
        Objects.requireNonNull(identificadorConta, "identificador da conta não pode ser nulo");
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        Objects.requireNonNull(descricaoNormalizada, "descrição normalizada não pode ser nula");
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (identificadorConta.isBlank()) {
            throw new IllegalArgumentException("identificador da conta não pode ser vazio");
        }
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor do compromisso precisa ser positivo: " + valor);
        }
        if (parcelaNumero < 1 || parcelaTotal < 2 || parcelaNumero > parcelaTotal) {
            throw new IllegalArgumentException(
                    "parcela inválida: " + parcelaNumero + "/" + parcelaTotal);
        }
    }

    public CompromissoFuturo comId(UUID novoId) {
        return new CompromissoFuturo(novoId, identificadorConta, descricao, descricaoNormalizada,
                categoria, competencia, valor, parcelaNumero, parcelaTotal, confirmado);
    }
}
