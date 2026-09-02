alter table lancamento_planejado
    add column serie_id uuid,
    add column dia_recorrencia integer;

alter table lancamento_planejado
    add constraint ck_lancamento_planejado_dia_recorrencia
    check (dia_recorrencia is null or dia_recorrencia between 1 and 31);

create index idx_lancamento_planejado_serie
    on lancamento_planejado (serie_id) where serie_id is not null;
