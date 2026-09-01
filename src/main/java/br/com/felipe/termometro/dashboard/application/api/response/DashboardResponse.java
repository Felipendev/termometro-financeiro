package br.com.felipe.termometro.dashboard.application.api.response;

import br.com.felipe.termometro.diagnostico.application.api.response.ViabilidadeResponse;

/**
 * RN-11 — Dashboard dos Três Eus, agregado. {@code viabilidade} vai sempre junto (não só quando o
 * veredito é ruim): é o front quem decide abrir com o bloco de queda estrutural de renda quando
 * {@code veredito != VIAVEL}, o backend só entrega o dado.
 */
public record DashboardResponse(
        String competencia,
        ViabilidadeResponse viabilidade,
        EuDoPassadoResponse euDoPassado,
        EuDoPresenteResponse euDoPresente,
        EuDoFuturoResponse euDoFuturo) {
}
