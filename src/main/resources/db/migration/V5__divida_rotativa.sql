-- =============================================================================
-- Fatia 9 (RN-09): a dívida em aberto no cartão que a projeção efetivamente
-- simula pagando mês a mês. Deliberadamente uma tabela separada de
-- `divida_ativa` (V4) em vez de estender aquela: `divida_ativa` modela dívida
-- de PARCELA FIXA e prazo já conhecido (o empréstimo Nubank quita sozinho,
-- independente de qualquer estratégia de amortização) — o motor de projeção
-- não tem nada a decidir sobre ela, ela já é `saida_fixa(m)`. Esta tabela
-- modela o saldo ROTATIVO: um valor e uma taxa, sem parcela nem prazo fixos,
-- que só quita porque o motor decide quanto amortizar a cada mês. As duas
-- semânticas de pagamento são diferentes o bastante para não valer a pena
-- forçar num único formato agora — o `divida` unificado da seção 4 da spec
-- (com `tipo` discriminando PARCELAMENTO_FIXO/ROTATIVO) fica como refactor
-- futuro, quando as duas tabelas já estiverem provadas em uso.
-- =============================================================================

create table divida_rotativa (
    id                  uuid          primary key,
    nome                text          not null,
    saldo_devedor       numeric(14,2) not null,
    taxa_juros_mensal   numeric(8,6)  not null,
    taxa_estimada       boolean       not null default false,
    observacao          text,
    criado_em           timestamptz   not null default now(),
    atualizado_em       timestamptz   not null default now(),

    constraint divida_rotativa_nome_preenchido check (length(btrim(nome)) > 0),
    constraint divida_rotativa_saldo_nao_negativo check (saldo_devedor >= 0),
    constraint divida_rotativa_taxa_nao_negativa check (taxa_juros_mensal >= 0)
);

-- Saldo em aberto no cartão (aba "Saída da dívida" da planilha), pago via Pix no crédito.
-- Taxa medida diretamente na fatura, não estimada.
insert into divida_rotativa (id, nome, saldo_devedor, taxa_juros_mensal, taxa_estimada, observacao) values
    ('d2b0a000-0000-4000-8000-000000000001', 'Rotativo cartão (Pix no crédito)', 7952.24, 0.0636, false,
        'Taxa medida na fatura (Anexo C da spec) — 6,36% a.m. Alimenta o motor de projeção (RN-09), não o Saldo de Sobrevivência (RN-08), que só olha parcela/mês.');
