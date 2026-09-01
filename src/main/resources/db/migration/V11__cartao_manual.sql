-- =============================================================================
-- Cadastro manual de cartão: nome + limite opcional + valor da fatura digitado à
-- mão, até o spike do endpoint `bills` da Pluggy (ver ROADMAP) permitir buscar a
-- fatura de verdade. Tabela própria, separada de `conta` (V10) — aquela é 100%
-- automática, reescrita por inteiro a cada sync (RN-01); esta é editada por
-- Felipe. `identificador_conta_pluggy` é só um id solto de correlação com
-- `conta.identificador`, sem FK: os dois nunca precisam existir em sincronia.
--
-- DELETE é soft: `ativo` marca a remoção, a linha nunca é apagada.
-- =============================================================================

create table cartao_manual (
    id                          uuid          primary key,
    nome                        text          not null,
    limite                      numeric(14,2),
    valor_fatura                numeric(14,2) not null default 0,
    origem_fatura               text          not null default 'MANUAL'
                                                check (origem_fatura in ('MANUAL','PLUGGY_BILL')),
    identificador_conta_pluggy  text,
    observacao                  text,
    ativo                       boolean       not null default true,
    atualizado_em               timestamptz   not null default now(),

    constraint cartao_manual_nome_preenchido check (length(btrim(nome)) > 0),
    constraint cartao_manual_limite_nao_negativo check (limite is null or limite >= 0),
    constraint cartao_manual_valor_fatura_nao_negativo check (valor_fatura >= 0)
);

create index idx_cartao_manual_ativo on cartao_manual (ativo);
