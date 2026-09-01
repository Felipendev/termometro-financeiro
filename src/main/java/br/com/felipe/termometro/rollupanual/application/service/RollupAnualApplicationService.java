package br.com.felipe.termometro.rollupanual.application.service;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository.LancamentoImportado;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.rollupanual.domain.CalculadoraDeTaxaDeEconomia;
import br.com.felipe.termometro.rollupanual.domain.MesDoRollup;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RN-10 reduzida — pura composição sobre as mesmas portas que a planilha viva já usa
 * ({@code LancamentoPlanejadoRepository}/{@code LancamentoImportadoRepository}). Diferente da
 * planilha, não precisa de saldo inicial nem de cascata: é só entrada/saída por mês, igual à
 * aba "Economia" da planilha original.
 *
 * <p><b>Duplicação conhecida e aceita:</b> os filtros de CANCELADO/TRANSFERENCIA e a leitura de
 * sinal repetem o que {@code PlanilhaApplicationService} já faz. Extrair um agregador comum é o
 * refactor natural se um terceiro consumidor aparecer — dois consumidores ainda não paga a
 * abstração.
 */
@Service
@RequiredArgsConstructor
public class RollupAnualApplicationService implements RollupAnualService {

    private final LancamentoPlanejadoRepository lancamentoPlanejadoRepository;
    private final LancamentoImportadoRepository lancamentoImportadoRepository;

    @Override
    public List<MesDoRollup> consulta(int ano) {
        List<MesDoRollup> meses = new ArrayList<>(12);
        for (int mes = 1; mes <= 12; mes++) {
            Competencia competencia = Competencia.de(ano, mes);
            Dinheiro entrada = somaPlanejadaEImportada(competencia, true);
            Dinheiro saida = somaPlanejadaEImportada(competencia, false);
            meses.add(new MesDoRollup(competencia, entrada, saida, CalculadoraDeTaxaDeEconomia.calcula(entrada, saida)));
        }
        return meses;
    }

    private Dinheiro somaPlanejadaEImportada(Competencia competencia, boolean entrada) {
        Dinheiro total = somaPlanejada(competencia, entrada);
        return total.somar(somaImportada(competencia, entrada));
    }

    private Dinheiro somaPlanejada(Competencia competencia, boolean entrada) {
        Dinheiro total = Dinheiro.ZERO;
        for (LancamentoPlanejado item : lancamentoPlanejadoRepository.buscaPorCompetencia(competencia)) {
            if (item.status() == StatusLancamentoPlanejado.CANCELADO || item.tipo() == TipoLancamentoPlanejado.TRANSFERENCIA) {
                continue;
            }
            boolean ehReceita = item.tipo() == TipoLancamentoPlanejado.RECEITA;
            if (ehReceita == entrada) {
                total = total.somar(item.valor());
            }
        }
        return total;
    }

    private Dinheiro somaImportada(Competencia competencia, boolean entrada) {
        Dinheiro total = Dinheiro.ZERO;
        for (LancamentoImportado item : lancamentoImportadoRepository.buscaPorCompetencia(competencia)) {
            boolean ehEntrada = item.valorComSinal().ehPositivo();
            boolean ehSaida = item.valorComSinal().ehNegativo();
            if (entrada && ehEntrada) {
                total = total.somar(item.valorComSinal());
            } else if (!entrada && ehSaida) {
                total = total.somar(item.valorComSinal().absoluto());
            }
        }
        return total;
    }
}
