package br.com.felipe.termometro.dashboard.application.api;

import br.com.felipe.termometro.dashboard.application.api.response.DashboardResponse;
import br.com.felipe.termometro.dashboard.application.api.response.DashboardInicioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * RN-11 — Dashboard dos Três Eus, agregado. Só a visão de leitura: Eu do Passado (compromissos
 * futuros + dívidas), Eu do Presente (diagnóstico + resumo de triagem + vampiros) e Eu do Futuro
 * (marcos da projeção + painel de reserva + plano de ajuste), com o veredito de viabilidade
 * junto para o front decidir quando abrir com o bloco de queda estrutural de renda.
 *
 * <p>O simulador de compra (RN-11, {@code POST /cenarios/{id}/simular-compra}) fica fora desta
 * fatia — depende do conceito de {@code cenario} persistido, que ainda não existe no código
 * (mesma lacuna já documentada em {@code ProjecaoAPI}, {@code PlanoAjusteAPI} e {@code
 * TriagemAPI}). Migra para cá quando "cenário" existir.
 */
@RestController
@RequestMapping("/v1/dashboard")
public interface DashboardAPI {

    @GetMapping("/tres-eus")
    @ResponseStatus(HttpStatus.OK)
    DashboardResponse getDashboard(@RequestParam String competencia);

    @GetMapping("/inicio")
    @ResponseStatus(HttpStatus.OK)
    DashboardInicioResponse getInicio(@RequestParam String competencia);
}
