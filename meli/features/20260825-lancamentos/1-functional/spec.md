# Especificação funcional — Lançamentos

## Objetivo

Oferecer uma tela única, mensal e responsiva para consultar e administrar lançamentos manuais e importados. A referência do Organizze orienta a hierarquia visual: aviso de pendências, cabeçalho com competência, filtros compactos, lista por data e resumo final.

## Histórias e critérios

### L-01 — Consultar o período

Como usuário, quero navegar entre meses e ver os lançamentos em ordem de data, para entender o realizado e o previsto.

- A tela apresenta competência, anterior/próximo, busca e lista agrupada por dia.
- Cada item exibe ícone/categoria, descrição, conta ou cartão, status e valor. A recorrência mensal é indicada pela marcação `CUSTO_FIXO`, exibida com ícone e rótulo próprios.
- Despesas usam vermelho e sinal negativo; receitas usam verde e sinal positivo; transferências têm tratamento neutro e não entram nos totais de receita/despesa.
- O rodapé mostra saldo realizado e saldo previsto da competência.

### L-02 — Filtrar sem perder contexto

Como usuário, quero combinar filtros para encontrar um lançamento.

- Tipo, status, conta/cartão, categoria e texto podem ser usados juntos.
- Todo filtro respeita a competência selecionada.
- A URL não precisa guardar filtros nesta fase; trocar de mês preserva os filtros já escolhidos.
- A lista é paginada e informa total, página atual e estado vazio.

### L-03 — Criar e repetir

Como usuário, quero criar despesa, receita ou transferência sem sair da tela.

- O botão “Novo lançamento” reaproveita o modal rápido existente.
- O modal permite categoria, conta ou cartão, valor monetário mascarado, data e “Salvar e criar outra”.
- A categoria escolhida é aplicada antes da triagem, fazendo o item aparecer imediatamente nos gráficos e na home.

### L-04 — Administrar com segurança

Como usuário, quero editar, liquidar, reabrir ou excluir o que lancei manualmente.

- Lançamento manual pendente pode ser editado, liquidado, cancelado ou excluído após confirmação.
- Lançamento manual liquidado pode ser reaberto somente revertendo, de forma atômica, o movimento e saldo que ele gerou.
- Importações não podem ser excluídas silenciosamente: a tela apresenta apenas ação de revisar/classificar e explica sua origem.
- Exclusão de manual pode ser definitiva após confirmação explícita. Lançamento liquidado deve ser reaberto antes da exclusão para reverter saldo e análises.

### L-05 — Pendências claras

Como usuário, quero ser avisado de vencimentos não liquidados.

- Pendências vencidas aparecem em aviso no topo e recebem destaque na lista.
- Não há pendência: o aviso não ocupa espaço.
- Sem lançamentos no período: orientar criação ou importação.

## Checklist de aceite

- [x] Manual: criar, visualizar, editar, cancelar e liquidar/reabrir.
- [x] Filtros combináveis respeitam competência e paginação.
- [x] Categoria explícita atualiza home, gráfico e previsão.
- [x] Transferência não infla receitas/despesas nem saldo consolidado.
- [x] Importado não é apagado pelo fluxo manual.
- [x] Desktop/mobile e teclado cobertos por validação visual e testes.

## Ampliação visual — decisão de 2026-08-25

- A navegação principal passa a priorizar **Visão geral**, **Lançamentos**, **Cartões** e **Relatórios**.
- As telas genéricas **Contas e cartões** e **Planejamento** deixam de existir como destinos principais.
- Valores antes configurados no planejamento passam a ser receitas/despesas com flags de `CUSTO_FIXO` e `PISO_HUMANO`, alimentando previsões e Relatórios.
- Relatórios apresentam categorias, entradas e saídas e contas/cartões por competência.
- Ícones devem vir de uma biblioteca consistente ou de assets SVG próprios; emojis e glifos improvisados não são aceitos.
- O campo monetário inicia visualmente em `R$ 0,00` e cada dígito desloca centavos da direita para a esquerda (`1` → `0,01`; `123456` → `1.234,56`).
