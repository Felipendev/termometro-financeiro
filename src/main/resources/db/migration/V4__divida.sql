-- =============================================================================
-- Fatia 4 (RN-08): dívidas com parcela fixa e prazo conhecido, para somar em
-- ServicoDivida(m). Deliberadamente mais simples que o `divida` completo da
-- especificação (seção 4) — sem saldo devedor nem taxa de juros, porque RN-08
-- só precisa saber "quanto cai de parcela neste mês", não "quando quita" (isso
-- é RN-09, ainda não implementada). Quando a fatia 9 chegar, esta tabela ganha
-- os campos que faltam em vez de ser substituída.
-- =============================================================================

create table divida_ativa (
    id                        uuid          primary key,
    nome                      text          not null,
    valor_parcela             numeric(14,2) not null,
    competencia_ultima_parcela date         not null,
    observacao                text,
    criado_em                 timestamptz   not null default now(),
    atualizado_em             timestamptz   not null default now(),

    constraint divida_nome_preenchido check (length(btrim(nome)) > 0),
    constraint divida_parcela_positiva check (valor_parcela > 0),
    constraint divida_competencia_e_dia_1 check (extract(day from competencia_ultima_parcela) = 1)
);

-- Empréstimo Nubank (aba "Saída da dívida" da planilha): 4 parcelas de R$ 2.058,05,
-- 3 já pagas até agosto/2026, a última cai em setembro/2026. Depois disso, zero.
insert into divida_ativa (id, nome, valor_parcela, competencia_ultima_parcela, observacao) values
    ('d1b0a000-0000-4000-8000-000000000001', 'Empréstimo Nubank', 2058.05, '2026-09-01',
        '4x R$ 2.058,05 — 3 pagas, última em 08/set/2026. Depois disso, zero.');
