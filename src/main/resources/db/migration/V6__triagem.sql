-- =============================================================================
-- Triagem das quatro cores (RN-05): a transação já classificada (M5) ganha uma
-- etiqueta — AZUL, AMARELA, VERMELHA, VERDE ou NAO_TRIADA.
-- =============================================================================

alter table transacao
    add column etiqueta text;

-- Só interessa filtrar rápido o que ainda não foi triado ou o que ficou pendente
-- de piso — o resto é lido por competência inteira (ver TriagemSpringDataJpaRepository).
create index idx_transacao_etiqueta on transacao (data desc)
    where etiqueta is null or etiqueta = 'NAO_TRIADA';
