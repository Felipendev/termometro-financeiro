package br.com.felipe.termometro.classificacao.application.service;

import br.com.felipe.termometro.classificacao.application.api.request.ClassificarTransacaoRequest;
import br.com.felipe.termometro.classificacao.application.api.response.ContextoDeRevisaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ContextoDeRevisaoResponse.SugestaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaClassificacaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaCorrecaoResponse;
import br.com.felipe.termometro.classificacao.domain.Categoria;
import br.com.felipe.termometro.classificacao.domain.ContextoDeRevisao;
import br.com.felipe.termometro.classificacao.domain.GrupoDeCategoria;
import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.classificacao.domain.PeriodoDoDia;
import br.com.felipe.termometro.handler.APIException;
import org.springframework.http.HttpStatus;
import br.com.felipe.termometro.classificacao.application.repository.ClassificacaoRepository;
import br.com.felipe.termometro.classificacao.application.repository.RegraDeCategorizacaoRepository;
import br.com.felipe.termometro.classificacao.domain.Categorizador;
import br.com.felipe.termometro.classificacao.domain.Classificacao;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Competencia;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassificacaoApplicationService implements ClassificacaoService {

    private final ClassificacaoRepository classificacaoRepository;
    private final RegraDeCategorizacaoRepository regraRepository;

    @Override
    @Transactional
    public ResultadoDaClassificacaoResponse classifica(Competencia competencia) {
        return executa(competencia, true);
    }

    @Override
    @Transactional
    public ResultadoDaClassificacaoResponse reclassifica(Competencia competencia) {
        return executa(competencia, false);
    }

    private ResultadoDaClassificacaoResponse executa(Competencia competencia, boolean apenasNovas) {
        log.info("[inicia] ClassificacaoApplicationService - {} [{}]",
                apenasNovas ? "classifica" : "reclassifica", competencia);

        // O catálogo do sistema é código; o que vem do banco são as regras do usuário e as
        // aprendidas — que, por terem prioridade negativa, são avaliadas primeiro.
        Categorizador categorizador = Categorizador.padrao()
                .com(regraRepository.buscaRegrasDoUsuario());

        Map<UUID, TransacaoBruta> transacoes =
                classificacaoRepository.buscaParaClassificar(competencia, apenasNovas);

        Map<UUID, Classificacao> resultados = new HashMap<>(transacoes.size());
        Map<String, Integer> porCategoria = new LinkedHashMap<>();
        List<String> avisos = new ArrayList<>();
        int naVerba = 0;
        int pendentes = 0;

        for (Map.Entry<UUID, TransacaoBruta> entrada : transacoes.entrySet()) {
            Classificacao classificacao = categorizador.classificar(entrada.getValue());
            resultados.put(entrada.getKey(), classificacao);
            porCategoria.merge(classificacao.categoria().nome(), 1, Integer::sum);
            if (classificacao.contaNoDiaADia()) {
                naVerba++;
            }
            if (classificacao.precisaRevisao()) {
                pendentes++;
            }
        }

        int aplicadas = classificacaoRepository.aplica(resultados);

        if (!transacoes.isEmpty() && pendentes > transacoes.size() / 2) {
            avisos.add(("%d de %d transações ficaram sem classificação confiável. "
                    + "Vale revisar a fila antes de confiar na verba diária deste mês.")
                    .formatted(pendentes, transacoes.size()));
        }
        log.info("[finaliza] ClassificacaoApplicationService [{} classificadas, {} na verba, {} pendentes]",
                aplicadas, naVerba, pendentes);
        return new ResultadoDaClassificacaoResponse(competencia.toString(), transacoes.size(),
                aplicadas, naVerba, pendentes, porCategoria, avisos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextoDeRevisaoResponse> filaDeRevisao(Competencia competencia, int limite) {
        log.info("[inicia] ClassificacaoApplicationService - filaDeRevisao [{}]", competencia);
        List<ContextoDeRevisaoResponse> fila = classificacaoRepository
                .buscaFilaDeRevisao(competencia, limite)
                .stream()
                .map(ClassificacaoApplicationService::paraResponse)
                .toList();
        log.info("[finaliza] ClassificacaoApplicationService - filaDeRevisao [{}]", fila.size());
        return fila;
    }

    /**
     * O usuário corrigindo uma classificação.
     *
     * <p>Duas coisas acontecem quando ele marca {@code aplicarAoGrupo}, e a ordem importa: primeiro
     * a regra é criada (para valer daqui em diante, inclusive em sincronizações futuras), depois o
     * grupo é reclassificado (para valer para o passado). Só reclassificar resolveria hoje e
     * deixaria a próxima fatura cair na fila de novo.
     */
    @Override
    @Transactional
    public ResultadoDaCorrecaoResponse corrige(UUID transacaoId, ClassificarTransacaoRequest request) {
        log.info("[inicia] ClassificacaoApplicationService - corrige [{}]", transacaoId);
        ContextoDeRevisao contexto = classificacaoRepository.buscaContexto(transacaoId)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Transação não encontrada: " + transacaoId));

        Categoria categoria = montaCategoria(request);
        boolean contaNaVerba = categoria.natureza().entraNaVerbaDiaria();
        boolean criouRegra = false;
        int afetadas;

        if (request.aplicarAoGrupo()) {
            regraRepository.aprende(contexto.grupoDeSimilaridade(), categoria);
            criouRegra = true;
            afetadas = classificacaoRepository.aplicaAoGrupo(contexto.grupoDeSimilaridade(),
                    categoria, contaNaVerba);
        } else {
            afetadas = classificacaoRepository.aplicaAoGrupo(contexto.grupoDeSimilaridade(),
                    categoria, contaNaVerba) > 0 ? 1 : 0;
        }

        String mensagem = criouRegra
                ? "%d transações de '%s' agora são %s. Novas compras nesse estabelecimento já entram classificadas."
                        .formatted(afetadas, contexto.grupoDeSimilaridade(), categoria.nome())
                : "Transação classificada como %s.".formatted(categoria.nome());

        log.info("[finaliza] ClassificacaoApplicationService - corrige [{} afetadas]", afetadas);
        return new ResultadoDaCorrecaoResponse(categoria.nome(), contaNaVerba, afetadas,
                criouRegra, mensagem);
    }

    private static Categoria montaCategoria(ClassificarTransacaoRequest request) {
        try {
            return new Categoria(request.categoria().strip().toUpperCase(java.util.Locale.ROOT),
                    GrupoDeCategoria.valueOf(request.grupo().strip().toUpperCase(java.util.Locale.ROOT)),
                    Natureza.valueOf(request.natureza().strip().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Grupo ou natureza inválidos. Grupos: " + java.util.Arrays.toString(GrupoDeCategoria.values())
                            + "; naturezas: " + java.util.Arrays.toString(Natureza.values()), e);
        }
    }

    private static ContextoDeRevisaoResponse paraResponse(ContextoDeRevisao contexto) {
        return new ContextoDeRevisaoResponse(
                contexto.id(),
                contexto.descricaoOriginal(),
                contexto.valor().paraJson(),
                contexto.data(),
                contexto.resumo().split(" ")[0],
                contexto.periodoOpcional().map(PeriodoDoDia::rotulo).orElse(null),
                contexto.horaConfiavel(),
                contexto.grupoDeSimilaridade(),
                contexto.similaresNoGrupo(),
                contexto.ticketMedioDoGrupo().paraJson(),
                contexto.resumo(),
                contexto.sugestoes().stream()
                        .map(s -> new SugestaoResponse(s.categoria().nome(),
                                s.categoria().grupo().name(), s.categoria().natureza().name(),
                                s.confianca(), s.motivo()))
                        .toList());
    }
}
