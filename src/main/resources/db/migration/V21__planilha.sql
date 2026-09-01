create table saldo_inicial_planilha (
    id smallint primary key default 1,
    data_referencia date not null,
    valor numeric(14,2) not null,
    atualizado_em timestamptz not null default now(),
    constraint saldo_inicial_planilha_singleton check (id = 1)
);

create table diario_override (
    data date primary key,
    valor numeric(14,2) not null,
    atualizado_em timestamptz not null default now()
);

create table observacao_dia (
    data date primary key,
    texto text not null,
    atualizado_em timestamptz not null default now()
);
