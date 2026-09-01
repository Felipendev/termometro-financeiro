package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.ingestao.application.api.response.CartaoResponse;
import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.ingestao.application.repository.ContaRepository;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Map;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CartaoApplicationService implements CartaoService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final CartaoRepository cartaoRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public CartaoApplicationService(ContaRepository contaRepository,
            TransacaoRepository transacaoRepository, CartaoRepository cartaoRepository) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.cartaoRepository = cartaoRepository;
    }

    /** Mantém os testes unitários antigos focados apenas nas contas bancárias importadas. */
    CartaoApplicationService(ContaRepository contaRepository, TransacaoRepository transacaoRepository) {
        this(contaRepository, transacaoRepository, new CartaoRepository() {
            @Override public java.util.List<br.com.felipe.termometro.cartao.domain.Cartao> buscaAtivos() { return java.util.List.of(); }
            @Override public br.com.felipe.termometro.cartao.domain.Cartao salva(br.com.felipe.termometro.cartao.domain.Cartao cartao) { return cartao; }
            @Override public void remove(java.util.UUID id) { }
        });
    }

    @Override
    public ResumoCartoesResponse consultaCartoes(Competencia competencia) {
        log.info("[inicia] CartaoApplicationService - consultaCartoes [{}]", competencia);

        Map<String, Dinheiro> gastoPorConta = transacaoRepository.somaGastoDeCartaoPorConta(competencia);

        var manuaisComImportacao = cartaoRepository.buscaAtivos().stream()
                .filter(cartao -> gastoPorConta.getOrDefault(cartao.id().toString(), Dinheiro.ZERO).ehPositivo())
                .toList();
        Set<String> nomesManuaisComImportacao = manuaisComImportacao.stream()
                .map(cartao -> normaliza(cartao.nome())).collect(java.util.stream.Collectors.toSet());
        List<CartaoResponse> cartoesAutomaticos = contaRepository.buscaCartoes().stream()
                .filter(conta -> !nomesManuaisComImportacao.contains(normaliza(conta.nome())))
                .map(conta -> CartaoResponse.de(conta,
                        gastoPorConta.getOrDefault(conta.identificador(), Dinheiro.ZERO)))
                .toList();
        List<CartaoResponse> cartoes = java.util.stream.Stream.concat(
                cartoesAutomaticos.stream(),
                manuaisComImportacao.stream().map(cartao -> CartaoResponse.de(cartao,
                        gastoPorConta.getOrDefault(cartao.id().toString(), Dinheiro.ZERO))))
                .toList();

        log.info("[finaliza] CartaoApplicationService - consultaCartoes [{} cartões]", cartoes.size());
        return ResumoCartoesResponse.de(cartoes);
    }

    private static String normaliza(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
