package br.com.felipe.termometro.notificacao.domain;

/**
 * Porta de saída: qualquer canal capaz de entregar uma mensagem de texto ao usuário. O scheduler
 * (application) depende só desta interface — trocar Telegram por e-mail, WhatsApp ou push é troca
 * de adapter em infra, sem tocar em quem decide o quê enviar.
 */
public interface CanalDeNotificacao {

    void envia(String mensagem);

    /** Se o canal não está configurado (sem token, sem credencial), a notificação é pulada, não falha. */
    boolean habilitado();
}
