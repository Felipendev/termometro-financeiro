-- =============================================================================
-- Estrutura mínima do MVP: orçamento (verba + eventos) e transações.
-- O resto do modelo da especificação entra quando as fatias correspondentes
-- chegarem — migration que cria tabela sem código que a use é dívida, não preparo.
-- =============================================================================

-- ---------------------------------------------------------------- ORÇAMENTO
create table verba_mensal (
    competencia     date         primary key,
    verba_variavel  numeric(14,2) not null,
    provisao        numeric(14,2) not null,
    criado_em       timestamptz   not null default now(),
    atualizado_em   timestamptz   not null default now(),

    constraint verba_mensal_competencia_e_dia_1 check (extract(day from competencia) = 1),
    constraint verba_mensal_verba_nao_negativa  check (verba_variavel >= 0),
    constraint verba_mensal_provisao_nao_negativa check (provisao >= 0),
    -- RN-20: a provisão fica DENTRO da verba. A invariante vive no domínio
    -- (VerbaMensal) e é repetida aqui de propósito: o banco é a última linha de
    -- defesa contra um caminho de escrita que um dia esqueça de passar pelo domínio.
    constraint verba_mensal_provisao_cabe_na_verba check (provisao <= verba_variavel)
);

create table evento (
    id           uuid          primary key,
    competencia  date          not null,
    data         date          not null,
    descricao    text          not null,
    valor        numeric(14,2) not null,
    realizado    boolean       not null default false,
    criado_em    timestamptz   not null default now(),

    constraint evento_valor_positivo check (valor > 0),
    constraint evento_descricao_preenchida check (length(btrim(descricao)) > 0),
    constraint evento_data_na_competencia
        check (data >= competencia and data < competencia + interval '1 month')
);

create index idx_evento_competencia on evento (competencia, data);

-- --------------------------------------------------------------- TRANSAÇÕES
create table transacao (
    id                    uuid          primary key,
    identificador_conta   text          not null,
    data                  date          not null,
    -- 'timestamp' sem fuso de propósito: a conversão de UTC para America/Fortaleza
    -- acontece na ingestão (edge case 22), e o que chega aqui já é hora local. Guardar
    -- com fuso convidaria a uma segunda conversão.
    data_hora             timestamp,
    hora_confiavel        boolean       not null default false,
    descricao             text          not null,
    descricao_original    text          not null,
    descricao_normalizada text          not null,
    -- RN-01: saída negativa, entrada positiva. Sempre.
    valor                 numeric(14,2) not null,
    cidade                text,
    categoria_banco       text,
    secao                 text          not null,
    parcela_numero        int,
    parcela_total         int,
    origem                text          not null,
    -- RN-02: separa cobranças idênticas do mesmo dia. Quatro máquinas de lavar
    -- de R$ 14,98 no mesmo dia são quatro máquinas, não uma.
    ordinal               int           not null default 0,
    -- RN-19: gasto vinculado a evento tem orçamento próprio na provisão e não
    -- entra na conta do dia a dia.
    evento_id             uuid          references evento (id),
    -- Preenchido pela categorização (M5). Até lá todo gasto conta como dia a dia.
    conta_no_dia_a_dia    boolean       not null default true,
    hash_dedupe           text          not null,
    criado_em             timestamptz   not null default now(),

    constraint transacao_unica unique (identificador_conta, hash_dedupe),
    constraint transacao_parcela_coerente check (
        (parcela_numero is null and parcela_total is null)
        or (parcela_total between 2 and 48 and parcela_numero between 1 and parcela_total)),
    constraint transacao_ordinal_nao_negativo check (ordinal >= 0)
);

create index idx_transacao_data on transacao (data desc);
create index idx_transacao_conta_data on transacao (identificador_conta, data desc);
create index idx_transacao_dia_a_dia on transacao (data)
    where conta_no_dia_a_dia and evento_id is null and valor < 0;
