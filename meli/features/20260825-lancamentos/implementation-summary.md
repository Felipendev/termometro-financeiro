# Resumo de implementação

## Arquitetura

`lancamento_planejado` permanece como fonte de estado dos itens manuais. `transacao` permanece como fonte dos fatos realizados e importados. A consulta mensal unifica as duas origens sem permitir que itens importados sejam alterados ou apagados pelo CRUD manual.

Liquidação e reabertura são idempotentes. Transferências criam e estornam os dois lados de forma atômica e nunca entram nos totais de receita ou despesa. Categorias explícitas alimentam classificação, triagem, dashboard e previsões.

## Backend

- API mensal filtrável e paginada, com competência obrigatória;
- comandos de liquidação, reabertura e cancelamento;
- hard delete somente de lançamento manual permitido pelo domínio;
- revisão de importado limitada a transações cuja origem não é `MANUAL`;
- metadados de categoria, cartão, transação e marcação de planejamento;
- 19 migrações Flyway, incluindo índices de consulta e reconciliação legada segura;
- resposta HTTP 400 para competência ausente.

## Frontend

- navegação principal: Visão geral, Lançamentos, Cartões e Relatórios;
- máscara monetária de centavos com cursor fixado ao final;
- modal com categoria, cartão/conta, custo fixo, piso humano e “Salvar e criar outra”;
- listagem com ícones Lucide, origem, status, recorrência, ações e destaque de atraso;
- dashboard com totais, categorias, gráfico, faturas, compromissos e pendências;
- relatórios por movimentos da competência, excluindo transferências e cancelados.

## Decisões de escopo

- tags foram removidas por decisão do usuário;
- telas antigas de Planejamento e Contas e cartões deixaram de ser destinos principais;
- acesso Pluggy de produção não foi necessário para concluir o fluxo manual/importado local.
