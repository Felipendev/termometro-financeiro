package br.com.felipe.termometro.lancamentoplanejado.application.service;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository.LancamentoImportado;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsultaLancamentosService {
    private final LancamentoPlanejadoRepository repository;
    private final LancamentoImportadoRepository importados;
    private final Clock relogio;

    @Autowired
    public ConsultaLancamentosService(LancamentoPlanejadoRepository repository,
                                      LancamentoImportadoRepository importados) {
        this(repository, importados, Clock.systemDefaultZone());
    }

    ConsultaLancamentosService(LancamentoPlanejadoRepository repository) {
        this(repository, competencia -> List.of(), Clock.systemDefaultZone());
    }

    ConsultaLancamentosService(LancamentoPlanejadoRepository repository, Clock relogio) {
        this(repository, competencia -> List.of(), relogio);
    }

    ConsultaLancamentosService(LancamentoPlanejadoRepository repository,
                               LancamentoImportadoRepository importados, Clock relogio) {
        this.repository = repository;
        this.importados = importados;
        this.relogio = relogio;
    }

    public Resultado consulta(Filtro filtro) {
        LocalDate hoje = LocalDate.now(relogio);
        List<LancamentoPlanejado> manuais = repository.buscaPorCompetencia(filtro.competencia());
        List<ItemConsulta> candidatosManuais = manuais.stream()
                .map(item -> new ItemConsulta(item, new MetadadosConsulta(null, true, "MANUAL")))
                .toList();
        List<ItemConsulta> candidatosImportados = importados.buscaPorCompetencia(filtro.competencia()).stream()
                        .filter(item -> !item.valorComSinal().ehZero())
                        .map(this::converteImportado)
                        .toList();
        List<ItemConsulta> filtradosComMetadados = java.util.stream.Stream
                .concat(candidatosManuais.stream(), candidatosImportados.stream())
                .filter(candidato -> filtro.competencia().contem(candidato.lancamento().vencimento()))
                .filter(candidato -> filtro.tipo() == null
                        || candidato.lancamento().tipo().name().equals(filtro.tipo()))
                .filter(candidato -> correspondeAoStatus(candidato.lancamento(), filtro.status(), hoje))
                .filter(candidato -> filtro.contaId() == null
                        || filtro.contaId().equals(candidato.lancamento().contaOrigemId())
                        || filtro.contaId().equals(candidato.lancamento().contaDestinoId()))
                .filter(candidato -> filtro.cartaoId() == null
                        || filtro.cartaoId().equals(candidato.lancamento().cartaoManualId()))
                .filter(candidato -> filtro.categoria() == null
                        || candidato.lancamento().categoria() != null
                        && candidato.lancamento().categoria().nome().equalsIgnoreCase(filtro.categoria()))
                .filter(candidato -> filtro.texto() == null
                        || candidato.lancamento().descricao().toLowerCase(Locale.ROOT)
                        .contains(filtro.texto().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing((ItemConsulta item) -> item.lancamento().vencimento()).reversed()
                        .thenComparing(item -> item.lancamento().id()))
                .toList();
        List<LancamentoPlanejado> filtrados = filtradosComMetadados.stream()
                .map(ItemConsulta::lancamento)
                .toList();
        Map<UUID, MetadadosConsulta> metadados = filtradosComMetadados.stream()
                .collect(Collectors.toUnmodifiableMap(
                        item -> item.lancamento().id(), ItemConsulta::metadados, (primeiro, segundo) -> primeiro));
        int inicio = Math.min(filtro.pagina() * filtro.tamanho(), filtrados.size());
        int fim = Math.min(inicio + filtro.tamanho(), filtrados.size());
        List<LancamentoPlanejado> ativos = filtrados.stream()
                .filter(item -> item.status() != StatusLancamentoPlanejado.CANCELADO)
                .toList();
        Dinheiro despesas = soma(ativos, TipoLancamentoPlanejado.DESPESA, null);
        Dinheiro receitas = soma(ativos, TipoLancamentoPlanejado.RECEITA, null);
        Dinheiro despesasRealizadas = soma(ativos, TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.LIQUIDADO);
        Dinheiro receitasRealizadas = soma(ativos, TipoLancamentoPlanejado.RECEITA,
                StatusLancamentoPlanejado.LIQUIDADO);
        int atrasados = (int) filtrados.stream()
                .filter(item -> item.status() == StatusLancamentoPlanejado.PENDENTE)
                .filter(item -> item.vencimento().isBefore(hoje))
                .count();
        return new Resultado(filtrados.subList(inicio, fim), filtrados.size(), despesas, receitas,
                receitasRealizadas.subtrair(despesasRealizadas), receitas.subtrair(despesas), atrasados,
                filtro.pagina(), filtro.tamanho(), fim < filtrados.size(), metadados);
    }

    private ItemConsulta converteImportado(LancamentoImportado importado) {
        TipoLancamentoPlanejado tipo = ehTransferencia(importado)
                ? TipoLancamentoPlanejado.TRANSFERENCIA
                : importado.valorComSinal().ehNegativo()
                    ? TipoLancamentoPlanejado.DESPESA : TipoLancamentoPlanejado.RECEITA;
        CategoriaDoLancamento categoria = tipo != TipoLancamentoPlanejado.DESPESA
                || importado.categoria() == null || importado.categoria().isBlank()
                ? null
                : new CategoriaDoLancamento(importado.categoria(),
                        valorOuPadrao(importado.grupo(), "OUTROS"),
                        valorOuPadrao(importado.natureza(), "VARIAVEL"));
        LancamentoPlanejado lancamento = new LancamentoPlanejado(importado.id(), importado.descricao(),
                tipo, importado.valorComSinal().absoluto(), importado.data(),
                StatusLancamentoPlanejado.LIQUIDADO, null, null, categoria, null, importado.id(),
                MarcacaoPlanejamento.NENHUMA);
        return new ItemConsulta(lancamento,
                new MetadadosConsulta(importado.contaOuCartao(), false, importado.origem()));
    }

    private static String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    private static boolean ehTransferencia(LancamentoImportado importado) {
        return java.util.stream.Stream.of(importado.categoria(), importado.grupo())
                .filter(java.util.Objects::nonNull)
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .anyMatch(valor -> valor.contains("TRANSFERENCIA") || valor.contains("TRANSFERÊNCIA"));
    }

    private static boolean correspondeAoStatus(LancamentoPlanejado item, String status, LocalDate hoje) {
        if (status == null || status.isBlank()) return true;
        if ("ATRASADO".equals(status)) {
            return item.status() == StatusLancamentoPlanejado.PENDENTE
                    && item.vencimento().isBefore(hoje);
        }
        return item.status().name().equals(status);
    }

    private static Dinheiro soma(List<LancamentoPlanejado> itens, TipoLancamentoPlanejado tipo,
                                 StatusLancamentoPlanejado status) {
        return itens.stream()
                .filter(item -> item.tipo() == tipo)
                .filter(item -> status == null || item.status() == status)
                .map(LancamentoPlanejado::valor)
                .reduce(Dinheiro.ZERO, Dinheiro::somar);
    }

    public record Filtro(Competencia competencia, String tipo, String status, UUID contaId, UUID cartaoId,
                         String categoria, String texto, int pagina, int tamanho) {
        public Filtro {
            Objects.requireNonNull(competencia, "competencia é obrigatória");
            if (pagina < 0 || tamanho < 1 || tamanho > 100) throw new IllegalArgumentException("paginação inválida");
        }
    }
    public record Resultado(
            List<LancamentoPlanejado> itens,
            int totalDeItens,
            Dinheiro totalDespesas,
            Dinheiro totalReceitas,
            Dinheiro saldoRealizado,
            Dinheiro saldoPrevisto,
            int quantidadeAtrasados,
            int pagina,
            int tamanho,
            boolean temMais,
            Map<UUID, MetadadosConsulta> metadados) { }

    public record MetadadosConsulta(String contaOuCartao, boolean editavel, String origem) { }

    private record ItemConsulta(LancamentoPlanejado lancamento, MetadadosConsulta metadados) { }
}
