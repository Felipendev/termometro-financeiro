package br.com.felipe.termometro.naogasto.domain;

import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Um lançamento já persistido, na forma que o motor de conciliação (RN-03) precisa: com
 * identidade, conta de origem e o sinal original — a decisão de casamento depende de valores
 * opostos, e a triagem/classificação já convertem para valor absoluto antes de chegar lá.
 *
 * @param identificadorConta chave estável da conta (RN-02) — é o que distingue "veio da corrente"
 *                           de "veio do cartão", já que contas diferentes têm identificadores
 *                           diferentes
 * @param valor              com sinal (RN-01): saída negativa, entrada positiva. Essencial aqui,
 *                            diferente de {@code TransacaoClassificada} da triagem
 * @param secao               {@link SecaoFatura#MOVIMENTO_CONTA} identifica lançamento de conta
 *                            corrente/poupança; qualquer outro valor veio de fatura de cartão
 */
public record LancamentoParaConciliar(
        UUID id, String identificadorConta, LocalDate data, Dinheiro valor, String descricao,
        @Nullable String cidade, SecaoFatura secao) {

    public LancamentoParaConciliar {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(identificadorConta, "identificadorConta não pode ser nulo");
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(descricao, "descricao não pode ser nula");
        Objects.requireNonNull(secao, "secao não pode ser nula");
        if (identificadorConta.isBlank()) {
            throw new IllegalArgumentException("identificadorConta não pode ser vazio");
        }
    }

    public boolean veioDeContaCorrente() {
        return secao == SecaoFatura.MOVIMENTO_CONTA;
    }

    public boolean veioDeFaturaDeCartao() {
        return !veioDeContaCorrente();
    }

    public Optional<String> cidadeOpcional() {
        return Optional.ofNullable(cidade);
    }
}
