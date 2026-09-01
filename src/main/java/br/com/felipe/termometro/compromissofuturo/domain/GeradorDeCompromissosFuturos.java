package br.com.felipe.termometro.compromissofuturo.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RN-04, motor: para cada série de parcelas vista na ingestão, escolhe a âncora (parcela mais
 * recente) e gera um {@link CompromissoFuturo} para cada parcela que ainda não foi sincronizada.
 *
 * <pre>
 * série = (identificadorConta, descricaoNormalizada, parcelaTotal)
 * âncora(série) = lançamento de maior parcelaNumero dentro da série
 * gerados(série) = { competencia = âncora.competencia + (n - âncora.parcelaNumero),
 *                     valor = âncora.valor,
 *                     para n em [âncora.parcelaNumero + 1 .. parcelaTotal] }
 * </pre>
 *
 * <p><b>Aproximação documentada:</b> assume parcela de valor fixo (o caso comum de parcelamento
 * de varejo brasileiro). Parcelamento com juros ou com a última parcela ajustada por
 * arredondamento vai gerar um compromisso levemente diferente do que será cobrado de fato — sem
 * uma "fatura declarada" com o valor de cada parcela futura (não existe essa informação na API
 * do Pluggy nem nos PDFs lidos), essa é a melhor estimativa disponível, e ela se autocorrige a
 * cada mês: quando a parcela real chega, ela vira a nova âncora e substitui a estimativa.
 *
 * <p>Só a parcela âncora entra na série. Uma série cuja âncora já é a última parcela
 * ({@code parcelaNumero == parcelaTotal}) ainda aparece em {@code seriesProcessadas} — sem
 * gerar nenhum {@link CompromissoFuturo} — para que a reconciliação apague qualquer compromisso
 * gerado num mês anterior para essa mesma série.
 */
public final class GeradorDeCompromissosFuturos {

    private GeradorDeCompromissosFuturos() {
    }

    public static ResultadoDaGeracao gera(List<LancamentoParceladoAncora> lancamentos) {
        Objects.requireNonNull(lancamentos, "lançamentos não podem ser nulos");

        Map<ChaveDeSerie, LancamentoParceladoAncora> ancoraPorSerie = lancamentos.stream()
                .collect(Collectors.toMap(LancamentoParceladoAncora::chave, l -> l,
                        GeradorDeCompromissosFuturos::maisRecente));

        List<CompromissoFuturo> gerados = new ArrayList<>();
        for (LancamentoParceladoAncora ancora : ancoraPorSerie.values()) {
            for (int n = ancora.parcelaNumero() + 1; n <= ancora.parcelaTotal(); n++) {
                gerados.add(new CompromissoFuturo(null, ancora.identificadorConta(), ancora.descricao(),
                        ancora.descricaoNormalizada(), ancora.categoria(),
                        ancora.competencia().mais(n - ancora.parcelaNumero()), ancora.valor().absoluto(),
                        n, ancora.parcelaTotal(), true));
            }
        }

        Set<ChaveDeSerie> seriesProcessadas = new HashSet<>(ancoraPorSerie.keySet());
        return new ResultadoDaGeracao(gerados, seriesProcessadas);
    }

    /** Entre duas visões da mesma série, vence a parcela de maior número; empate, a mais recente. */
    private static LancamentoParceladoAncora maisRecente(LancamentoParceladoAncora a,
            LancamentoParceladoAncora b) {
        Comparator<LancamentoParceladoAncora> porParcelaEDepoisData = Comparator
                .comparingInt(LancamentoParceladoAncora::parcelaNumero)
                .thenComparing(LancamentoParceladoAncora::competencia);
        return porParcelaEDepoisData.compare(a, b) >= 0 ? a : b;
    }
}
