package br.com.felipe.termometro.reserva.application.service;

import br.com.felipe.termometro.reserva.domain.PainelDeReserva;

/** Porta de entrada do painel de reserva de emergência em níveis (RN-21). */
public interface ReservaService {

    PainelDeReserva consultaPainel();
}
