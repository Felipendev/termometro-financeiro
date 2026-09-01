package br.com.felipe.termometro.reserva.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.orcamento.application.repository.OrcamentoRepository;
import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.projecao.application.service.ProjecaoService;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.reserva.domain.CalculadoraDeNiveisDeReserva;
import br.com.felipe.termometro.reserva.domain.PainelDeReserva;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-21: soma o custo fixo do catálogo com a verba variável real declarada no
 * orçamento — sem dívida, ao contrário de {@code CustoMinimoVida} (RN-16, que usa piso humano em
 * vez de verba) e de {@code Marcos.reservaCompleta} (RN-09, que inclui dívida e recalcula o alvo
 * mês a mês) — e reaproveita {@link ProjecaoService} (estratégia AVALANCHE, sem que o painel
 * exponha escolha de estratégia) como única fonte de "quando a reserva cruza cada nível", já que
 * o sistema não guarda reserva já acumulada fora da simulação. Ver
 * {@link CalculadoraDeNiveisDeReserva} para a limitação documentada de {@code atingido}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReservaApplicationService implements ReservaService {

    /** Horizonte da simulação usada para achar os cruzamentos de nível — mesmo default da RN-09. */
    private static final int HORIZONTE_MESES = 60;

    private final CatalogoRepository catalogoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final ProjecaoService projecaoService;
    private final Clock relogio;
    private final TotaisMarcadosDoMes totaisMarcados;

    @Override
    public PainelDeReserva consultaPainel() {
        log.info("[inicia] ReservaApplicationService - consultaPainel");

        Competencia hoje = Competencia.atual(relogio);
        Dinheiro custoFixoTotal = totaisMarcados.marcadoOuLegado(hoje,
                MarcacaoPlanejamento.CUSTO_FIXO, Dinheiro.somaDe(
                        catalogoRepository.buscaCustoFixoAtivo().stream().map(CustoFixoItem::valor).toList()));
        VerbaMensal verbaMensal = orcamentoRepository.buscaVerbaPorCompetencia(hoje)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Nenhum orçamento de gastos variáveis definido para " + hoje
                                + ". Cadastre esse limite mensal para calcular a reserva."));
        Dinheiro custoMensal = custoFixoTotal.somar(verbaMensal.verbaVariavel());

        Projecao projecao = projecaoService.projeta(hoje, Estrategia.AVALANCHE, HORIZONTE_MESES);
        PainelDeReserva painel = CalculadoraDeNiveisDeReserva.calcular(custoMensal, projecao.meses());

        log.info("[finaliza] ReservaApplicationService - consultaPainel [proximoNivel={}]",
                painel.proximoNivel());
        return painel;
    }
}
