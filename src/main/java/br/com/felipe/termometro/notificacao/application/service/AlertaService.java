package br.com.felipe.termometro.notificacao.application.service;

import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import java.util.List;

/** Porta de entrada dos 4 gatilhos de RN-22 além da notificação matinal (RN-19). */
public interface AlertaService {

    /** Avalia as transações recém-sincronizadas (não as que já existiam) contra o limite de alerta. */
    void avaliaTransacoesAltas(List<TransacaoBruta> transacoesNovas);

    /** Avalia a verba de hoje; dispara se a faixa está RUIM/PÉSSIMO e piorou desde o último aviso do dia. */
    void avaliaVerbaBaixa();

    /** Avalia se algum marco da projeção (quitação, primeiro real guardado, reserva completa) é hoje. */
    void avaliaMarcos();

    /** Avalia os eventos que entraram na janela de 3 dias de antecedência (RN-20). */
    void avaliaEventosProximos();
}
