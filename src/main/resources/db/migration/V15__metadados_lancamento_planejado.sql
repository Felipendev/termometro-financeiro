alter table lancamento_planejado add column if not exists categoria varchar(100);
alter table lancamento_planejado add column if not exists grupo varchar(50);
alter table lancamento_planejado add column if not exists natureza varchar(30);
alter table lancamento_planejado add column if not exists cartao_manual_id uuid;
alter table lancamento_planejado add column if not exists transacao_id uuid;

create index if not exists idx_lancamento_planejado_cartao_manual on lancamento_planejado(cartao_manual_id);
create unique index if not exists uk_lancamento_planejado_transacao on lancamento_planejado(transacao_id) where transacao_id is not null;
