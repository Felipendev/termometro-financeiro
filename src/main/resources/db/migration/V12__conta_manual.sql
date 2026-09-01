create table conta_manual (
    id uuid primary key,
    identificador text not null unique,
    nome text not null,
    tipo text not null check (tipo in ('CORRENTE','POUPANCA')),
    saldo numeric(14,2) not null,
    ativa boolean not null default true,
    atualizado_em timestamptz not null default now(),
    constraint conta_manual_identificador_preenchido check (length(btrim(identificador)) > 0),
    constraint conta_manual_nome_preenchido check (length(btrim(nome)) > 0)
);
create index idx_conta_manual_ativa on conta_manual (ativa);
