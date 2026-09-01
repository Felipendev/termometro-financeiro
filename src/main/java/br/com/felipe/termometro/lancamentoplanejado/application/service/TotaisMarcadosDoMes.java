package br.com.felipe.termometro.lancamentoplanejado.application.service;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Faz das despesas marcadas a fonte das premissas antes mantidas numa tela separada. */
@Service
@RequiredArgsConstructor
public class TotaisMarcadosDoMes {
    private final LancamentoPlanejadoRepository repository;

    public Dinheiro total(Competencia competencia, MarcacaoPlanejamento marcacao) {
        return Dinheiro.somaDe(repository.buscaPorCompetencia(competencia).stream()
                .filter(item -> item.status() != StatusLancamentoPlanejado.CANCELADO)
                .filter(item -> item.marcacaoPlanejamento() == marcacao)
                .map(item -> item.valor())
                .toList());
    }

    /** Mantém o catálogo legado apenas enquanto nenhuma despesa do mês tiver a nova marcação. */
    public Dinheiro marcadoOuLegado(Competencia competencia, MarcacaoPlanejamento marcacao,
            Dinheiro legado) {
        Dinheiro marcado = total(competencia, marcacao);
        return marcado.ehZero() ? legado : marcado;
    }
}
