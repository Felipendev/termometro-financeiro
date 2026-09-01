-- A V16 reconciliava somente por descrição e data. Isso era suficiente para recuperar
-- os lançamentos já existentes, mas podia ocultar movimentos legítimos com o mesmo nome.
-- Reavaliamos exclusivamente aquele escopo legado, agora respeitando tipo, valor e
-- cardinalidade: um lançamento pendente/cancelado só pode ocultar um movimento.
--
-- O fator 10 preserva o erro histórico já identificado no campo monetário (por exemplo,
-- R$ 2.200,00 persistido como R$ 22.000,00) sem voltar ao pareamento aberto da V16.
with escopo_legado as (
    select
        t.id,
        t.descricao,
        t.data,
        t.valor,
        row_number() over (
            partition by t.descricao, t.data, sign(t.valor), abs(t.valor)
            order by t.id
        ) as posicao
    from transacao t
    where t.origem = 'MANUAL'
      and t.identificador_conta = 'manual-planejado'
      and t.evento_id is null
      and exists (
          select 1
          from lancamento_planejado l
          where l.descricao = t.descricao
            and l.vencimento = t.data
            and l.status in ('PENDENTE', 'CANCELADO')
      )
), reconciliacao as (
    select
        e.id,
        e.posicao,
        (
            select count(*)
            from lancamento_planejado l
            where l.descricao = e.descricao
              and l.vencimento = e.data
              and l.status in ('PENDENTE', 'CANCELADO')
              and (
                    (l.tipo = 'DESPESA' and e.valor < 0)
                 or (l.tipo = 'RECEITA' and e.valor > 0)
              )
              and (
                    abs(e.valor) = l.valor
                 or abs(e.valor) = l.valor * 10
              )
        ) as quantidade_compativel
    from escopo_legado e
)
update transacao t
set ignorada = r.quantidade_compativel > 0
               and r.posicao <= r.quantidade_compativel
from reconciliacao r
where t.id = r.id;
