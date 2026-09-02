package br.com.felipe.termometro.lancamentoplanejado.recorrencia;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recorrência de lançamento planejado: uma série (mesmo {@code serieId}) é um grupo de ocorrências
 * mensais, uma por competência, com o mesmo dia fixo do mês ({@code diaRecorrencia}, com clamp pro
 * último dia em meses mais curtos). Não existe tabela de "template" separada — a ocorrência mais
 * recente da própria série é sempre a referência de conteúdo pra gerar as próximas.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecorrenciaLancamentoService {

    /** Quantos meses à frente uma série deve sempre ter materializados. */
    public static final int HORIZONTE_MESES = 12;

    private final LancamentoPlanejadoRepository repository;
    private final Clock relogio;

    /** Cria a série a partir da primeira ocorrência (já persistida com o vencimento escolhido) e
     *  materializa as próximas até completar o horizonte. */
    @Transactional
    public LancamentoPlanejado criaSerie(LancamentoPlanejado origem, int diaRecorrencia) {
        UUID serieId = UUID.randomUUID();
        LancamentoPlanejado primeira = repository.salva(origem.comRecorrencia(serieId, diaRecorrencia));
        materializaAteHorizonte(serieId);
        return primeira;
    }

    /**
     * Adota lançamentos que foram marcados como recorrentes mas ficaram sem série — nenhum mês
     * futuro foi gerado pra eles. Rede de segurança: qualquer caminho de escrita que grave o dia
     * sem criar a série é consertado sozinho aqui, em vez de o usuário descobrir meses depois que
     * "o recorrente não apareceu".
     *
     * @return quantas séries foram criadas
     */
    @Transactional
    public int adotaOrfaos() {
        int adotados = 0;
        for (LancamentoPlanejado orfao : repository.buscaOrfaosDeRecorrencia()) {
            criaSerie(orfao, orfao.diaRecorrencia());
            adotados++;
        }
        if (adotados > 0) {
            log.info("[RecorrenciaLancamentoService] adotaOrfaos [{} séries criadas]", adotados);
        }
        return adotados;
    }

    /** Garante que a série tenha ocorrências PENDENTE até hoje + {@link #HORIZONTE_MESES}. Idempotente
     *  — nunca sobrescreve uma ocorrência já existente, mesmo que ela tenha sido customizada. */
    @Transactional
    public void materializaAteHorizonte(UUID serieId) {
        List<LancamentoPlanejado> ocorrencias = repository.buscaPorSerie(serieId);
        LancamentoPlanejado modelo = ocorrencias.stream()
                .filter(item -> item.status() != StatusLancamentoPlanejado.CANCELADO)
                .max(Comparator.comparing(LancamentoPlanejado::vencimento))
                .orElse(null);
        if (modelo == null || modelo.diaRecorrencia() == null) {
            return;
        }
        Set<YearMonth> existentes = ocorrencias.stream()
                .map(item -> YearMonth.from(item.vencimento()))
                .collect(Collectors.toSet());
        // HORIZONTE_MESES é o total de meses de cobertura desejado a partir de hoje (contando o
        // mês atual) — daí o -1: hoje+0 até hoje+11 já são 12 meses.
        Competencia horizonte = Competencia.atual(relogio).mais(HORIZONTE_MESES - 1);
        Competencia cursor = Competencia.de(YearMonth.from(modelo.vencimento())).proxima();
        int criadas = 0;
        while (!cursor.valor().isAfter(horizonte.valor())) {
            if (!existentes.contains(cursor.valor())) {
                LocalDate vencimento = diaClamped(cursor.valor(), modelo.diaRecorrencia());
                repository.salva(modelo.comoNovaOcorrencia(UUID.randomUUID(), vencimento));
                criadas++;
            }
            cursor = cursor.proxima();
        }
        if (criadas > 0) {
            log.info("[RecorrenciaLancamentoService] materializaAteHorizonte [serie={}, criadas={}]",
                    serieId, criadas);
        }
    }

    /** Aplica o conteúdo editado a esta e a toda ocorrência PENDENTE futura da mesma série
     *  (vencimento &gt;= o desta), recalculando o vencimento delas se o dia mudou, e repõe o
     *  horizonte em seguida. */
    @Transactional
    public void aplicaEdicaoATodasAsFuturas(LancamentoPlanejado editado) {
        if (editado.serieId() == null) {
            throw new IllegalArgumentException("lançamento não pertence a uma série de recorrência");
        }
        for (LancamentoPlanejado ocorrencia : repository.buscaPorSerie(editado.serieId())) {
            boolean futuraOuAtual = ocorrencia.status() == StatusLancamentoPlanejado.PENDENTE
                    && !ocorrencia.vencimento().isBefore(editado.vencimento());
            if (!futuraOuAtual) continue;
            LocalDate vencimento = editado.diaRecorrencia() == null ? ocorrencia.vencimento()
                    : diaClamped(YearMonth.from(ocorrencia.vencimento()), editado.diaRecorrencia());
            repository.salva(ocorrencia.comConteudoDe(editado).comVencimento(vencimento));
        }
        materializaAteHorizonte(editado.serieId());
    }

    private static LocalDate diaClamped(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }
}
