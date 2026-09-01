package br.com.felipe.termometro.cartao.fatura.application.repository;

import br.com.felipe.termometro.cartao.fatura.domain.PagamentoFatura;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;

public interface PagamentoFaturaRepository {
    List<PagamentoFatura> buscaPorCompetencia(Competencia competencia);
    PagamentoFatura salva(PagamentoFatura pagamento);
}
