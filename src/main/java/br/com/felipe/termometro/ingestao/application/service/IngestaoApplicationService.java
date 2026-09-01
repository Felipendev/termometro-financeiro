package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.ingestao.application.leitor.LeitorDeFatura;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.Deduplicador;
import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestaoApplicationService implements IngestaoService {

    private final Map<String, LeitorDeFatura> leitoresPorFormato;
    private final TransacaoRepository transacaoRepository;

    public IngestaoApplicationService(List<LeitorDeFatura> leitores,
                                      TransacaoRepository transacaoRepository) {
        this.leitoresPorFormato = leitores.stream()
                .collect(Collectors.toMap(LeitorDeFatura::formato, Function.identity()));
        this.transacaoRepository = transacaoRepository;
    }

    @Override
    public ResultadoDaLeitura importaArquivo(String identificadorDaConta, String formato,
                                             InputStream conteudo) {
        log.info("[inicia] IngestaoApplicationService - importaArquivo [{}]", formato);
        LeitorDeFatura leitor = leitorDe(formato);

        ResultadoDaLeitura resultado;
        try {
            resultado = leitor.ler(conteudo);
        } catch (IOException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Não consegui ler o arquivo enviado.", e);
        }

        // RN-02.1: importação que não reconcilia entra marcada, mas entra — o aviso vai no resultado,
        // e é a fila de revisão que decide o que fazer com ela. Recusar o arquivo inteiro por causa
        // de R$ 0,04 de diferença seria pior que registrar a divergência.
        if (!resultado.confiavel()) {
            log.warn("[reconciliação] importação não fecha: {}",
                    resultado.conferencia().map(r -> r.relatorio()).orElse("sem total declarado"));
        }

        int novas = salva(identificadorDaConta, resultado).size();
        log.info("[finaliza] IngestaoApplicationService - importaArquivo [{} novas]", novas);
        return resultado;
    }

    @Override
    public ImportacaoConcluida importaArquivoComResultado(String identificadorDaConta, String formato,
                                                           InputStream conteudo) {
        LeitorDeFatura leitor = leitorDe(formato);
        try {
            ResultadoDaLeitura resultado = leitor.ler(conteudo);
            return new ImportacaoConcluida(resultado, salva(identificadorDaConta, resultado));
        } catch (IOException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Não consegui ler o arquivo enviado.", e);
        }
    }

    private List<br.com.felipe.termometro.ingestao.domain.TransacaoBruta> salva(String identificadorDaConta,
                                                                                  ResultadoDaLeitura resultado) {
        return transacaoRepository.salvaTodas(identificadorDaConta,
                new Deduplicador(identificadorDaConta).absorver(resultado.transacoes()).transacoes());
    }

    private LeitorDeFatura leitorDe(String formato) {
        LeitorDeFatura leitor = leitoresPorFormato.get(formato);
        if (leitor == null) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Formato não suportado: '" + formato + "'. Disponíveis: " + leitoresPorFormato.keySet());
        }
        return leitor;
    }

    @Override
    public PropostaDeImportacao propoeImportacao(byte[] conteudo) {
        for (Map.Entry<String, LeitorDeFatura> candidato : leitoresPorFormato.entrySet()) {
            ResultadoDaLeitura resultado = tentaLer(candidato.getValue(), conteudo);
            if (resultado != null && !resultado.transacoes().isEmpty()) {
                return new PropostaDeImportacao(candidato.getKey(), resultado, List.copyOf(leitoresPorFormato.keySet()));
            }
        }
        return new PropostaDeImportacao(null, null, List.copyOf(leitoresPorFormato.keySet()));
    }

    /**
     * A detecção é tentativa e erro: um leitor de CSV recebendo um PDF (ou vice-versa) lança algo
     * não documentado por cada implementação — capturar {@code Exception} aqui é proposital, o
     * papel desta borda é só "reconheceu ou não", nunca propagar erro de parsing de um formato que
     * nem era esse.
     */
    private ResultadoDaLeitura tentaLer(LeitorDeFatura leitor, byte[] conteudo) {
        try {
            return leitor.ler(new ByteArrayInputStream(conteudo));
        } catch (Exception falhaDeReconhecimento) {
            return null;
        }
    }
}
