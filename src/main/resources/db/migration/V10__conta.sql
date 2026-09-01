-- =============================================================================
-- Conta (extensão da fatia 13, feature Cartões): persiste o que o Pluggy já
-- manda em GET /accounts (nome, tipo, limite, saldo) e até agora era descartado
-- logo depois de normalizar o sinal das transações (ContaBancaria era só um
-- objeto de passagem dentro do sync). Alimenta GET /v1/cartoes — gasto real por
-- cartão na competência, sem depender de classificação/não-gasto/triagem terem
-- rodado, já que é soma direta das transações da seção CARTAO.
-- =============================================================================

create table conta (
    id             uuid          primary key,
    identificador  text          not null unique,
    nome           text          not null,
    tipo           text          not null check (tipo in ('CORRENTE','CARTAO_CREDITO','POUPANCA')),
    limite         numeric(14,2),
    saldo          numeric(14,2),
    atualizado_em  timestamptz   not null default now(),

    constraint conta_identificador_preenchido check (length(btrim(identificador)) > 0),
    constraint conta_nome_preenchido check (length(btrim(nome)) > 0)
);

create index idx_conta_tipo on conta (tipo);
