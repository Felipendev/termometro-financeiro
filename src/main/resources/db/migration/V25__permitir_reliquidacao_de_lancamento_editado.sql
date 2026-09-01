alter table transacao drop constraint if exists transacao_unica;

create unique index if not exists ux_transacao_ativa_conta_hash
    on transacao (identificador_conta, hash_dedupe)
    where ignorada = false;
