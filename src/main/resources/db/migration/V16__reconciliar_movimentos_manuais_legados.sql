-- Antes de evento_id existir, editar/reabrir/cancelar um lançamento não conseguia localizar
-- o movimento já enviado às análises. Ignoramos apenas movimentos MANUAIS legados que possuem
-- um lançamento homônimo, na mesma data, atualmente pendente ou cancelado.
UPDATE transacao t
SET ignorada = true
WHERE t.origem = 'MANUAL'
  AND t.identificador_conta = 'manual-planejado'
  AND t.evento_id IS NULL
  AND t.ignorada = false
  AND EXISTS (
      SELECT 1
      FROM lancamento_planejado l
      WHERE l.descricao = t.descricao
        AND l.vencimento = t.data
        AND l.status IN ('PENDENTE', 'CANCELADO')
  );
