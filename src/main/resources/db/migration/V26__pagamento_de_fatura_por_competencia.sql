create table pagamento_fatura_cartao (
    id uuid primary key,
    referencia_cartao varchar(180) not null,
    nome_cartao varchar(180) not null,
    competencia varchar(7) not null,
    valor numeric(14,2) not null check (valor > 0),
    data_pagamento date not null,
    -- A transação da aplicação grava os dois registros atomicamente. Sem FK aqui porque o
    -- lançamento é persistido pelo contexto JPA e este registro por JDBC antes do flush final.
    lancamento_planejado_id uuid not null unique,
    criado_em timestamp with time zone not null default current_timestamp
);

create index idx_pagamento_fatura_competencia
    on pagamento_fatura_cartao (competencia, referencia_cartao);

create table fatura_cartao_declarada (
    referencia_cartao varchar(180) not null,
    nome_cartao varchar(180) not null,
    competencia varchar(7) not null,
    valor numeric(14,2) not null check (valor >= 0),
    atualizado_em timestamp with time zone not null default current_timestamp,
    primary key (referencia_cartao, competencia)
);
