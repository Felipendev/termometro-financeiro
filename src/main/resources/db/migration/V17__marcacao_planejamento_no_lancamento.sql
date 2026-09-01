alter table lancamento_planejado
    add column if not exists marcacao_planejamento varchar(30) not null default 'NENHUMA';

alter table lancamento_planejado
    add constraint ck_lancamento_planejado_marcacao
    check (marcacao_planejamento in ('NENHUMA', 'CUSTO_FIXO', 'PISO_HUMANO'));
