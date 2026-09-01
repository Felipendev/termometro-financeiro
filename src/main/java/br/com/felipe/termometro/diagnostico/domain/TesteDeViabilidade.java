package br.com.felipe.termometro.diagnostico.domain;

import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * RN-16 — a regra mais importante do sistema: dá para guardar a meta declarada com o padrão de
 * vida atual, ou o padrão precisa cair?
 *
 * <pre>
 *   CustoMinimoVida = CustoFixoTotal + PisoVariavelTotal
 *   EconomiaMaxima  = RendaLiquida − CustoMinimoVida
 *   TaxaMaxima      = EconomiaMaxima / RendaLiquida
 * </pre>
 *
 * <p>{@code TaxaMaxima} é o teto: o quanto sobraria executando com perfeição absoluta, gastando
 * exatamente o piso em tudo. Domínio puro — sem Spring, sem banco, só as duas premissas que
 * {@code catalogo} já entregou somadas.
 */
public final class TesteDeViabilidade {

    /** RN-16.1: só compara duas medianas de 3 meses se houver os 6 meses inteiros. */
    private static final int MESES_POR_JANELA = 3;
    private static final int MESES_MINIMOS_PARA_DETECTAR_QUEDA = MESES_POR_JANELA * 2;
    /** Gatilho: mediana atual abaixo de 85% da mediana anterior. */
    private static final BigDecimal LIMITE_DE_QUEDA = new BigDecimal("0.85");

    private TesteDeViabilidade() {
    }

    public static Viabilidade calcular(Competencia competencia, Dinheiro rendaLiquida,
            Dinheiro custoFixoTotal, Dinheiro pisoVariavelTotal, Percentual metaEconomia,
            List<Renda> historicoDeRenda) {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(rendaLiquida, "renda líquida não pode ser nula");
        Objects.requireNonNull(custoFixoTotal, "custo fixo total não pode ser nulo");
        Objects.requireNonNull(pisoVariavelTotal, "piso variável total não pode ser nulo");
        Objects.requireNonNull(metaEconomia, "meta de economia não pode ser nula");
        Objects.requireNonNull(historicoDeRenda, "histórico de renda não pode ser nulo");
        if (rendaLiquida.ehZero() || rendaLiquida.ehNegativo()) {
            throw new IllegalArgumentException(
                    "renda líquida precisa ser positiva para calcular viabilidade: " + rendaLiquida);
        }

        Dinheiro custoMinimoVida = custoFixoTotal.somar(pisoVariavelTotal);
        Dinheiro economiaMaxima = rendaLiquida.subtrair(custoMinimoVida);
        Percentual taxaMaxima = economiaMaxima.sobre(rendaLiquida);
        Veredito veredito = veredito(taxaMaxima, metaEconomia);
        Dinheiro alvoReducaoFixo = metaEconomia.aplicarSobre(rendaLiquida).subtrair(economiaMaxima);

        QuedaDeRenda quedaDeRenda = detectaQuedaDeRenda(custoFixoTotal, historicoDeRenda);

        String leitura = leitura(veredito, taxaMaxima, metaEconomia, custoMinimoVida, rendaLiquida,
                alvoReducaoFixo);

        return new Viabilidade(competencia, rendaLiquida, custoFixoTotal, pisoVariavelTotal,
                custoMinimoVida, economiaMaxima, taxaMaxima, metaEconomia, veredito,
                alvoReducaoFixo, quedaDeRenda, leitura);
    }

    private static Veredito veredito(Percentual taxaMaxima, Percentual metaEconomia) {
        if (taxaMaxima.maiorOuIgualA(metaEconomia)) {
            return Veredito.VIAVEL;
        }
        if (!taxaMaxima.ehNegativo() && !taxaMaxima.equals(Percentual.ZERO)) {
            return Veredito.VIAVEL_PARCIALMENTE;
        }
        return Veredito.INVIAVEL;
    }

    private static String leitura(Veredito veredito, Percentual taxaMaxima, Percentual metaEconomia,
            Dinheiro custoMinimoVida, Dinheiro rendaLiquida, Dinheiro alvoReducaoFixo) {
        return switch (veredito) {
            case VIAVEL -> "Dá para bater a meta de %s sem mexer no padrão de vida — no cenário de "
                    .formatted(metaEconomia.formatado())
                    + "execução perfeita, a economia máxima é %s da renda. O gap está no amarelo e "
                    .formatted(taxaMaxima.formatado())
                    + "no vermelho: é execução, não estrutura.";
            case VIAVEL_PARCIALMENTE -> ("No melhor cenário possível você guarda %s, abaixo da meta "
                    + "de %s. Para chegar lá, o custo fixo precisa cair R$ %s.")
                    .formatted(taxaMaxima.formatado(), metaEconomia.formatado(),
                            alvoReducaoFixo.valor().toPlainString());
            case INVIAVEL -> ("O custo mínimo de vida (R$ %s) excede a renda líquida (R$ %s). "
                    + "Nenhuma disciplina de gasto resolve isso — é estrutural, e a saída passa por "
                    + "reduzir o custo fixo ou aumentar a renda.")
                    .formatted(custoMinimoVida.valor().toPlainString(), rendaLiquida.valor().toPlainString());
        };
    }

    /**
     * RN-16.1. {@code historicoDeRenda} vem da mais recente para a mais antiga; sem os 6 meses
     * inteiros, a detecção simplesmente não dispara (edge case 30) — não estima com o que falta.
     */
    private static @Nullable QuedaDeRenda detectaQuedaDeRenda(Dinheiro custoFixoTotal,
            List<Renda> historicoDeRenda) {
        if (historicoDeRenda.size() < MESES_MINIMOS_PARA_DETECTAR_QUEDA) {
            return null;
        }
        Dinheiro rendaAtual = mediana(historicoDeRenda.subList(0, MESES_POR_JANELA));
        Dinheiro rendaAnterior = mediana(
                historicoDeRenda.subList(MESES_POR_JANELA, MESES_POR_JANELA * 2));

        if (rendaAnterior.ehZero() || rendaAtual.ehZero()) {
            return null;
        }
        Percentual proporcaoAtual = rendaAtual.sobre(rendaAnterior);
        if (proporcaoAtual.maiorOuIgualA(Percentual.deFracao(LIMITE_DE_QUEDA))) {
            return null;
        }
        Percentual quedaPct = Percentual.CEM.subtrair(proporcaoAtual);

        Percentual pesoFixoAntes = custoFixoTotal.sobre(rendaAnterior);
        Percentual pesoFixoAgora = custoFixoTotal.sobre(rendaAtual);
        Dinheiro excedenteEstrutural = custoFixoTotal.subtrair(pesoFixoAntes.aplicarSobre(rendaAtual));

        String mensagem = ("Sua renda caiu %s (de R$ %s para R$ %s). Um custo fixo que pesava %s da "
                + "renda antiga passa a pesar %s da nova, sem que nada tenha sido gasto a mais. Isso "
                + "é aritmética, não falta de disciplina.")
                .formatted(quedaPct.formatado(), rendaAnterior.valor().toPlainString(),
                        rendaAtual.valor().toPlainString(), pesoFixoAntes.formatado(),
                        pesoFixoAgora.formatado());

        return new QuedaDeRenda(rendaAnterior, rendaAtual, quedaPct, pesoFixoAntes, pesoFixoAgora,
                excedenteEstrutural, mensagem);
    }

    /** Mediana, não média (edge case 31): um mês misto não contamina as duas janelas. */
    private static Dinheiro mediana(List<Renda> janela) {
        List<Dinheiro> valores = new ArrayList<>(janela.stream().map(Renda::valorLiquido).toList());
        valores.sort(Comparator.naturalOrder());
        int meio = valores.size() / 2;
        if (valores.size() % 2 == 0) {
            return valores.get(meio - 1).somar(valores.get(meio)).dividirPor(BigDecimal.valueOf(2));
        }
        return valores.get(meio);
    }
}
