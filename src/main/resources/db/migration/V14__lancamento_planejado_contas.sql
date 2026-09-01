-- As colunas já existem em V13 para novas instalações. Mantemos esta migração idempotente
-- para bancos que receberam uma versão intermediária da funcionalidade.
alter table lancamento_planejado add column if not exists conta_origem_id uuid;
alter table lancamento_planejado add column if not exists conta_destino_id uuid;
