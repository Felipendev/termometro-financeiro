# Lançamentos

Feature concluída em 2026-08-26 para centralizar receitas, despesas e transferências por competência.

## Entregue

- listagem mensal paginada com filtros combináveis;
- criação e edição por modal monetário, categoria, conta ou cartão;
- liquidar, reabrir, cancelar e excluir definitivamente lançamentos manuais;
- proteção de itens importados, com revisão de categoria e origem explícita;
- saldos realizado e previsto, atraso, sinais e cores por tipo;
- atualização coordenada da tela inicial, categorias, análises e previsões;
- relatórios mensais de categorias, fluxo, contas e cartões;
- flags `CUSTO_FIXO` e `PISO_HUMANO` diretamente nos lançamentos;
- navegação e dashboard alinhados à linguagem visual solicitada.

## Documentos

- [Especificação funcional](1-functional/spec.md)
- [Especificação técnica](2-technical/spec.md)
- [Tasks e checklist](3-tasks/tasks.json)
- [Progresso](4-implementation/progress.md)
- [Resumo de implementação](implementation-summary.md)
- [Relatório de validação](VALIDATION_REPORT.md)

## Fora do fechamento local

A integração Pluggy de produção continua adiada até a liberação das credenciais. Antes de expor o webhook, configurar `PLUGGY_WEBHOOK_SECRET`.
