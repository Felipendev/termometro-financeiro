package br.com.felipe.termometro.naogasto.application.service;

import br.com.felipe.termometro.naogasto.domain.ResultadoDaConciliacao;
import br.com.felipe.termometro.shared.Competencia;

/** RN-03 — porta de entrada do motor de "não é gasto". */
public interface NaoGastoService {

    /**
     * Roda os três casadores (pagamento de fatura, transferência própria, estorno) olhando o
     * histórico até {@code competencia} e persiste {@code ignorada = true} nos lançamentos
     * casados.
     */
    ResultadoDaConciliacao concilia(Competencia competencia);
}
