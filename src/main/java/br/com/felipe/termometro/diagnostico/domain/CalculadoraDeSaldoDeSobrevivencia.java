package br.com.felipe.termometro.diagnostico.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * RN-08:
 * <pre>
 *   ComprometidoFixo   = CustoFixoTotal + compromissos futuros do mês (parcelas de cartão)
 *   MinimoVariavel     = PisoVariavelTotal
 *   ServicoDivida      = Σ parcelas de dívida ativas no mês
 *   TotalComprometido  = ComprometidoFixo + MinimoVariavel + ServicoDivida
 *   Saldo              = RendaLiquida − TotalComprometido
 *
 *   se Saldo &lt; 0: RendaExtraNecessaria = arredondar_para_cima(|Saldo|, 50)
 * </pre>
 *
 * <p>Domínio puro — os agregados já chegam prontos de {@code catalogo} e {@code ingestao}; esta
 * classe só sabe somar e decidir o déficit.
 */
public final class CalculadoraDeSaldoDeSobrevivencia {

    /** RN-08: déficit arredondado para cima no múltiplo de R$ 50 — "R$ 380 vira R$ 400". */
    private static final Dinheiro MULTIPLO_DE_ARREDONDAMENTO = Dinheiro.de(50);

    private CalculadoraDeSaldoDeSobrevivencia() {
    }

    public static SaldoDeSobrevivencia calcular(Competencia competencia, Dinheiro rendaLiquida,
            Dinheiro custoFixoTotal, Dinheiro compromissosFuturosDoMes, Dinheiro pisoVariavelTotal,
            Dinheiro servicoDivida) {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(rendaLiquida, "renda líquida não pode ser nula");
        Objects.requireNonNull(custoFixoTotal, "custo fixo total não pode ser nulo");
        Objects.requireNonNull(compromissosFuturosDoMes, "compromissos futuros não podem ser nulos");
        Objects.requireNonNull(pisoVariavelTotal, "piso variável total não pode ser nulo");
        Objects.requireNonNull(servicoDivida, "serviço da dívida não pode ser nulo");

        Dinheiro comprometidoFixo = custoFixoTotal.somar(compromissosFuturosDoMes);
        Dinheiro totalComprometido = comprometidoFixo.somar(pisoVariavelTotal).somar(servicoDivida);
        Dinheiro saldo = rendaLiquida.subtrair(totalComprometido);
        boolean deficit = saldo.ehNegativo();
        Dinheiro rendaExtraNecessaria = deficit
                ? saldo.absoluto().arredondarParaCima(MULTIPLO_DE_ARREDONDAMENTO)
                : Dinheiro.ZERO;

        return new SaldoDeSobrevivencia(competencia, rendaLiquida, comprometidoFixo, pisoVariavelTotal,
                servicoDivida, totalComprometido, saldo, deficit, rendaExtraNecessaria);
    }
}
