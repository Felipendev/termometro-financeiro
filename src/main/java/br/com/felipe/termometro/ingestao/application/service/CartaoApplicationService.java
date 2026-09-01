package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.ingestao.application.api.response.CartaoResponse;
import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.ingestao.application.repository.ContaRepository;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartaoApplicationService implements CartaoService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    @Override
    public ResumoCartoesResponse consultaCartoes(Competencia competencia) {
        log.info("[inicia] CartaoApplicationService - consultaCartoes [{}]", competencia);

        Map<String, Dinheiro> gastoPorConta = transacaoRepository.somaGastoDeCartaoPorConta(competencia);

        List<CartaoResponse> cartoes = contaRepository.buscaCartoes().stream()
                .map(conta -> CartaoResponse.de(conta,
                        gastoPorConta.getOrDefault(conta.identificador(), Dinheiro.ZERO)))
                .toList();

        log.info("[finaliza] CartaoApplicationService - consultaCartoes [{} cartões]", cartoes.size());
        return ResumoCartoesResponse.de(cartoes);
    }
}
