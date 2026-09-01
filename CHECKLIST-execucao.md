# Checklist de Execução — Nova Arquitetura do Termômetro

**Como usar:** cada fase tem seu critério de pronto ligado aos cenários Gherkin das specs
(`ESPEC-termometro-financeiro.md`, `ESPEC-planilha-viva.md`, `ESPEC-v2-nova-arquitetura.md`). Marca o
checkbox conforme avança. Ordem pensada por dependência real, não por RN em ordem crescente.

## Panorama

| Fase | Nome | RN | Status |
|---|---|---|---|
| 0 | Remoções | — | ✅ feito em 2026-08-28 (backend + frontend; só falta você limpar o `.env` real) |
| 1 | Padrão visual | — | ✅ feito em 2026-08-28 — tokens consolidados, paleta aplicada, CSS morto de media query limpo |
| 2 | Nova navegação (4 abas) | RN-26 | ✅ feito em 2026-08-28 — Planilha entrou, Cartões saiu da nav, Configurações corrigida (engrenagem/modal) |
| 3 | Lançamentos manuais | RN-24 | ✅ concluído em 2026-08-28 — reaproveitado `LancamentoPlanejado`; botões Despesa/Receita conferidos |
| 4 | Planilha viva — leitura | RN-24.1, RN-24.2 | ✅ concluída em 2026-08-28 |
| 5 | Diário override + fill | RN-25, RN-25.1 | ✅ concluída — edição e preenchimento em série funcionando |
| 6 | Importação assistida | RN-27, RN-27.1–27.3 | ✅ concluída — proposta, detecção, fallback e confirmação |
| 7 | Crédito × débito | RN-18 | ✅ concluída — classificação e selos na planilha |
| 8 | Simulador de decisão | RN-23, RN-23.1–23.3 | ✅ concluída — simulação sem persistência e confirmação |
| 9 | Classificação por papel na vida | RN-29 | ✅ concluída — derivação coberta por testes |
| 10 | Gráfico comparativo | RN-30 | ✅ concluída em 2026-09-01 — escala, tooltips e referências atual/bom/ideal/ruim |
| 11 | Dízimo e ofertas | RN-28, RN-28.1 | ✅ concluída — autorização progressiva em Configurações |
| 12 | Relatórios consolidados | — | ✅ concluída em 2026-09-01 — planejamento, cartões e rollup anual reunidos |

---

## Achados da Fase 0 que mudam as fases seguintes

Ao acessar o repositório real (`C:\workspace-pessoal\termometro`), descobri que o código está mais
adiantado do que o `ROADMAP.md` do projeto sugeria:

- **`LancamentoPlanejado`** (módulo `lancamentoplanejado`) já é, na prática, o RN-24 (célula do dia)
  especificado do zero no adendo v1 — só que mais maduro: DESPESA/RECEITA/TRANSFERENCIA, ciclo de vida
  PENDENTE→LIQUIDADO/CANCELADO, aprende categoria ao liquidar, liga com `ContaManual` e `Cartao` manual.
  **A Fase 3/4 deste checklist precisa ser revisada para reaproveitar isso, não recriar.**
- **`ContaManual`** já é o "saldo de carteira" que a spec v2 previa ter que criar do zero pós-Pluggy.
- **O frontend já tem as 4 abas** (`dashboard`, `lancamentos`, `cartoes`, `relatorios`) — mais adiantado
  que a Fase 2 (RN-26) presumia. A tela "cartoes" hoje é onde vive a importação de fatura — decidir se
  ela vira seção de Relatórios (RN-26) ou se fica como está.
- **O motor de verba diária (RN-19/20)** já existe no módulo `orcamento`, como já era esperado.

Antes de atacar as Fases 3+ deste checklist, vale uma sessão dedicada só pra atualizar as specs
(`ESPEC-planilha-viva.md` e `ESPEC-v2-nova-arquitetura.md`) contra o que já existe, em vez de seguir o
plano original às cegas.

---

## Fase 0 — Remoções

**Objetivo:** tirar tudo que a nova filosofia (Pluggy fora, planilha como núcleo) torna morto ou órfão,
antes de construir em cima de uma base suja.

### Backend

- [ ] Remover módulo `ingestao` — adapter Pluggy inteiro (`PluggySincronizador`, cliente HTTP, DTOs de
      `Transaction`/`Account`/`Item` da Pluggy)
- [ ] Remover `SincronizacaoApplicationService` e `SincronizacaoAutomaticaScheduler` (os 4 horários fixos)
- [ ] Remover config `pluggy.item-ids` / `PLUGGY_ITEM_IDS` e qualquer client id/secret em variável de
      ambiente
- [ ] Remover a flag `PLUGGY_BILL` de `Cartao.origemFatura` — domínio passa a só conhecer `MANUAL`
- [ ] Migração nova: ajustar `check constraint` de `transacao.origem` para `MANUAL|ARQUIVO_CSV|ARQUIVO_PDF`
      (remove `OPEN_FINANCE`)
- [ ] Remover qualquer tabela/coluna exclusiva de Pluggy sem uso fora dele (`conexao`, `item_id_externo`,
      `consentimento_expira_em`) — migração de drop, não só de código
- [ ] Remover job de alerta de expiração de consentimento (Pluggy-specific)
- [ ] Apagar qualquer protótipo de RN-06 (Efeito Choque) e RN-13 (Padrões temporais) — nunca foram pra
      produção, mas checar branches/spikes soltos
- [ ] Em RN-10 (comparativo), remover a geração de frase narrativa — manter só o cálculo de
      `snapshot_mensal`/`snapshot_categoria` que vira o rollup anual
- [ ] Confirmar que RN-14 por categoria nunca foi implementado (era `⬜` no roadmap) — se sobrou algo,
      remover; mantém só a faixa global de RN-16
- [ ] Em RN-12 (fila de não identificados), remover geração de "cartão de contexto" caso algo tenha sido
      prototipado (co-ocorrência, sugestões ranqueadas) — mantém classificação por regra + histórico

### Frontend

- [ ] Remover rota/aba "Cartões" da navegação atual
- [ ] Remover fluxo de conexão com Pluggy (botão "conectar banco", widget/callback)
- [ ] Remover indicador de "última sincronização"
- [ ] Remover do dashboard atual (Visão Geral antiga) os blocos: Contas a receber, Minhas contas, Triagem
      por categoria, Plano de ajuste, Saldo de sobrevivência, Vampiros, Dívidas ativas, Fatura declarada à
      mão — **não descartar o componente**, só desconectar da tela; eles voltam na Fase 12 dentro de
      Relatórios

### Verificação (não fecha a fase sem isso)

- [ ] `grep -ri pluggy` no backend inteiro → zero ocorrência fora de referência histórica em `.md`
- [ ] `grep -ri pluggy` no frontend inteiro → zero ocorrência
- [ ] Suíte de testes rodada — testes órfãos de Pluggy **removidos**, não ignorados (`@Disabled` não conta
      como removido)
- [ ] `mvn test` local limpo
- [ ] `npm run build` e `npm run lint` limpos
- [ ] Migração de drop aplicada e testada num banco local antes de ir pra frente

---

## Fase 1 — Padrão visual

**Objetivo:** uma única fonte de verdade de design, aplicada nas telas que sobraram antes de construir as
novas — hoje cada tela tem um padrão diferente, isso não pode se propagar pras próximas fases.

- [ ] Extrair os tokens do mockup da planilha viva (paleta ink/paper, semáforo vermelho/laranja/amarelo/
      verde-claro/verde, tipografia serif+mono+sans) como arquivo único de variáveis
- [ ] Criar o arquivo de tokens compartilhado no frontend (mesmo nome de variável em todo lugar — sem
      cada tela reinventar sua própria cor de "sucesso")
- [ ] Definir os componentes-base reutilizáveis: card, pill de status, tabela densa, badge de origem
      (era banco/manual, agora é `manual`/`arquivo`)
- [ ] Aplicar o padrão nas telas que sobreviveram da Fase 0 (o que resta do dashboard, Configurações) —
      só visual, zero mudança de comportamento nesta fase
- [ ] Revisão de consistência entre as 4 abas antes de seguir — checklist rápido: mesma fonte, mesmo
      espaçamento, mesmo tom de cor por estado (verde/amarelo/laranja/vermelho) em qualquer lugar do app

---

## Fase 2 — Nova navegação (RN-26)

**Objetivo:** as 4 abas existindo como casca, mesmo vazias, para as fases seguintes terem onde entrar.

- [ ] Frontend: estrutura de navegação com 4 abas — Visão Geral, Lançamentos, Planilha, Relatórios
- [ ] Visão Geral: monta o esqueleto das 6 seções (Acesso rápido, Mês atual, Categorias, Forma de
      pagamento, Compras por cartão, Gráfico comparativo) — conteúdo real vem nas fases seguintes
- [ ] Lançamentos: esqueleto com os dois botões (Despesa/Receita) — liga na Fase 3
- [ ] Confirma que nenhuma tela antiga ficou acessível fora dessas 4 abas

---

## Fase 3 — Lançamentos manuais (RN-24) ✅ concluída

**Objetivo:** a porta de entrada de dado voltar a existir — sem isso, nada mais tem o que mostrar.

- [x] Reaproveitar `LancamentoPlanejado` (migração V13 e repositório já existentes), em vez de criar uma
      segunda tabela `lancamento_manual`. Ao liquidar, ele materializa uma `Transacao` de origem `MANUAL`.
- [x] Edição, cancelamento/reabertura e remoção de lançamentos manuais já são cobertos pelo módulo.
- [x] Formulário de Despesa (vermelho) e Receita (verde) na aba Lançamentos.
- [x] Testes do serviço cobrem a materialização de despesa/receita manual; teste da tela cobre a seleção
      explícita de cada tipo. A soma na célula será validada junto da API da planilha na Fase 4.
- [x] Critério de pronto: os lançamentos manuais ficam consultáveis e entram na análise financeira ao
      serem liquidados.

---

## Fase 4 — Planilha viva: leitura e edição (RN-24.1, RN-24.2) ✅ concluída

**Objetivo:** a segunda tela existir de fato, lendo o que a Fase 3 grava.

- [x] Testes: cenários de soma banco/manual, cascata, observação e edição segura de lançamentos
- [x] Backend: `GET /v1/planilha?competencia=`, cascata de saldo entre dias e entre meses
- [x] Backend: `observacao_dia` (nota sem lançamento)
- [x] Frontend: grade horizontal com os meses lado a lado, células coloridas (saldo cheio, saída em
      heatmap), tooltip de observação, drawer de composição do dia
- [x] Inclusão, edição e remoção direta de lançamentos manuais; importados permanecem somente leitura
- [x] Critério de pronto: os lançamentos da Fase 3 aparecem na planilha, e o saldo cascateia corretamente
      de um mês para o outro

---

## Fase 5 — Diário override + preenchimento em série (RN-25, RN-25.1) ✅ concluída

**Objetivo:** a interação de arrastar e preencher, validada no mockup, virando real.

- [x] Testes: cenários de RN-25 e RN-25.1 (override, intervalo não cruza mês)
- [x] Backend: `diario_override`, `PUT /v1/planilha/diario-serie`
- [x] Frontend: célula editável + alça de preenchimento (fill handle) por mês
- [x] Critério de pronto: arrastar um valor de Diário recalcula o saldo em cascata até dezembro, sem
      cruzar mês

---

## Fase 6 — Importação assistida de fatura (RN-27, RN-27.1–27.3) ✅ concluída

**Objetivo:** a segunda fonte de dado (fatura mensal) reativando os leitores do Anexo C.

- [x] Testes: cenários de detecção e leitura de Itaú/Nubank/PicPay
- [x] Backend: `POST /v1/importacoes/propor` — detecção por conteúdo, reconciliação (RN-02.1)
- [x] Backend: confirmação explícita — só então persiste e vincula ao cartão escolhido
- [x] Backend: `Cartao.diaFechamento` (revisão da decisão anterior) + gatilho de lembrete no scheduler de
      alertas (RN-22 reaproveitado)
- [x] Frontend: botão único "Importar", tela de conferência antes de confirmar
      (banco detectado, competência, total reconciliado, amostra de transações)
- [x] Fallback: seleção manual de banco quando confiança for baixa
- [x] Critério de pronto: subir um PDF real do Itaú, ver a proposta batendo com o total impresso, confirmar
      e ver as transações na planilha

---

## Fase 7 — Crédito × débito (RN-18) ✅ concluída

**Objetivo:** o selo de uso de crédito nas transações vindas de fatura.

- [x] Testes: os 3 cenários (déficit disfarçado, ferramenta, atenção)
- [x] Backend: `usoDeCredito` calculado sobre transações de cartão, cruzando Sinal 1 (Pix) e Sinal 2
      (verba diária do dia)
- [x] Frontend: selo visível na composição do dia (drawer) e agregado por mês
- [x] Critério de pronto: importar uma fatura com um Pix parcelado e ver a classificação correta aparecer

---

## Fase 8 — Simulador de decisão sob déficit (RN-23, RN-23.1–23.3) ✅ concluída

**Objetivo:** "e se eu pagar isso no crédito agora" sem tocar em dado real.

- [x] Testes: cenários de simulação e confirmação
- [x] Backend: `POST /v1/planilha/simular-decisao` (não persiste) e `.../confirmar` (materializa)
- [x] Backend: bloco de priorização quando `SaldoSobrevivencia(m) < 0`, reaproveitando RN-15 e RN-09
- [x] Frontend: tela de comparação lado a lado (real × simulado) com o alerta de atraso de quitação
- [x] Critério de pronto: simular uma compra parcelada, ver o atraso de quitação projetado, confirmar e ver
      virar compromisso futuro real

---

## Fase 9 — Classificação por papel na vida (RN-29) ✅ concluída

**Objetivo:** a base de dado que o gráfico comparativo (Fase 10) precisa.

- [x] Backend: mapeamento de `PapelNaVida` a partir de etiqueta (RN-05) + `categoria.natureza`/`grupo`
      já existentes — sem nova entrada de categorização manual
- [x] Testes: cada combinação de etiqueta/natureza cai no grupo certo
- [x] Critério de pronto: consultar qualquer transação já triada e ver o `papelNaVida` derivado corretamente

---

## Fase 10 — Gráfico comparativo (RN-30) ✅ concluída

**Objetivo:** o gráfico de pontos em Visão Geral.

- [x] Backend: `GET /v1/visao-geral/comparativo-categorias`, usando referências calibradas
      em RN-16 onde existirem
- [x] Frontend: gráfico de pontos (categoria × % da renda, atual/bom/ideal/ruim), régua inferior e tooltips
- [x] Sessão de calibração com o Felipe: revisar categoria por categoria antes de fechar os valores de
      referência — **não fechar esta fase sem essa conversa**
- [x] Critério de pronto: o gráfico reflete dado real do mês corrente, sem número inventado

---

## Fase 11 — Dízimo e ofertas (RN-28, RN-28.1) ✅ concluída

**Objetivo:** a meta de contribuição com autorização progressiva.

- [x] Alinhar com o Felipe a definição exata de `colchao_minimo` antes de codar (proposta em spec:
      reaproveitar `MinimoVariavel` de RN-08 — precisa confirmação)
- [x] Testes: os 3 cenários (sem espaço, espaço surge, confirmação altera projeção)
- [x] Backend: `meta_contribuicao`, algoritmo de autorização cruzando RN-08/RN-09
- [x] Backend: `GET /v1/metas-contribuicao`, `POST .../autorizar-proximo-passo`
- [x] Frontend: seção em Configurações + indicador do próximo passo
      sugerido, quando houver
- [x] Critério de pronto: a projeção mostra o efeito da contribuição atual, e o sistema propõe o próximo
      incremento só quando a projeção mostrar espaço de verdade

---

## Fase 12 — Relatórios consolidados ✅ concluída

**Objetivo:** os blocos tirados da Visão Geral na Fase 0 ganham lar definitivo.

- [x] Reconectar (não recriar) os componentes desligados na Fase 0: Contas a receber, Minhas contas,
      Triagem por categoria, Plano de ajuste, Saldo de sobrevivência, Vampiros, Dívidas ativas
- [x] Mover a seção "Cartões" (detalhe por cartão) para dentro de Relatórios
- [x] Ligar o clique em "Forma de pagamento" (Visão Geral) para abrir direto na seção Cartões de Relatórios
- [x] Adicionar o rollup anual (RN-10 reduzida, estilo aba Economia) como seção nova
- [x] Aplicar o padrão visual da Fase 1 em tudo que for reconectado aqui — é a última chance de sobrar
      tela fora do padrão
- [x] Critério de pronto: nenhuma informação da Visão Geral antiga foi perdida, só reorganizada

---

## Fase 13 — Valores reais, faturas e rastreabilidade mensal ✅ concluída

**Objetivo:** substituir números opacos por lançamentos reais do mês e permitir acompanhar e pagar
faturas sem duplicar consumo no Comparativo.

- [x] “Mês atual” usa receitas menos despesas da competência, separado da cascata da Planilha
- [x] Custo fixo e Piso humano são clicáveis e abrem os lançamentos que formam cada total
- [x] Comparativo prioriza lançamentos manuais/importados, mantém catálogo apenas como fallback e
      detalha os itens de cada grupo
- [x] Categorias do relatório são clicáveis e exibem os valores específicos incluídos
- [x] Faturas têm histórico por competência, detalhe, estado aberta/parcial/paga e pagamento parcial
- [x] Pagamento de fatura gera saída na Planilha e é `NAO_E_GASTO` para não inflar o Comparativo
- [x] Cartões importados refletem os imports; valores manuais podem ser ajustados por competência
- [x] Logotipos de bancos/cartões exportados do arquivo Figma e armazenados localmente, com fallback
- [x] Inclusão/edição direta na Planilha permite marcar despesa comum, recorrente ou Piso humano
- [x] Receita manual pode ser marcada como recorrente tanto no lançamento rápido quanto na Planilha
- [x] Saldo positivo ou negativo é carregado continuamente para o mês seguinte, sem zerar a cascata
- [x] Testes novos de regressão + `npm run build` + `mvn clean test` no último passo
- [x] Commit e push da nova versão no repositório pessoal `Felipendev`

### Correção pós-Fase 13 ✅

- [x] Planilha abre e calcula usando as entradas e saídas existentes, sem exigir saldo manual
- [x] Saldo manual é uma conciliação opcional e não bloqueia o próprio mês nem meses anteriores
- [x] Cascata automática validada sem âncora e com âncora dentro do mês consultado

### Revisão responsiva e continuidade do saldo ✅

- [x] Navegação principal legível em 320 px, celular, tablet e desktop
- [x] Cartões e faturas sem sobreposição; modais limitados à altura visível
- [x] Abas de Relatórios sempre acessíveis e tabela anual legível sem corte lateral
- [x] Comparativo com régua e pontos legíveis em telas estreitas
- [x] Planilha mostra um mês no celular/tablet e dois no desktop, sem rolagem horizontal
- [x] Saldo do mês usa `entrada - saída - diário` e o fechamento inicia o mês seguinte
- [x] Cenário `10 → 8 → 6 → 4 → 2 → 10.000` coberto por teste automatizado
- [x] Validado em dados reais: setembro fecha em R$ 1.005,38 e outubro começa em R$ 1.005,38
- [x] `npm run build` e suíte completa com 483 testes aprovados
