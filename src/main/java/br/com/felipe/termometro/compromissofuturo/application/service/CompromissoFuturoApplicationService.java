package br.com.felipe.termometro.compromissofuturo.application.service;

import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.compromissofuturo.domain.GeradorDeCompromissosFuturos;
import br.com.felipe.termometro.compromissofuturo.domain.LancamentoParceladoAncora;
import br.com.felipe.termometro.compromissofuturo.domain.ResultadoDaGeracao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-04: busca as parcelas conhecidas na ingestão (via a porta deste módulo, que
 * lê a mesma tabela {@code transacao} por baixo — a mesma costura de {@code naogasto}) e delega
 * o cálculo para {@link GeradorDeCompromissosFuturos}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompromissoFuturoApplicationService implements CompromissoFuturoService {

    private final CompromissoFuturoRepository repository;

    @Override
    public ResultadoDaGeracao gera() {
        log.info("[inicia] CompromissoFuturoApplicationService - gera");

        List<LancamentoParceladoAncora> lancamentos = repository.buscaTodosLancamentosParcelados();
        ResultadoDaGeracao resultado = GeradorDeCompromissosFuturos.gera(lancamentos);
        repository.reconcilia(resultado);

        log.info("[finaliza] CompromissoFuturoApplicationService - gera [gerados={}, series={}]",
                resultado.gerados().size(), resultado.seriesProcessadas().size());
        return resultado;
    }
}
