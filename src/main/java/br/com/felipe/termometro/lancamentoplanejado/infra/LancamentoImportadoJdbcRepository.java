package br.com.felipe.termometro.lancamentoplanejado.infra;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LancamentoImportadoJdbcRepository implements LancamentoImportadoRepository {
    private final JdbcTemplate jdbcTemplate;

    public LancamentoImportadoJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LancamentoImportado> buscaPorCompetencia(Competencia competencia) {
        return jdbcTemplate.query("""
                select id, descricao, valor, data, identificador_conta,
                       categoria, grupo, natureza, origem
                from transacao
                where data between ? and ?
                  and ignorada = false
                  and origem <> 'MANUAL'
                order by data desc, id
                """,
                (resultado, linha) -> new LancamentoImportado(
                        resultado.getObject("id", java.util.UUID.class),
                        resultado.getString("descricao"),
                        Dinheiro.de(resultado.getBigDecimal("valor")),
                        resultado.getObject("data", java.time.LocalDate.class),
                        resultado.getString("identificador_conta"),
                        resultado.getString("categoria"),
                        resultado.getString("grupo"),
                        resultado.getString("natureza"),
                        resultado.getString("origem")),
                competencia.primeiroDia(), competencia.ultimoDia());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<java.time.LocalDate> primeiraData() {
        return Optional.ofNullable(jdbcTemplate.queryForObject("""
                select min(data)
                from transacao
                where ignorada = false
                  and origem <> 'MANUAL'
                """, java.time.LocalDate.class));
    }
}
