package br.com.felipe.termometro.naogasto.application.service;

import br.com.felipe.termometro.naogasto.application.repository.NaoGastoRepository;
import br.com.felipe.termometro.naogasto.domain.LancamentoParaConciliar;
import br.com.felipe.termometro.naogasto.domain.MotorDeNaoGasto;
import br.com.felipe.termometro.naogasto.domain.ResultadoDaConciliacao;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RN-03. Defaults documentados aqui porque a spec não fecha os números — nenhum deles vem de um
 * cenário Gherkin, diferente da rampa (RN-15) ou do detector de recorrências (RN-07):
 *
 * <ul>
 *   <li>Tolerância de R$ 0,05 no casamento fatura×pagamento — mais folga que o R$ 0,01 da
 *       reconciliação de leitura (RN-02.1), porque aqui a soma passa por duas fontes
 *       independentes (extrato do cartão e extrato da corrente), não uma reconciliação contra o
 *       total impresso na própria fatura.</li>
 *   <li>Janela de 10 dias após o fechamento do mês para achar o débito de pagamento — a spec não
 *       define vencimento; 10 dias cobre a prática usual sem inventar uma data exata por banco.</li>
 *   <li>Janela de 3 meses de contexto (a competência informada + os 2 anteriores) — cobre o caso
 *       comum de pagamento de fatura (1 mês) e a maioria dos estornos (RN-03 pede até 90 dias).
 *       Um estorno de uma compra com mais de 3 meses não é pego numa única chamada — decisão de
 *       escopo, não bug: documentado, não escondido.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NaoGastoApplicationService implements NaoGastoService {

    static final Dinheiro TOLERANCIA_FATURA = Dinheiro.de("0.05");
    static final int JANELA_FATURA_DIAS = 10;
    static final int JANELA_TRANSFERENCIA_DIAS = 1;
    static final int JANELA_ESTORNO_DIAS = 90;
    static final int MESES_DE_CONTEXTO = 2;

    private final NaoGastoRepository naoGastoRepository;

    @Override
    public ResultadoDaConciliacao concilia(Competencia competencia) {
        log.info("[inicia] concilia [competencia={}]", competencia);

        Competencia inicio = competencia.menos(MESES_DE_CONTEXTO);
        List<LancamentoParaConciliar> lancamentos =
                naoGastoRepository.buscaLancamentos(inicio.primeiroDia(), competencia.ultimoDia());

        ResultadoDaConciliacao resultado = MotorDeNaoGasto.concilia(lancamentos, TOLERANCIA_FATURA,
                JANELA_FATURA_DIAS, JANELA_TRANSFERENCIA_DIAS, JANELA_ESTORNO_DIAS);

        if (!resultado.idsParaIgnorar().isEmpty()) {
            naoGastoRepository.marcaIgnoradas(resultado.idsParaIgnorar());
        }

        log.info("[finaliza] concilia [competencia={}, ignorados={}]", competencia,
                resultado.idsParaIgnorar().size());
        return resultado;
    }
}
