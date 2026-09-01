package br.com.felipe.termometro.orcamento.infra;

import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read model: o orçamento precisa dos gastos, que vivem na tabela da ingestão.
 *
 * <p>O acoplamento entre os domínios fica <b>aqui</b>, em infra, onde o banco já é compartilhado —
 * e não na camada de aplicação, onde ele viraria dependência de um domínio no outro. Por isso a
 * consulta é nativa e devolve uma projeção, em vez de mapear a entidade de transação.
 */
interface GastoDiarioSpringDataJpaRepository extends Repository<TransacaoJpaEntity, UUID> {

    /**
     * Soma, por dia, os gastos que entram na conta do dia a dia (RN-19).
     *
     * <p>Fica de fora: o que está vinculado a evento (tem orçamento próprio na provisão), o que a
     * categorização marcou como fora do dia a dia — fixo e parcela, a partir do M5 — e qualquer
     * lançamento que não seja saída. O sinal é invertido no SELECT porque a RN-01 guarda despesa
     * como negativo e o orçamento trabalha com valor positivo.
     */
    @Query(value = """
            select t.data as data, sum(-t.valor) as total
              from transacao t
             where t.data >= :primeiroDia
               and t.data <= :ultimoDia
               and t.conta_no_dia_a_dia
               and not t.ignorada
               and t.evento_id is null
               and t.valor < 0
             group by t.data
             order by t.data
            """, nativeQuery = true)
    List<GastoDiarioProjection> somaPorDia(@Param("primeiroDia") LocalDate primeiroDia,
                                           @Param("ultimoDia") LocalDate ultimoDia);
}
