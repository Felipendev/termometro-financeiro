# Especificação técnica — Lançamentos

## Decisões

1. `lancamento_planejado` continua sendo a fonte de estado para manuais; `transacao` continua sendo a fonte de fatos realizados/importados.
2. A categoria explícita não cria uma nova tabela global. Ela armazena `categoria`, `grupo` e `natureza` no lançamento manual e, na liquidação, aplica a mesma classificação à transação vinculada antes de executar a triagem.
3. Cartão manual é forma de pagamento, não saldo bancário. Conta manual afeta saldo quando é origem/destino.
4. Cancelar é soft-state (`CANCELADO`); apagar transação importada é proibido na API.
5. Reabrir só é permitido para manual liquidado e reverte uma transação vinculada, de modo idempotente e transacional.

## Modelo e migrações

Estender `lancamento_planejado` com:

- `categoria varchar`, `grupo varchar`, `natureza varchar` opcionais;
- `cartao_manual_id uuid` opcional;
- `transacao_id uuid` opcional e único;
- `marcacao_planejamento` registra `NENHUMA`, `CUSTO_FIXO` ou `PISO_HUMANO`; o estado e `atualizado_em` formam a trilha operacional, sem timestamps redundantes por comando.

Índices: `(status, vencimento)`, `(vencimento, status)`, `(conta_origem_id)`, `(conta_destino_id)`, `(cartao_manual_id)` e unicidade de `transacao_id`.

## API

- `GET /v1/lancamentos?competencia=&tipo=&status=&contaId=&cartaoId=&categoria=&q=&pagina=&tamanho=` retorna itens, paginação, aviso de vencidas e totais realizado/previsto.
- `PUT /v1/lancamentos-planejados/{id}` cria/edita o manual pendente; request inclui categoria e forma de pagamento.
- `POST /v1/lancamentos-planejados/{id}/liquidar`, `/reabrir`, `/cancelar` são comandos idempotentes.
- `PUT /v1/lancamentos-planejados/{id}` altera a categoria do manual pendente; `POST /v1/transacoes/{id}/classificar` revisa a categoria de um importado e pode aprender para o grupo. Em ambos os fluxos, o frontend reexecuta análise/triagem da competência afetada.

## Testes obrigatórios

- Unidade: filtros combináveis, sinal/totais, categoria explícita, cancelamento/reabertura idempotentes e transferência equilibrada.
- Integração: Flyway, listagem paginada, edição/cancelamento de manual e bloqueio de importado.
- Frontend: máscara de dinheiro, filtros, estado vazio, ações por status, modal “criar outra” e sinais/cores.

## Ampliação visual e de classificação

- Adotar `lucide-react` para ações e meios de pagamento; categorias usam um componente central de ícone, permitindo futura troca por SVGs próprios.
- Substituir o valor formatado como fonte de edição por uma sequência de centavos; a máscara sempre deriva apenas dos dígitos.
- Acrescentar flags de lançamento (`CUSTO_FIXO`, `PISO_HUMANO`) ao modelo manual e aos filtros/relatórios.
- Após CRUD ou classificação manual, invalidar e recarregar consulta de lançamentos, dashboard, categorias e previsões da competência afetada.
