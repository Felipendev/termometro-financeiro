package br.com.felipe.termometro.vampiros.application.service;

import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.Normalizador;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.vampiros.domain.DetectorDeRecorrencias;
import br.com.felipe.termometro.vampiros.domain.Ocorrencia;
import br.com.felipe.termometro.vampiros.domain.Recorrencia;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-07: junta as transações dos últimos 6 meses, agrupa pela chave de
 * estabelecimento que a ingestão já sabe calcular ({@link Normalizador}, RN-02/RN-12 — não há
 * {@code estabelecimento_cnpj} nem {@code merchant.name} neste código ainda, então a descrição
 * normalizada é o único agrupador disponível) e delega a decisão para
 * {@link DetectorDeRecorrencias}.
 *
 * <p>Não persiste nada — cada chamada recalcula do zero a partir das transações já ingeridas.
 * O rastreamento de decisão do usuário por recorrência (`PATCH /vampiros/{id}`, o sinalizador
 * "sem decisão há mais de 6 meses") fica para quando existir uma tabela {@code recorrencia}
 * para guardar esse estado — hoje o único sinalizador de "cobrança silenciosa" que este código
 * calcula é {@code valorMedio < R$ 50}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VampirosApplicationService implements VampirosService {

    private static final int MESES_DE_JANELA = 6;

    private final TransacaoRepository transacaoRepository;

    @Override
    public List<Recorrencia> listaVampiros(Competencia ate) {
        log.info("[inicia] VampirosApplicationService - listaVampiros [ate={}]", ate);

        Competencia inicio = ate.menos(MESES_DE_JANELA - 1);
        List<TransacaoBruta> despesasDoPeriodo = inicio.ate(ate)
                .flatMap(mes -> transacaoRepository.buscaPorCompetencia(mes).stream())
                .filter(TransacaoBruta::ehDespesa)
                .toList();

        Map<String, List<Ocorrencia>> agrupadasPorEstabelecimento = despesasDoPeriodo.stream()
                .collect(Collectors.groupingBy(
                        t -> Normalizador.chaveDeEstabelecimento(t.descricao(), t.cidade()),
                        Collectors.mapping(t -> new Ocorrencia(t.data(), t.valor().absoluto()), Collectors.toList())));

        List<Recorrencia> recorrencias = agrupadasPorEstabelecimento.entrySet().stream()
                .map(grupo -> DetectorDeRecorrencias.detectar(grupo.getKey(), grupo.getValue()))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(Recorrencia::custoAnual).reversed())
                .toList();

        log.info("[finaliza] VampirosApplicationService - listaVampiros [detectadas={}]", recorrencias.size());
        return recorrencias;
    }
}
