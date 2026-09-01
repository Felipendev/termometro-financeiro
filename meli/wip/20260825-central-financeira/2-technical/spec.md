# Especificação técnica - Central financeira

## Decisões

1. **Categorias serão reutilizadas.** `classificacao` já persiste categoria, grupo, natureza e a `triagem` já devolve `ResumoDeCategoriaResponse`; não haverá tabela Categoria nova.
2. **Processamento pós-importação será um caso de uso próprio.** O controlador não coordenará regras de domínio. Um orquestrador recebe as competências afetadas e executa: classificação de novas transações -> conciliação de não-gasto -> geração de compromissos futuros -> triagem.
3. **Contas manuais não usam a tabela automática `conta`.** A sincronização Pluggy atual faz upsert de contas externas. Uma tabela `conta_manual` evita que sincronização apague edição local.
4. **Pendências são lançamentos planejados, não transações brutas.** Uma tabela `lancamento_planejado` contém vencimento/status; ao liquidar, cria transação de origem MANUAL vinculada. Transferência terá par vinculado por `transferencia_id` e será marcada como não-gasto.

## Arquitetura

```text
Home React
  | ações e leitura consolidada
  v
Dashboard API + Lançamentos API + Importações API
  |                       |                  |
  |                       |                  +-> Orquestrador pós-importação
  |                       |                           -> Classificação -> Não-gasto
  |                       |                           -> Compromissos -> Triagem
  v                       v
DashboardApplicationService    ContaManual / LancamentoPlanejado services
  |                       |
  +-----------------------+-----------------------> PostgreSQL/Flyway
```

## Dados e migrações

### `conta_manual`

`id uuid`, `identificador unique`, `nome`, `tipo (CORRENTE|POUPANCA)`, `saldo`, `ativa`, `criado_em`, `atualizado_em`.

### Extensão de `cartao`

Adicionar `identificador_importacao` único opcional, `data_vencimento` opcional e `data_fechamento` opcional. O endpoint de cartão manual passa a aceitar esses dados sem exigir Pluggy.

### `lancamento_planejado`

`id uuid`, `descricao`, `tipo (DESPESA|RECEITA|TRANSFERENCIA)`, `valor`, `vencimento`, `status (PENDENTE|LIQUIDADO|CANCELADO)`, `conta_origem_id`, `conta_destino_id`, `categoria`, `recorrencia`, `transacao_id`, `criado_em`.

Invariantes: valor positivo; transferência exige origem e destino distintos; lançamento liquidado possui transação vinculada; não pode liquidar duas vezes.

## APIs

- `POST /v1/importacoes/*`: resposta estendida com `competenciasProcessadas`, `classificadas`, `triadas`, `pendentesDeRevisao` e `novasTransacoes`.
- `GET /v1/dashboard/inicio?competencia=AAAA-MM`: DTO específico de home, com resumo de fluxo de caixa, contas, cartões associados, pendências e categorias. O endpoint dos Três Eus permanece compatível.
- `GET|POST|PUT|DELETE /v1/contas-manuais`.
- `GET|POST|PATCH /v1/lancamentos-planejados`; `POST /{id}/liquidar`.
- `POST /v1/lancamentos/manuais/despesa`, `/receita`, `/transferencia` para os atalhos.

### Correção do fluxo rápido

- A API de lançamento rápido recebe `categoria`, `grupo` e `natureza`; ao persistir o movimento, aplica essa classificação diretamente à transação recém-criada e só então roda a triagem.
- O formulário lista contas manuais e cartões manuais. Cartão é forma de pagamento e não altera saldo de conta; conta em despesa/receita altera o saldo quando selecionada.
- A soma da home inclui `totalNaoTriada`, para que nenhum lançamento classificado, mas sem piso configurado, desapareça da leitura.

## Regras de cálculo

- Receitas/despesas do resumo são transações não ignoradas na competência, separadas pelo sinal, sem transferências.
- Saldo consolidado = saldos de contas manuais ativas + saldos de contas sincronizadas, com precedência de identificador para evitar duplicação.
- Fatura exibida vem do cartão manual associado; gasto importado é informativo e não se soma novamente à fatura.
- Orçamento compara gasto variável classificado contra `VerbaMensal.verbaVariavel`.

## TDD e testes obrigatórios

### Unidade

- Orquestrador processa cada competência distinta uma vez, na ordem correta e ignora lote sem lançamentos novos.
- Regras de status de pendência cobrem atrasada, hoje, próxima, liquidada e cancelada.
- Liquidação idempotente; transferência cria par equilibrado e ignora ambos nas agregações.
- Resumo da home não duplica cartão/fatura e não conta pagamentos/transferências.
- Associação de cartão manual por identificador conecta gasto importado correto.

### Integração

- Migrações Flyway criam tabelas/índices e restrições.
- Importar CSV/PDF persiste, processa e `GET /dashboard/inicio` mostra a competência afetada.
- Endpoints de pendências preservam estado e recusam conta inexistente.

### Frontend

- Testes de componentes para estados vazios, ocultar valores, atalhos e atualização após importação.
- Verificação manual em viewport desktop e mobile com dados reais anonimizados; sem transmissão de arquivos pessoais durante teste automatizado.

## Segurança e operação

- Upload continua local para a API configurada; sem envio a Pluggy.
- Validar tipo/tamanho de arquivo e valores monetários no backend.
- Não registrar conteúdo de fatura ou descrição completa em logs de produção.
