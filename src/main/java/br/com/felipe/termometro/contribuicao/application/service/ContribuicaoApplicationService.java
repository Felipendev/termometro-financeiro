package br.com.felipe.termometro.contribuicao.application.service;

import br.com.felipe.termometro.contribuicao.application.repository.ContribuicaoRepository;
import br.com.felipe.termometro.contribuicao.domain.CalculadoraDeAutorizacaoDeContribuicao;
import br.com.felipe.termometro.contribuicao.domain.MetaContribuicao;
import br.com.felipe.termometro.contribuicao.domain.NomeDaContribuicao;
import br.com.felipe.termometro.contribuicao.domain.ProximoPassoContribuicao;
import br.com.felipe.termometro.diagnostico.application.service.DiagnosticoService;
import br.com.felipe.termometro.diagnostico.domain.SaldoDeSobrevivencia;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * RN-28 — pura composição sobre {@code diagnostico} (RN-08), mesmo espírito de {@code reserva} e
 * {@code planoajuste}: nenhum cálculo de saldo próprio, só a decisão de autorizar ou não o próximo
 * passo da rampa.
 *
 * <p><b>Fora do escopo desta fatia:</b> uma vez autorizada, a contribuição não volta a entrar como
 * saída comprometida em {@code SaldoDeSobrevivencia} nem no motor de projeção — isso exigiria
 * estender {@code CalculadoraDeSaldoDeSobrevivencia}, módulo que não foi tocado aqui.
 */
@Service
@RequiredArgsConstructor
public class ContribuicaoApplicationService implements ContribuicaoService {

    private final ContribuicaoRepository contribuicaoRepository;
    private final DiagnosticoService diagnosticoService;

    @Override
    public List<MetaComProximoPasso> consulta(Competencia competencia) {
        List<MetaContribuicao> metas = contribuicaoRepository.buscaTodas();
        Competencia proxima = competencia.proxima();
        SaldoDeSobrevivencia saldoProximo;
        try {
            saldoProximo = diagnosticoService.consultaSaldoDeSobrevivencia(proxima);
        } catch (APIException excecao) {
            if (excecao.getStatusException() != HttpStatus.NOT_FOUND) {
                throw excecao;
            }
            String informacaoNecessaria = "Para calcular os valores e o próximo passo de "
                    + proxima + ", falta: " + excecao.getMessage();
            return metas.stream()
                    .map(meta -> new MetaComProximoPasso(meta, null, null, informacaoNecessaria))
                    .toList();
        }
        Dinheiro contribuicaoAtualTotal = somaContribuicaoAtual(metas, saldoProximo.rendaLiquida());

        return metas.stream()
                .map(meta -> new MetaComProximoPasso(
                        meta,
                        meta.percentualAtual().aplicarSobre(saldoProximo.rendaLiquida()),
                        avalia(meta, proxima, saldoProximo, contribuicaoAtualTotal),
                        null))
                .toList();
    }

    @Override
    public MetaContribuicao autoriza(NomeDaContribuicao nome, Competencia competencia) {
        MetaContribuicao atual = contribuicaoRepository.busca(nome)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND, "Meta não cadastrada: " + nome));

        Competencia proxima = competencia.proxima();
        SaldoDeSobrevivencia saldoProximo;
        try {
            saldoProximo = diagnosticoService.consultaSaldoDeSobrevivencia(proxima);
        } catch (APIException excecao) {
            if (excecao.getStatusException() != HttpStatus.NOT_FOUND) {
                throw excecao;
            }
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível autorizar o próximo passo: " + excecao.getMessage());
        }
        Dinheiro contribuicaoAtualTotal = somaContribuicaoAtual(contribuicaoRepository.buscaTodas(), saldoProximo.rendaLiquida());

        ProximoPassoContribuicao proposta = avalia(atual, proxima, saldoProximo, contribuicaoAtualTotal);
        if (proposta == null) {
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ainda não há espaço no fluxo de caixa projetado para autorizar o próximo passo.");
        }

        return contribuicaoRepository.salva(atual.comPercentualAtual(proposta.percentualProposto()));
    }

    private ProximoPassoContribuicao avalia(MetaContribuicao meta, Competencia proxima,
            SaldoDeSobrevivencia saldoProximo, Dinheiro contribuicaoAtualTotal) {
        return CalculadoraDeAutorizacaoDeContribuicao.avalia(
                meta, proxima, saldoProximo.saldo(), saldoProximo.rendaLiquida(),
                contribuicaoAtualTotal, saldoProximo.minimoVariavel()).orElse(null);
    }

    private Dinheiro somaContribuicaoAtual(List<MetaContribuicao> metas, Dinheiro rendaLiquida) {
        return Dinheiro.somaDe(metas.stream()
                .map(meta -> meta.percentualAtual().aplicarSobre(rendaLiquida))
                .toList());
    }
}
