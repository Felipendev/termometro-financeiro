-- RN-22: memória mínima de "já avisei isso" para os alertas proativos que não podem repetir todo
-- dia (marco atingido, evento próximo, verba baixa). Chave-valor deliberadamente genérico em vez
-- de uma tabela por gatilho: o volume e a variedade de chaves são pequenos demais para justificar
-- 3 tabelas quase idênticas. atualizado_em é só para depuração manual — nenhuma decisão do sistema
-- lê essa coluna, e ela só é preenchida pelo default na inserção (não é atualizada em cima de uma
-- chave já existente).
create table estado_de_alerta (
    chave         text        primary key,
    valor         text        not null,
    atualizado_em timestamptz not null default now()
);
