package br.com.felipe.termometro.notificacao.application.service;

import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.notificacao.application.repository.EstadoDeAlertaRepository;
import br.com.felipe.termometro.notificacao.domain.CanalDeNotificacao;
import br.com.felipe.termometro.notificacao.domain.DecisorDeAlerta;
import br.com.felipe.termometro.orcamento.application.service.OrcamentoService;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.FaixaSaude;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import br.com.felipe.termometro.projecao.application.service.ProjecaoService;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.Marcos;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-22: {@link DecisorDeAlerta} decide o quê e quando; aqui é só orquestração —
 * busca o que os outros módulos já sabem, consulta/atualiza o estado de dedup, manda pro canal.
 *
 * <p>Nenhum método aqui engole exceção: quem chama (o scheduler, ou
 * {@code SincronizacaoApplicationService}) decide como conter falha, mesma responsabilidade que
 * {@code NotificacaoMatinalScheduler} já tem sobre a notificação matinal.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertaApplicationService implements AlertaService {

    /** "Transação alta" da RN-22 — sem base numérica na spec, valor citado no roadmap do produto. */
    private static final Dinheiro LIMITE_TRANSACAO_ALTA = Dinheiro.de("100");
    /** Mesmo horizonte usado pela composição de `reserva` (RN-21) para achar marcos futuros. */
    private static final int HORIZONTE_MESES = 60;

    private final CanalDeNotificacao canal;
    private final EstadoDeAlertaRepository estadoRepository;
    private final OrcamentoService orcamentoService;
    private final ProjecaoService projecaoService;
    private final Clock relogio;

    @Override
    public void avaliaTransacoesAltas(List<TransacaoBruta> transacoesNovas) {
        log.info("[inicia] AlertaApplicationService - avaliaTransacoesAltas");
        if (!canal.habilitado()) {
            return;
        }
        List<TransacaoBruta> altas =
                DecisorDeAlerta.transacoesAcimaDoLimite(transacoesNovas, LIMITE_TRANSACAO_ALTA);
        if (!altas.isEmpty()) {
            canal.envia(DecisorDeAlerta.mensagemTransacoesAltas(altas, LIMITE_TRANSACAO_ALTA));
        }
        log.info("[finaliza] AlertaApplicationService - avaliaTransacoesAltas [{} altas]", altas.size());
    }

    @Override
    public void avaliaVerbaBaixa() {
        log.info("[inicia] AlertaApplicationService - avaliaVerbaBaixa");
        if (!canal.habilitado()) {
            return;
        }
        VerbaDoDia verba = orcamentoService.consultaVerbaDeHoje();
        String chave = "verba-baixa:" + verba.data();
        FaixaSaude ultimaAvisadaHoje = estadoRepository.busca(chave).map(FaixaSaude::valueOf).orElse(null);
        if (DecisorDeAlerta.verbaPiorou(verba.faixa(), ultimaAvisadaHoje)) {
            canal.envia(DecisorDeAlerta.mensagemVerbaBaixa(verba));
            estadoRepository.salva(chave, verba.faixa().name());
        }
        log.info("[finaliza] AlertaApplicationService - avaliaVerbaBaixa [faixa={}]", verba.faixa());
    }

    @Override
    public void avaliaMarcos() {
        log.info("[inicia] AlertaApplicationService - avaliaMarcos");
        if (!canal.habilitado()) {
            return;
        }
        Competencia hoje = Competencia.atual(relogio);
        Marcos marcos = projecaoService.projeta(hoje, Estrategia.AVALANCHE, HORIZONTE_MESES).marcos();
        avaliaMarco("quitacao", "a dívida foi quitada", marcos.dataQuitacao(), hoje);
        avaliaMarco("primeiro-real-guardado", "primeiro real guardado neste mês",
                marcos.primeiroRealGuardado(), hoje);
        avaliaMarco("reserva-completa", "a reserva-alvo da projeção foi atingida",
                marcos.reservaCompleta(), hoje);
        log.info("[finaliza] AlertaApplicationService - avaliaMarcos");
    }

    private void avaliaMarco(String tipo, String rotulo, @Nullable Competencia marco, Competencia hoje) {
        if (!DecisorDeAlerta.marcoAtingidoAgora(marco, hoje)) {
            return;
        }
        String chave = "marco:" + tipo + ":" + hoje;
        if (estadoRepository.busca(chave).isPresent()) {
            return;
        }
        canal.envia(DecisorDeAlerta.mensagemMarco(rotulo, hoje));
        estadoRepository.salva(chave, "1");
    }

    @Override
    public void avaliaEventosProximos() {
        log.info("[inicia] AlertaApplicationService - avaliaEventosProximos");
        if (!canal.habilitado()) {
            return;
        }
        VerbaDoDia verba = orcamentoService.consultaVerbaDeHoje();
        for (Evento evento : verba.eventosProximos()) {
            String chave = "evento:" + evento.data() + ":" + evento.descricao();
            if (estadoRepository.busca(chave).isPresent()) {
                continue;
            }
            canal.envia(DecisorDeAlerta.mensagemEvento(evento));
            estadoRepository.salva(chave, "1");
        }
        log.info("[finaliza] AlertaApplicationService - avaliaEventosProximos");
    }
}
