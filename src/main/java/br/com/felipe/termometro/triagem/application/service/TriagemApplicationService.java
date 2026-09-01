package br.com.felipe.termometro.triagem.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.triagem.application.api.response.ResultadoDaTriagemResponse;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import br.com.felipe.termometro.triagem.application.repository.TriagemRepository;
import br.com.felipe.termometro.triagem.domain.Etiqueta;
import br.com.felipe.termometro.triagem.domain.MotorDeTriagem;
import br.com.felipe.termometro.triagem.domain.ResumoDeCategoria;
import br.com.felipe.termometro.triagem.domain.TransacaoClassificada;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TriagemApplicationService implements TriagemService {

    private final TriagemRepository triagemRepository;
    private final CatalogoRepository catalogoRepository;

    @Override
    @Transactional
    public ResultadoDaTriagemResponse executaTriagem(Competencia competencia) {
        log.info("[inicia] TriagemApplicationService - executaTriagem [{}]", competencia);
        List<TransacaoClassificada> transacoes = triagemRepository.buscaClassificadasDoMes(competencia);
        Map<String, Dinheiro> pisoPorCategoria = pisoPorCategoria();

        Map<UUID, Etiqueta> etiquetas = MotorDeTriagem.triar(transacoes, pisoPorCategoria);
        int aplicadas = triagemRepository.aplicaEtiquetas(etiquetas);

        Map<String, Integer> porEtiqueta = new LinkedHashMap<>();
        etiquetas.values().forEach(e -> porEtiqueta.merge(e.name(), 1, Integer::sum));

        log.info("[finaliza] TriagemApplicationService - executaTriagem [{} triadas]", aplicadas);
        return new ResultadoDaTriagemResponse(competencia.toString(), transacoes.size(), aplicadas, porEtiqueta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumoDeCategoriaResponse> resumo(Competencia competencia) {
        log.info("[inicia] TriagemApplicationService - resumo [{}]", competencia);
        List<TransacaoClassificada> transacoes = triagemRepository.buscaClassificadasDoMes(competencia);
        Map<String, Dinheiro> pisoPorCategoria = pisoPorCategoria();

        List<ResumoDeCategoria> resumo = MotorDeTriagem.resumir(transacoes, pisoPorCategoria);
        log.info("[finaliza] TriagemApplicationService - resumo [{} categorias]", resumo.size());
        return resumo.stream().map(ResumoDeCategoriaResponse::new).toList();
    }

    @Override
    @Transactional
    public void promoveParaVermelha(UUID transacaoId) {
        log.info("[inicia] TriagemApplicationService - promoveParaVermelha [{}]", transacaoId);
        Etiqueta atual = triagemRepository.buscaEtiquetaAtual(transacaoId)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Transação não encontrada ou ainda não triada: " + transacaoId));
        if (!atual.podeSerPromovidaParaVermelha()) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Só é possível promover para VERMELHA uma transação hoje AMARELA. Etiqueta atual: " + atual);
        }
        triagemRepository.promoveParaVermelha(transacaoId);
        log.info("[finaliza] TriagemApplicationService - promoveParaVermelha [{}]", transacaoId);
    }

    private Map<String, Dinheiro> pisoPorCategoria() {
        return catalogoRepository.buscaPisoHumano().stream()
                .collect(Collectors.toMap(PisoHumano::categoria, PisoHumano::valorPiso));
    }
}
