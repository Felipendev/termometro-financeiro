alter table lancamento_planejado
    drop constraint ck_lancamento_planejado_marcacao;

alter table lancamento_planejado
    add constraint ck_lancamento_planejado_marcacao
    check (marcacao_planejamento in ('NENHUMA', 'CUSTO_FIXO', 'PISO_HUMANO', 'RECEITA_RECORRENTE'));
