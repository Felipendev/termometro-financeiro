alter table lancamento_planejado
    add column origem_receita varchar(30);

alter table lancamento_planejado
    add constraint lancamento_planejado_origem_receita_valida
        check (origem_receita is null or origem_receita in ('SALARIO', 'INVESTIMENTO', 'EMPRESTIMO'));

alter table lancamento_planejado
    add constraint lancamento_planejado_origem_compativel_com_tipo
        check (origem_receita is null or tipo = 'RECEITA');
