package br.com.felipe.termometro.contribuicao.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;
import java.util.Optional;

/**
 * RN-28.1 — o sistema mede, não decide: só propõe o próximo passo quando o colchão de segurança
 * sobra de verdade no mês seguinte, considerando o que a contribuição atual já consome.
 *
 * <p>{@code saldoProjetado} não sabe nada sobre dízimo/oferta (RN-08 não conhece este módulo) —
 * por isso a contribuição atual em reais é subtraída aqui, não lá.
 */
public final class CalculadoraDeAutorizacaoDeContribuicao {

    private CalculadoraDeAutorizacaoDeContribuicao() {
    }

    public static Optional<ProximoPassoContribuicao> avalia(
            MetaContribuicao meta,
            Competencia proximaCompetencia,
            Dinheiro saldoProjetado,
            Dinheiro rendaLiquida,
            Dinheiro contribuicaoAtualTotalEmReais,
            Dinheiro colchaoMinimo) {
        Objects.requireNonNull(meta, "meta não pode ser nula");
        Objects.requireNonNull(proximaCompetencia, "próxima competência não pode ser nula");
        Objects.requireNonNull(saldoProjetado, "saldo projetado não pode ser nulo");
        Objects.requireNonNull(rendaLiquida, "renda líquida não pode ser nula");
        Objects.requireNonNull(contribuicaoAtualTotalEmReais, "contribuição atual não pode ser nula");
        Objects.requireNonNull(colchaoMinimo, "colchão mínimo não pode ser nulo");

        if (meta.jaAtingiuOAlvo()) {
            return Optional.empty();
        }

        Dinheiro disponivelReal = saldoProjetado.subtrair(contribuicaoAtualTotalEmReais);
        if (!disponivelReal.maiorQue(colchaoMinimo)) {
            return Optional.empty();
        }

        Percentual proximoPercentual = proximoPercentual(meta);
        Dinheiro valorProposto = proximoPercentual.aplicarSobre(rendaLiquida);
        return Optional.of(new ProximoPassoContribuicao(meta.nome(), proximaCompetencia, proximoPercentual, valorProposto));
    }

    private static Percentual proximoPercentual(MetaContribuicao meta) {
        Percentual somado = meta.percentualAtual().somar(meta.passoIncremento());
        return somado.menorQue(meta.percentualAlvo()) ? somado : meta.percentualAlvo();
    }
}
