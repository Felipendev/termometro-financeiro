# Especificação técnica — Insights de gastos

## Decisão arquitetural

A primeira versão será implementada no frontend, como uma extensão de `GraficoComparativo`. O endpoint atual `GET /v1/visao-geral/comparativo-categorias` já entrega, por grupo, `atual`, `ideal`, `bom`, `ruim`, `valorAtual`, `rendaReferencia`, `fonte` e os itens que compõem o gasto. Isso é suficiente para construir a recomendação sem uma nova consulta, persistência ou regra duplicada no backend.

O cálculo permanece explicável:

1. converter a fração de referência para reais usando `rendaReferencia`;
2. escolher `ideal` como alvo quando o atual está entre ideal e bom; escolher `bom` quando está acima dele;
3. calcular `max(valorAtual - alvo, 0)`;
4. classificar a mensagem por faixa: dentro do ideal, atenção, acima do limite e prioritária acima de `ruim`;
5. ordenar somente as oportunidades de redução por esse valor, de forma decrescente, e limitar a três.

Nenhuma referência nula será substituída por padrão arbitrário. Quando `fonte === "CATALOGO"`, o componente indica explicitamente a estimativa e evita mensagem imperativa de corte.

## Componentes e contratos

- Criar um helper puro no frontend para transformar `PontoComparativoResponse` em um modelo de insight. Ele recebe os mesmos dados do gráfico, retorna estado, texto, alvo e economia necessária, e é testado isoladamente.
- `GraficoComparativo` renderiza `InsightsDeGastos` imediatamente depois do gráfico. O componente recebe a lista de pontos e um callback para abrir o grupo correspondente no detalhamento já existente.
- O detalhamento aberto continua sendo a lista `ponto.itens` já retornada pela API; não haverá segunda fonte de transações nem duplicação de valores.
- Os estilos entram em `frontend/src/App.css`, seguindo as variáveis e os breakpoints atuais do gráfico.

## Mudanças de backend

Não há alteração de API, banco, Flyway ou autenticação nesta fase. Caso a experiência mostre necessidade de recomendações históricas ou de metas configuráveis, isso será uma feature posterior com regra de domínio e contrato próprios.

## Testes obrigatórios

- Unidade do helper: dentro do ideal, entre ideal/bom, acima do bom, acima do ruim, referência ausente, fonte catálogo, ordenação e limite de três.
- Componente: textos, valores em reais, ação de abrir o grupo, estado sem recomendações e indicação de estimativa.
- Regressão do gráfico: tooltips dos quatro pontos, escala e detalhamento existente continuam funcionando.
- Validação: `npm test`, `npm run build` e `mvn test` (regressão do monorepo) ao fim da feature.
