package br.com.felipe.termometro.ingestao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * O que um leitor de fatura devolve: os lançamentos, a conferência contra o total impresso e os
 * avisos do que não deu para ler. Nunca lança exceção por linha ilegível — uma linha estranha
 * vira aviso, e a reconciliação decide se o arquivo é confiável como um todo.
 */
public record ResultadoDaLeitura(
        List<TransacaoBruta> transacoes,
        @Nullable Reconciliacao reconciliacao,
        List<String> avisos) {

    public ResultadoDaLeitura {
        Objects.requireNonNull(transacoes, "transações não podem ser nulas");
        Objects.requireNonNull(avisos, "avisos não podem ser nulos");
        transacoes = List.copyOf(transacoes);
        avisos = List.copyOf(avisos);
    }

    public Optional<Reconciliacao> conferencia() {
        return Optional.ofNullable(reconciliacao);
    }

    /** Soma dos lançamentos que compõem o total desta fatura, em módulo. */
    public Dinheiro totalDeDespesas() {
        return Dinheiro.somaDe(transacoes.stream()
                .filter(TransacaoBruta::compoeTotalDaFatura)
                .map(TransacaoBruta::valor)
                .toList()).absoluto();
    }

    public List<TransacaoBruta> compromissosFuturos() {
        return transacoes.stream().filter(t -> t.secao() == SecaoFatura.FUTURO).toList();
    }

    /** Confiável quando não há reconciliação a fazer, ou quando ela fecha. */
    public boolean confiavel() {
        return reconciliacao == null || reconciliacao.fecha();
    }
}
