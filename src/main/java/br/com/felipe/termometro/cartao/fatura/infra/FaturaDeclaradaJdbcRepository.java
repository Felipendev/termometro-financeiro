package br.com.felipe.termometro.cartao.fatura.infra;

import br.com.felipe.termometro.cartao.fatura.application.repository.FaturaDeclaradaRepository;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FaturaDeclaradaJdbcRepository implements FaturaDeclaradaRepository {
    private final JdbcTemplate jdbc;

    @Override
    public Map<String, Dinheiro> buscaPorCompetencia(Competencia competencia) {
        Map<String, Dinheiro> resultado = new LinkedHashMap<>();
        RowCallbackHandler acumulaResultado = rs -> resultado.put(
                rs.getString("referencia_cartao"), Dinheiro.de(rs.getBigDecimal("valor")));
        jdbc.query("select referencia_cartao, valor from fatura_cartao_declarada where competencia = ?",
                acumulaResultado, competencia.toString());
        return Map.copyOf(resultado);
    }

    @Override
    public void salva(String referencia, String nome, Competencia competencia, Dinheiro valor) {
        jdbc.update("""
                insert into fatura_cartao_declarada
                    (referencia_cartao, nome_cartao, competencia, valor, atualizado_em)
                values (?, ?, ?, ?, current_timestamp)
                on conflict (referencia_cartao, competencia) do update
                set nome_cartao = excluded.nome_cartao, valor = excluded.valor,
                    atualizado_em = current_timestamp
                """, referencia, nome, competencia.toString(), valor.valor());
    }
}
