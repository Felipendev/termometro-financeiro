-- A listagem mensal parte de vencimento; os vínculos de conta também são filtros da tela.
create index if not exists idx_lancamento_planejado_vencimento_status
    on lancamento_planejado (vencimento, status);

create index if not exists idx_lancamento_planejado_conta_origem
    on lancamento_planejado (conta_origem_id)
    where conta_origem_id is not null;

create index if not exists idx_lancamento_planejado_conta_destino
    on lancamento_planejado (conta_destino_id)
    where conta_destino_id is not null;
