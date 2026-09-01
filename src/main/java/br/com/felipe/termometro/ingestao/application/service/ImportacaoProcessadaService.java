package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaClassificacaoResponse;
import br.com.felipe.termometro.classificacao.application.service.ClassificacaoService;
import br.com.felipe.termometro.compromissofuturo.application.service.CompromissoFuturoService;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.naogasto.application.service.NaoGastoService;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.triagem.application.api.response.ResultadoDaTriagemResponse;
import br.com.felipe.termometro.triagem.application.service.TriagemService;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fecha o ciclo local de uma importação: um arquivo útil não pode terminar como dados brutos que
 * a home ainda não enxerga. Só é chamado com o subconjunto novo, portanto reimportar o mesmo
 * arquivo não executa classificação nem reescreve a análise.
 */
@Service
@RequiredArgsConstructor
public class ImportacaoProcessadaService {

    private final ClassificacaoService classificacaoService;
    private final NaoGastoService naoGastoService;
    private final CompromissoFuturoService compromissoFuturoService;
    private final TriagemService triagemService;

    @Transactional
    public ResultadoDoProcessamentoImportacao processa(List<TransacaoBruta> novasTransacoes) {
        NavigableSet<Competencia> competencias = new TreeSet<>();
        novasTransacoes.forEach(transacao -> competencias.add(Competencia.de(transacao.data())));
        if (competencias.isEmpty()) {
            return ResultadoDoProcessamentoImportacao.vazio();
        }

        int classificadas = 0;
        int pendentes = 0;
        for (Competencia competencia : competencias) {
            ResultadoDaClassificacaoResponse resultado = classificacaoService.classifica(competencia);
            classificadas += resultado.classificadas();
            pendentes += resultado.pendentesRevisao();
        }
        for (Competencia competencia : competencias) {
            naoGastoService.concilia(competencia);
        }
        compromissoFuturoService.gera();

        int triadas = 0;
        for (Competencia competencia : competencias) {
            ResultadoDaTriagemResponse resultado = triagemService.executaTriagem(competencia);
            triadas += resultado.triadas();
        }
        return new ResultadoDoProcessamentoImportacao(List.copyOf(competencias), classificadas,
                pendentes, triadas);
    }
}
