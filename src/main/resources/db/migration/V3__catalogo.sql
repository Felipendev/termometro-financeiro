-- =============================================================================
-- Catálogo (fatia 4/5): as premissas que a RN-16 (viabilidade) e depois a RN-08
-- (saldo de sobrevivência) precisam — renda declarada, custo fixo item a item e
-- piso humano por categoria.
--
-- Os VALORES abaixo não são dado de exemplo: são a resposta de Felipe à RN-17
-- ("o sistema pergunta o que falta"), já calibrados e validados na planilha
-- `termometro-felipe.xlsx` (aba Premissas). Diferente do catálogo de regras de
-- categorização (V2), que é código porque é regra do SISTEMA, isto aqui é dado
-- declarado pelo USUÁRIO — pertence ao banco, editável depois por API, com este
-- INSERT como estado inicial.
-- =============================================================================

create table renda_declarada (
    competencia    date          primary key,
    valor_liquido  numeric(14,2) not null,
    observacao     text,
    criado_em      timestamptz   not null default now(),
    atualizado_em  timestamptz   not null default now(),

    constraint renda_competencia_e_dia_1 check (extract(day from competencia) = 1),
    constraint renda_nao_negativa check (valor_liquido >= 0)
);

create table custo_fixo_item (
    id               uuid          primary key,
    nome             text          not null,
    valor            numeric(14,2) not null,
    forma_pagamento  text,
    observacao       text,
    ativo            boolean       not null default true,
    criado_em        timestamptz   not null default now(),
    atualizado_em    timestamptz   not null default now(),

    constraint custo_fixo_nome_preenchido check (length(btrim(nome)) > 0),
    constraint custo_fixo_valor_nao_negativo check (valor >= 0)
);

create index idx_custo_fixo_ativo on custo_fixo_item (ativo) where ativo;

create table piso_humano (
    id             uuid          primary key,
    categoria      text          not null unique,
    valor_piso     numeric(14,2) not null,
    justificativa  text,
    estimado       boolean       not null default false,
    criado_em      timestamptz   not null default now(),
    atualizado_em  timestamptz   not null default now(),

    constraint piso_categoria_preenchida check (length(btrim(categoria)) > 0),
    constraint piso_valor_nao_negativo check (valor_piso >= 0)
);

-- --------------------------------------------------------------------- SEED

insert into renda_declarada (competencia, valor_liquido, observacao) values
    ('2026-09-01', 10000.00, 'PJ, sem variação declarada — R$ 10.000/mês fixo');

insert into custo_fixo_item (id, nome, valor, forma_pagamento, observacao) values
    ('a1c1e001-0000-4000-8000-000000000001', 'Aluguel', 2200.00, 'CONTA',
        'vence dia 25; pago com o limite Itaú até o salário cair dia 5'),
    ('a1c1e001-0000-4000-8000-000000000002', 'Imposto PJ (DARF)', 816.42, 'CARTAO',
        'medido nas faturas — Pix Ministério da Fazenda'),
    ('a1c1e001-0000-4000-8000-000000000003', 'Contador', 517.67, 'CARTAO',
        'R$ 500 + custo do Pix recorrente no crédito'),
    ('a1c1e001-0000-4000-8000-000000000004', 'Energia (Energisa)', 300.00, 'CONTA', null),
    ('a1c1e001-0000-4000-8000-000000000005', 'Internet (Tely)', 129.90, 'CARTAO',
        'medido na fatura Nubank'),
    ('a1c1e001-0000-4000-8000-000000000006', 'Anthropic / Claude', 110.00, 'CARTAO',
        'medido na fatura PicPay'),
    ('a1c1e001-0000-4000-8000-000000000007', 'Água', 55.00, 'CONTA', null),
    ('a1c1e001-0000-4000-8000-000000000008', 'Google One', 49.99, 'CARTAO', null),
    ('a1c1e001-0000-4000-8000-000000000009', 'Apple', 25.40, 'CARTAO', null),
    ('a1c1e001-0000-4000-8000-00000000000a', 'Amazon Prime', 19.99, 'CARTAO', null),
    ('a1c1e001-0000-4000-8000-00000000000b', 'iFood Club', 19.88, 'CARTAO', null),
    ('a1c1e001-0000-4000-8000-00000000000c', 'Claro Flex', 19.80, 'CARTAO', null);
-- total: R$ 4.264,05 — confere com o Painel da planilha

insert into piso_humano (id, categoria, valor_piso, justificativa) values
    ('b15000a0-0000-4000-8000-000000000001', 'Mercado', 700.00, '~R$ 175 por semana'),
    ('b15000a0-0000-4000-8000-000000000002', 'Comer fora', 160.00,
        '2x por mês, ticket de R$ 80 — declarado por você'),
    ('b15000a0-0000-4000-8000-000000000003', 'Transporte por app', 150.00,
        'deslocamentos que não dá para evitar'),
    ('b15000a0-0000-4000-8000-000000000004', 'Lavanderia', 120.00, null),
    ('b15000a0-0000-4000-8000-000000000005', 'Saúde/farmácia', 80.00,
        'medicamento e farmácia de rotina'),
    ('b15000a0-0000-4000-8000-000000000006', 'Lazer', 100.00,
        'não zerar lazer é o que faz o plano durar');
-- total: R$ 1.310,00 — confere com o Painel da planilha
