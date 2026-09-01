-- =============================================================================
-- Classificação (M5): a transação passa a saber o que é, e — o que importa para
-- o orçamento — se entra ou não na verba diária.
-- =============================================================================

alter table transacao
    add column categoria      text,
    add column grupo          text,
    add column natureza       text,
    add column confianca      numeric(3,2),
    add column origem_regra   text,
    add column precisa_revisao boolean not null default true,
    add column classificado_em timestamptz;

-- A fila de revisão da RN-12 é lida o tempo todo pela tela de não identificados;
-- índice parcial porque só interessa o que ainda não foi resolvido.
create index idx_transacao_revisao on transacao (data desc)
    where precisa_revisao;

create index idx_transacao_categoria on transacao (categoria, data desc);

-- Regras criadas pelo usuário e aprendidas a partir das correções dele (RN-12).
-- O catálogo do sistema NÃO vive aqui: ele é código, versionado com o resto, para
-- que uma mudança de regra apareça no diff em vez de sumir dentro de um INSERT.
create table regra_categorizacao (
    id           uuid          primary key,
    prioridade   int           not null,
    tipo         text          not null,
    padrao       text          not null,
    categoria    text          not null,
    grupo        text          not null,
    natureza     text          not null,
    origem       text          not null,
    criado_em    timestamptz   not null default now(),

    constraint regra_padrao_preenchido check (length(btrim(padrao)) > 0),
    constraint regra_origem_valida check (origem in ('SISTEMA', 'APRENDIZADO', 'USUARIO')),
    constraint regra_unica unique (tipo, padrao, origem)
);
