create table meta_contribuicao (
    nome text primary key check (nome in ('DIZIMO','OFERTA')),
    percentual_alvo numeric(7,6) not null,
    percentual_atual numeric(7,6) not null default 0,
    passo_incremento numeric(7,6) not null default 0.02
);

insert into meta_contribuicao (nome, percentual_alvo, percentual_atual, passo_incremento) values
    ('DIZIMO', 0.10, 0, 0.02),
    ('OFERTA', 0.10, 0, 0.02);
