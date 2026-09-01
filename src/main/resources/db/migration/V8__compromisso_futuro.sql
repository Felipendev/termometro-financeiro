-- =============================================================================
-- Fatia 7 (RN-04): compromissos futuros gerados a partir de compras parceladas
-- já sincronizadas. Quando uma parcela N/T chega pela ingestão, as T-N parcelas
-- restantes já estão contratadas e vão cair independentemente de qualquer
-- decisão futura (Parcela.java já documentava isso desde a fatia 2a).
--
-- Reconciliação por substituição: a cada geração, todas as linhas da mesma
-- série (identificador_conta + descricao_normalizada + parcela_total) são
-- apagadas e reinseridas a partir da parcela mais recente vista. Isso resolve
-- sozinho o caso em que a parcela N+1 já foi sincronizada como transação real
-- — a linha sintética dela é apagada, sem contagem dupla com
-- compromissosFuturosDoMes (que já soma as transações reais parceladas).
-- =============================================================================

create table compromisso_futuro (
    id                    uuid          primary key,
    identificador_conta   text          not null,
    descricao             text          not null,
    descricao_normalizada text          not null,
    categoria             text,
    competencia           date          not null,
    valor                 numeric(14,2) not null,
    parcela_numero        int           not null,
    parcela_total         int           not null,
    confirmado            boolean       not null default true,
    criado_em             timestamptz   not null default now(),

    constraint compromisso_futuro_valor_positivo check (valor > 0),
    constraint compromisso_futuro_parcela_valida
        check (parcela_numero > 0 and parcela_numero <= parcela_total),
    constraint compromisso_futuro_competencia_e_dia_1 check (extract(day from competencia) = 1),
    constraint compromisso_futuro_serie_parcela_unica
        unique (identificador_conta, descricao_normalizada, parcela_total, parcela_numero)
);

create index idx_compromisso_futuro_competencia on compromisso_futuro (competencia);
