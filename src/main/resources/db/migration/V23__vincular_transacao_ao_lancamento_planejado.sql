alter table transacao
    add column lancamento_planejado_id uuid;

alter table transacao
    add constraint transacao_lancamento_planejado_id_fkey
        foreign key (lancamento_planejado_id)
        references lancamento_planejado (id)
        on delete set null;

create index idx_transacao_lancamento_planejado
    on transacao (lancamento_planejado_id)
    where lancamento_planejado_id is not null;
