package br.com.felipe.termometro.cartao.fatura.infra;

import br.com.felipe.termometro.cartao.fatura.application.repository.PagamentoFaturaRepository;
import br.com.felipe.termometro.cartao.fatura.domain.PagamentoFatura;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PagamentoFaturaJdbcRepository implements PagamentoFaturaRepository {
    private final JdbcTemplate jdbc;

    @Override
    public List<PagamentoFatura> buscaPorCompetencia(Competencia competencia) {
        return jdbc.query("""
                select id, referencia_cartao, nome_cartao, competencia, valor,
                       data_pagamento, lancamento_planejado_id
                from pagamento_fatura_cartao
                where competencia = ?
                order by data_pagamento desc, criado_em desc
                """, (rs, row) -> new PagamentoFatura(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getString("referencia_cartao"),
                        rs.getString("nome_cartao"),
                        Competencia.parse(rs.getString("competencia")),
                        Dinheiro.de(rs.getBigDecimal("valor")),
                        rs.getObject("data_pagamento", java.time.LocalDate.class),
                        rs.getObject("lancamento_planejado_id", java.util.UUID.class)),
                competencia.toString());
    }

    @Override
    public PagamentoFatura salva(PagamentoFatura pagamento) {
        jdbc.update("""
                insert into pagamento_fatura_cartao
                    (id, referencia_cartao, nome_cartao, competencia, valor,
                     data_pagamento, lancamento_planejado_id)
                values (?, ?, ?, ?, ?, ?, ?)
                """, pagamento.id(), pagamento.referenciaCartao(), pagamento.nomeCartao(),
                pagamento.competencia().toString(), pagamento.valor().valor(),
                pagamento.dataPagamento(), pagamento.lancamentoPlanejadoId());
        return pagamento;
    }
}
