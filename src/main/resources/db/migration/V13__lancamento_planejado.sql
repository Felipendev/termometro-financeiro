create table lancamento_planejado (
    id uuid primary key,
    descricao text not null,
    tipo text not null check (tipo in ('DESPESA','RECEITA','TRANSFERENCIA')),
    valor numeric(14,2) not null check (valor > 0),
    vencimento date not null,
    status text not null check (status in ('PENDENTE','LIQUIDADO','CANCELADO')),
    conta_origem_id uuid,
    conta_destino_id uuid,
    transacao_id uuid,
    atualizado_em timestamptz not null default now(),
    constraint lancamento_planejado_descricao_preenchida check (length(btrim(descricao)) > 0)
);
create index idx_lancamento_planejado_status_vencimento on lancamento_planejado (status, vencimento);
