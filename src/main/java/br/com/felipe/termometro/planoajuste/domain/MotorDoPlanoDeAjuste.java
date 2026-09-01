package br.com.felipe.termometro.planoajuste.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RN-15 — Plano de Ajuste Progressivo. Para cada categoria calcula a mediana dos meses fechados,
 * decide se entra no plano e gera a rampa geométrica em direção ao piso; separadamente, zera a
 * parte vermelha de cada categoria já no mês 1; por fim, prioriza as três ações de maior retorno
 * sobre dor.
 *
 * <p><b>Decisão de design (extrapolação sobre a spec, alinhada com Felipe antes de codar):</b> a
 * spec não diz explicitamente como o vermelho de uma categoria interage com a rampa da mesma
 * categoria. A leitura adotada: o vermelho é um item independente — soma sozinho (RN-05 já dá
 * essa granularidade por transação) e some inteiro no mês 1 — enquanto a parte azul+amarela da
 * mesma categoria segue rampando normalmente até o piso, sem que o vermelho "contamine" essa
 * mediana.
 *
 * <p><b>Piso zero:</b> RN-15 não define o que fazer com uma categoria cujo piso humano é
 * R$ 0,00 — matematicamente a razão da rampa colapsaria para zero e o alvo já seria zero no mês
 * 1, o que não é uma rampa gradual. Em vez de inventar uma fórmula sem base na spec, essas
 * categorias são puladas e um aviso é emitido — decisão documentada, não um bug.
 */
public final class MotorDoPlanoDeAjuste {

    static final int DOR_VERMELHA = 1;
    static final int DOR_AMARELA = 2;
    private static final int ESCALA_IMPACTO = 6;

    private MotorDoPlanoDeAjuste() { }

    public static PlanoDeAjuste gerar(Competencia competenciaInicio, List<GastoDaCategoria> categorias,
            BigDecimal fatorMaxCorte, int mesesRampaSolicitados) {
        List<ItemDoPlano> itens = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        for (GastoDaCategoria categoria : categorias) {
            processaVariavel(categoria, fatorMaxCorte, mesesRampaSolicitados, itens, avisos);
            processaVermelho(categoria, itens);
        }

        Dinheiro economiaTotal =
                Dinheiro.somaDe(itens.stream().map(ItemDoPlano::economiaMensalFinal).toList());
        List<AcaoPrioritaria> acoes = prioriza(itens);

        return new PlanoDeAjuste(competenciaInicio, itens, avisos, acoes, economiaTotal);
    }

    private static void processaVariavel(GastoDaCategoria categoria, BigDecimal fatorMaxCorte,
            int mesesSolicitados, List<ItemDoPlano> itens, List<String> avisos) {
        Dinheiro piso = categoria.piso();
        if (piso == null) {
            avisos.add("Categoria '%s' sem piso humano definido — não entra no plano de ajuste."
                    .formatted(categoria.categoria()));
            return;
        }
        if (categoria.gastosVariaveisPorMes().isEmpty()) {
            return;
        }

        Dinheiro atual = mediana(categoria.gastosVariaveisPorMes());
        if (!atual.maiorQue(piso)) {
            return;
        }
        if (piso.ehZero()) {
            avisos.add(("Categoria '%s' tem piso humano R$ 0,00 — a rampa geométrica não é "
                    + "definida para alvo zero; categoria excluída do plano, requer decisão manual.")
                    .formatted(categoria.categoria()));
            return;
        }

        ResultadoDaRampa resultado =
                CalculadoraDeRampa.calcular(atual, piso, fatorMaxCorte, mesesSolicitados);
        if (resultado.alongada()) {
            BigDecimal fatorPercentual =
                    fatorMaxCorte.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_EVEN);
            avisos.add(("Categoria '%s': rampa de %d meses era curta demais para o limite de %s%% "
                    + "de corte mensal — alongada para %d meses.")
                    .formatted(categoria.categoria(), mesesSolicitados, fatorPercentual,
                            resultado.mesesEfetivos()));
        }

        Dinheiro economiaFinal = atual.subtrair(piso);
        itens.add(new ItemDoPlano(categoria.categoria(), ItemDoPlano.TipoDeItem.RAMPA_VARIAVEL, atual,
                piso, resultado.alvosMensais(), resultado.alongada(), DOR_AMARELA, economiaFinal));
    }

    private static void processaVermelho(GastoDaCategoria categoria, List<ItemDoPlano> itens) {
        if (categoria.gastosVermelhosPorMes().isEmpty()) {
            return;
        }
        Dinheiro atual = mediana(categoria.gastosVermelhosPorMes());
        if (!atual.ehPositivo()) {
            return;
        }
        AlvoMensal mesUnico = new AlvoMensal(1, Dinheiro.ZERO, Percentual.CEM);
        itens.add(new ItemDoPlano(categoria.categoria(), ItemDoPlano.TipoDeItem.ZERAR_VERMELHO, atual,
                Dinheiro.ZERO, List.of(mesUnico), false, DOR_VERMELHA, atual));
    }

    private static List<AcaoPrioritaria> prioriza(List<ItemDoPlano> itens) {
        return itens.stream()
                .map(item -> new AcaoPrioritaria(item.categoria(), descreve(item),
                        item.economiaMensalFinal(), item.dor(), impacto(item)))
                .sorted(Comparator.comparing(AcaoPrioritaria::impacto).reversed())
                .limit(3)
                .toList();
    }

    /**
     * {@code impacto = economiaMensal / dor} (RN-15). Calculado direto no {@link BigDecimal} do
     * valor, não via {@link Dinheiro#dividirPor}: o resultado não é dinheiro, é um fator de
     * ranqueamento, e arredondar para centavos aqui poderia empatar ações com prioridades
     * distintas.
     */
    private static BigDecimal impacto(ItemDoPlano item) {
        return item.economiaMensalFinal().valor()
                .divide(BigDecimal.valueOf(item.dor()), ESCALA_IMPACTO, RoundingMode.HALF_EVEN);
    }

    private static String descreve(ItemDoPlano item) {
        if (item.tipo() == ItemDoPlano.TipoDeItem.ZERAR_VERMELHO) {
            return "Cortar os impulsos reconhecidos em '%s': R$ %s/mês, dor mínima."
                    .formatted(item.categoria(), item.valorAtual().valor().toPlainString());
        }
        AlvoMensal ultimo = item.alvosMensais().get(item.alvosMensais().size() - 1);
        return "'%s' de R$ %s → R$ %s no mês %d: R$ %s/mês ao final."
                .formatted(item.categoria(), item.valorAtual().valor().toPlainString(),
                        item.alvoFinal().valor().toPlainString(), ultimo.mes(),
                        item.economiaMensalFinal().valor().toPlainString());
    }

    /** Mediana, não média (mesma disciplina da RN-16.1): um mês atípico não define a régua. */
    private static Dinheiro mediana(List<Dinheiro> valores) {
        List<Dinheiro> ordenados = new ArrayList<>(valores);
        ordenados.sort(Comparator.naturalOrder());
        int meio = ordenados.size() / 2;
        if (ordenados.size() % 2 == 0) {
            return ordenados.get(meio - 1).somar(ordenados.get(meio)).dividirPor(BigDecimal.valueOf(2));
        }
        return ordenados.get(meio);
    }
}
