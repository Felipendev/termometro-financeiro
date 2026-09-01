# Roadmap — onde estamos, para onde vamos

Duas camadas, na ordem em que foram atacadas:

1. **Automação** (MVP, `MVP.md`) — tirar o lançamento manual do caminho. **100% feito.**
2. **Inteligência** (as 13 fatias da seção 10 do `ESPEC-termometro-financeiro.md`) — as perguntas de
   viabilidade, projeção, plano de ajuste. Hoje respondidas na planilha `termometro-felipe.xlsx`;
   ainda não estão no código.

O MVP reordenou a sequência original da spec para priorizar "o sistema não exige digitação" acima
de "o sistema pensa por você" — por isso M0-M6 não é 1-a-1 com as fatias 1-13. Este documento junta
as duas visões num status único.

## Camada 1 — Automação (`MVP.md`, M0-M6) ✅ completa

| Fatia da spec | O que entrou | RN | Estado |
|---|---|---|---|
| 1 | `Dinheiro`, `Competencia`, `Percentual` | — | ✅ |
| 2a | Kernel de ingestão + Nubank CSV + dedupe | RN-01, RN-02, RN-02.1 | ✅ |
| 10a (parcial) | Motor da verba diária, provisão | RN-19, RN-20 | ✅ |
| — | Persistência (Postgres + Flyway + JPA) | RN-02 | ✅ |
| 12 | Adapter Pluggy (sync Itaú/Nubank/PicPay) | RN-01 | ✅ |
| 3 (parcial) | Categorização por regra + fila de não identificados | RN-12 | ✅ |
| 8 (parcial) | Fila de não identificados (RN-13 padrões ainda não) | RN-12 | ✅ |
| 10a + 12 (parcial) | `GET /hoje` + notificação matinal via Telegram | RN-19, RN-22 (só o gatilho matinal) | ✅ |

## Camada 2 — Inteligência financeira — pendente, respondida hoje na planilha

Ordem original da spec (seção 10), com status real do código (não da planilha):

| # | Fatia | Entregável | RN | Estado no código |
|---|---|---|---|---|
| 2b | Leitores PDF Itaú/PicPay + `POST /importacoes` | reconciliação nas 3 faturas | RN-01, RN-02.1 | ⬜ — só protótipo Python de verificação; **superado na prática pelo Pluggy** (fatia 12), vira plano B se algum banco cair do Open Finance |
| 3 | RN-03: motor automático de "não é gasto" (pagamento de fatura, transferência, estorno casados por valor+data) | `POST /v1/nao-gasto/{competencia}` | RN-03 | ✅ **feito em 2026-08-21** — módulo `naogasto`. Achado no caminho: bug real no `PluggySincronizador` (toda conta virava `SecaoFatura.CARTAO`, inclusive corrente) — corrigido. Detalhe abaixo |
| **3** | **RN-05: triagem 4 cores + piso humano (azul/amarela/vermelha/verde)** | `POST /v1/triagem/{competencia}`, `GET /v1/triagem/{competencia}/resumo`, `POST /v1/triagem/transacoes/{id}/promover-vermelha` | RN-05 | ✅ **feito em 2026-08-21** — módulo `triagem`, novo. `MotorDeTriagem` decide a etiqueta de cada transação já classificada (RN-12): FIXO sempre AZUL, NAO_E_GASTO sempre VERDE, VARIAVEL sem piso vira NAO_TRIADA, VARIAVEL com piso passa pelo `AlgoritmoDoPiso` (ordena por data, acumula, cruza o piso = AMARELA). Bate com os 2 cenários Gherkin da spec, inclusive o split lógico da transação-fronteira (R$ 160 azul / R$ 80 amarelo numa categoria de R$ 240) — resolvido calculando o resumo por categoria sob demanda (como `/viabilidade` e `/diagnostico`, não persistido), nunca só somando a etiqueta gravada, que perderia essa divisão. VERMELHA só existe por promoção manual a partir de uma transação hoje AMARELA (400 se tentar promover AZUL/VERDE/já-VERMELHA), e nunca é sobrescrita pelo algoritmo automático. Fora do escopo: cascata de auto-sugestão para categoria sem piso (regra do usuário → histórico do estabelecimento → `categoria.etiqueta_padrao` — hoje cai direto em NAO_TRIADA), `POST /transacoes/triagem-lote` com regra permanente, e a fila `GET /transacoes/pendentes-triagem` com cartão de contexto |
| 7 | Compromissos futuros formais via parcela do cartão | `POST /v1/compromissos-futuros/gerar` | RN-04 | ✅ **feito em 2026-08-22** — módulo `compromissofuturo`, novo. Detalhe abaixo |
| 4 | Saldo de sobrevivência | `GET /diagnostico` | RN-08 | ✅ **feito em 2026-08-21** — `SaldoDeSobrevivencia` soma custo fixo + piso + dívidas (novo: `Divida` no catálogo, empréstimo Nubank seedado) + compromissos futuros via parcelas reais já ingeridas. RN-08.1 (ponte de caixa) e o semáforo **por área** da RN-14 ficaram de fora — o semáforo depende de metas por categoria e de dado real fluindo pelo Pluggy, que ainda não está ligado; construir agora arriscava mostrar "tudo ótimo" sem dado nenhum por trás |
| **5** | **Viabilidade + queda de renda** — *a pergunta central* | `GET /viabilidade` | RN-16, RN-16.1 | ✅ — `catalogo` (renda/custo fixo/piso humano, seedado com as Premissas da planilha) + `diagnostico` (`TesteDeViabilidade`). RN-17 (fila de lacunas automatizada) ficou de fora — a resposta já veio pronta da planilha |
| 9 | Motor de projeção (quando quito) | `GET /v1/projecao?competencia=&estrategia=&horizonteMeses=` | RN-09 | ✅ **feito em 2026-08-21** — `MotorDeProjecao` (módulo `projecao`) simula mês a mês avalanche/bola-de-neve/proporcional; bate byte a byte com os 3 números do Anexo B (8 meses ambas estratégias, juros R$ 1.191,48 vs R$ 1.517,22, busca binária R$ 1.245,46/mês). Composto com `catalogo`: renda constante (PJ fixo, sem variação), custo fixo + parcela do empréstimo Nubank como `saida_fixa`, piso humano como `saida_var` (default da spec — ainda não há metas por categoria), e um novo `divida_rotativa` (R$ 7.952,24 a 6,36% a.m. medido) como a única dívida que a estratégia de amortização de fato decide algo sobre. RN-09.1 (comparador de crédito) e RN-09.2 (rolagem entre cartões) ficaram de fora — dependem de mais de uma linha de crédito real cadastrada, que ainda não existe |
| 10 | Plano de ajuste progressivo + migração para débito | `GET /v1/plano-ajuste?competencia=&mesesRampa=&fatorMaxCortePercentual=` | RN-15, RN-18 | ✅ **feito em 2026-08-21 (RN-15)** — módulo `planoajuste`, `MotorDoPlanoDeAjuste` bate exato com os 3 cenários Gherkin (1.240→320 alonga p/ 4 meses a 28,7%/mês, 1.000→100 alonga p/ 6 meses a 31,9%/mês, 310→180 sem alongar a 16,6%/mês) e o vermelho zera no mês 1. RN-18 (migração p/ débito) fora do escopo — detalhe abaixo |
| 10a | Reserva de emergência em níveis | `GET /v1/reserva` | RN-21 | ✅ **feito em 2026-08-22** — módulo `reserva`, novo. `CalculadoraDeNiveisDeReserva` varre a simulação de `projecao` (AVALANCHE, 60 meses) e marca a primeira competência em que a reserva acumulada cruza cada um dos 3 níveis (1×/3×/6× o custo mensal = custo fixo do catálogo + verba variável real do orçamento, **sem dívida** — diferente de `CustoMinimoVida` da RN-16 e de `Marcos.reservaCompleta` da RN-09). Nível 0 (ponte de caixa via RN-08.1) ficou de fora, mesmo motivo da fatia 4. Limitação conhecida e documentada: como o sistema não guarda quanto Felipe já tem guardado fora do app (decisão dele: assumir R$ 0 em vez de criar um campo manual novo), a simulação sempre recomeça do zero a cada consulta — `atingido` praticamente só fica `true` quando o próprio mês corrente já cruza o alvo |
| 6 | Detector de vampiros (assinaturas silenciosas) | `GET /v1/vampiros?competencia=` | RN-07 | ✅ **feito em 2026-08-21** — módulo `vampiros`, `DetectorDeRecorrencias` agrupa despesas dos últimos 6 meses por `Normalizador.chaveDeEstabelecimento` e classifica periodicidade (MENSAL 26-35 dias, ANUAL 350-380 dias) por mediana de intervalo. Bate com o Anexo B: assinatura R$ 42,40/mês → custo anual R$ 508,80. `confianca` é heurística própria (a spec não fecha a fórmula) — 40% nº de ocorrências, 30% regularidade do intervalo, 30% estabilidade do valor, validada nos dois cenários Gherkin da RN-07 e no número do Anexo B antes de virar código. Detecta reajuste (degrau monotônico no valor, ex.: R$ 40→R$ 50) sem quebrar a recorrência, e marca `cobrancaSilenciosa` para valores <R$ 50/mês. Fora do escopo: periodicidade SEMANAL (spec não dá faixa de dias), persistência de decisão (`PATCH /vampiros/{id}`) e o sinal "sem decisão há 6 meses" — hoje a lista é recalculada a cada chamada, sem estado |
| 8 | Padrões temporais de gasto | `GET /padroes` | RN-13 | ⬜ |
| 11 | Comparativo temporal + snapshots | `GET /comparativo` | RN-06, RN-10 | ⬜ |
| 12 | Alertas completos (verba baixa, evento próximo, marco, transação>R$100) | 4 gatilhos além do matinal | RN-22 | ✅ **feito em 2026-08-22** — sync automático novo (`SincronizacaoAutomaticaScheduler`, 8h/12h/16h/20h, fuso Fortaleza, lista de conexões via `pluggy.item-ids`) alimenta os dois gatilhos que dependem de dado novo (verba baixa, transação>R$100), avaliados dentro do próprio `SincronizacaoApplicationService.sincroniza` — manual ou automático. Marco atingido e evento próximo dependem só da data, então rodam num scheduler diário próprio (7h05). Nova tabela `estado_de_alerta` (chave-valor) deduplica os 3 gatilhos que podem repetir o mesmo aviso (transação alta não precisa: já é deduplicada pelo hash da RN-02). Detalhe abaixo |
| 13 | Dashboard dos Três Eus (front) + cadastro manual do catálogo | `GET /v1/dashboard/tres-eus?competencia=`, `PUT`/`DELETE /v1/catalogo/*` | RN-11 | ✅ **feito em 2026-08-24** — módulo `dashboard` novo (só `application`, sem `domain`/`infra` — pura composição sobre portas existentes, sem regra de negócio nova) expõe o agregado das 3 colunas + veredito de viabilidade num único endpoint. `frontend/` novo (React + Vite + TypeScript), tela Dashboard (leitura) + tela Configurações (cadastro). CORS não existia no projeto — `CorsConfig` novo (`app.cors.allowed-origins`). Extensão do mesmo dia: `catalogo` ganhou escrita (5 `PUT` upsert por chave + 3 `DELETE`, cobrindo renda/custo-fixo/piso-humano/dívidas/dívidas-rotativas) e a tela Configurações, pedido do Felipe pra não depender mais de mim rodando SQL a cada mudança de dado (fluxo tipo Organizze). RN-11 (simulador de compra) fora do escopo — depende de `cenario` persistido, que ainda não existe (mesma lacuna de `ProjecaoAPI`/`PlanoAjusteAPI`). Detalhe abaixo. Segunda extensão do mesmo dia: visão **Cartões** (`GET /v1/cartoes?competencia=`) — o gasto real por cartão de crédito na competência, direto das transações já sincronizadas (não depende de classificação/triagem/não-gasto terem rodado). Achado no caminho: `ContaBancaria` (nome/tipo/limite/saldo, que a Pluggy já devolve em `GET /accounts`) nunca era persistida — era montada e descartada a cada sync, só sobrevivia o `identificador` dentro de cada transação. Nova tabela `conta` (V10) + `ContaRepository` fecham essa lacuna com upsert por `identificador` a cada sync. `CartaoResponse` é propositalmente **não-aditivo**: soma bruta das transações da seção CARTAO, nunca somada em outro lugar do dashboard (parte do custo fixo já é paga pelo próprio cartão — somar duplicaria). Frontend: novo bloco "Cartões" na coluna Eu do Presente (nome/gasto/limite/% do limite/total) + dois botões manuais no cabeçalho do dashboard ("Rodar não-gasto"/"Rodar triagem") — Felipe não tinha como disparar `POST /v1/nao-gasto/{competencia}` e `POST /v1/triagem/{competencia}` sem curl. Ainda em aberto: o total de R$ 4.082,55 negativo que o Felipe calculou manualmente (contas fixas + faturas de cartão de setembro − renda de R$ 10.000) só vira projeção automática (RN-18) depois de um spike no endpoint `bills` da Pluggy (nunca chamado até hoje — só `/accounts` e `/transactions`), pra saber se dá pra buscar fechamento/vencimento/total declarado de fatura direto da instituição em vez do Felipe digitar `saldoDevedor` manualmente em Dívidas Rotativas. |

**Quanto falta:** 2 fatias não iniciadas (8, 11) + a fatia 2b, superada na prática pelo
Pluggy, de um total de 14 — fatias 3 (RN-05 e RN-03), 4, 5, 6, 9, 10 (RN-15), 7 (RN-04), 10a
(RN-21), 12 (RN-22) e agora 13 (RN-11, dashboard) saíram da lista desde 2026-08-21. A camada de
automação está fechada, as duas perguntas centrais ("dá pra guardar a meta?" e "quando eu quito?")
também, mais o detector de assinaturas silenciosas, a triagem 4 cores, o plano de ajuste progressivo
com priorização por dor, o motor de não-gasto, os compromissos futuros de compra parcelada, a
reserva de emergência em níveis, os alertas proativos completos e agora o dashboard consolidado.
Só ficam de fora as duas fatias que dependem de mais meses de histórico real (8, 11) — o app já
está pronto pra ser usado no dia a dia no lugar da planilha.

**Próximo passo recomendado:** as três perguntas centrais da spec ("pra onde vai o dinheiro?", "dá
pra guardar a meta?", "quando eu quito?"), a rampa de corte (RN-15), o motor de não-gasto (RN-03),
os compromissos futuros (RN-04), a reserva de emergência em níveis (RN-21), os alertas proativos
completos (RN-22) e agora o dashboard (RN-11) já estão respondidos pelo código. O que resta —
fatias 8 (padrões temporais) e 11 (comparativo temporal) — depende de meses de histórico real
fluindo pelo Pluggy que ainda não existem; ficam para quando esse volume acontecer.

**Próximo passo real (pedido do Felipe em 2026-08-24):** RN-18 (migração gradual pro débito) — mas reformulada: a necessidade de verdade não é a migração em si, é o sistema entender a saúde financeira real (hoje quase 100% no crédito) e projetar "quando eu saio do vermelho", igual a planilha fazia. `GET /v1/projecao` já calcula isso a partir de `divida_rotativa.saldoDevedor` (editável em Configurações) — falta (1) o spike no `bills` da Pluggy pra saber se dá pra puxar total/fechamento/vencimento de fatura automaticamente em vez de digitar o saldo à mão, e (2) separar juros/tarifa de cartão do gasto principal (ex.: R$ 500 de contador virando R$ 517,67 na fatura por sair no crédito) pra a projeção mostrar quanto sobra sem o custo do crédito. Sequência combinada: Cartões (feito) → spike `bills` → RN-18/projeção do "quando saio do vermelho".

> **Pendente de acesso externo (2026-08-24):** o spike do endpoint `bills` da Pluggy está deliberadamente adiado até que a conta receba a liberação necessária. Nenhuma parte do dashboard, das análises locais ou da projeção manual depende dessa liberação; enquanto isso, a fatura e o saldo rotativo permanecem editáveis em Configurações.

---

## O que entrou em 2026-08-24 (extensão) — cadastro manual de cartão

**Módulo `cartao`, novo** — `GET/PUT/DELETE /v1/cartoes/manuais`. Pedido do Felipe: poder cadastrar
um cartão e digitar o valor da fatura à mão, até o spike do endpoint `bills` da Pluggy (próximo
passo real, ver acima) permitir puxar isso automático.

**Decisão de modelagem (alinhada com o Felipe antes de codar):** uma entidade `Cartao` só, não uma
extensão de `ContaBancaria`/`conta` (V10) — aquela tabela é 100% automática, reescrita por inteiro a
cada sync (RN-01); misturar edição manual nos mesmos campos seria apagado sem aviso no sync
seguinte. `Cartao.identificadorContaPluggy` é só um id solto de correlação com `conta.identificador`
— sem FK, sem verificação, sem merge automático com o gasto real de `GET /v1/cartoes`. Um campo
`origemFatura` (`MANUAL`/`PLUGGY_BILL`) fica no domínio como flag reservada: hoje só `MANUAL` é
alcançável (o request nunca aceita origem do cliente, o serviço força sempre), `PLUGGY_BILL` fica
pronto pro dia em que o spike de `bills` entrar.

**Sem competência** — mesmo espírito de `DividaRotativa.saldoDevedor`: o valor da fatura é o estado
atual, editado quando a fatura fecha, não uma série mensal histórica. Evita normalizar um modelo de
"ciclo de fatura" completo (fechamento/vencimento/mínimo) pra um cadastro que é stopgap e vai ser
substituído.

**`DELETE` é soft delete de verdade** — pedido explícito do Felipe, diferente do resto do catálogo:
`CustoFixoItem` não tem `DELETE` (só `PUT` com `ativo: false`), `Divida`/`DividaRotativa` são hard
delete. Aqui existe um verbo `DELETE` dedicado, mas por baixo ele só marca `ativo = false` — a linha
nunca é apagada (`CartaoInfraRepository.remove`, testado em `CartaoInfraRepositoryIT` verificando
direto na tabela que a linha sobrevive). Reaproveita o mesmo find-or-create/upsert por id de
`Divida`/`CustoFixoItem` para criar/editar; `Persistable`/checagem de existência não foi necessária
porque `save()` do Spring Data já faz `merge()` (insere se a linha não existe, atualiza se existe).

**Dashboard:** `EuDoPresenteResponse` ganhou `cartoesManuais` (lido direto de `CartaoRepository`,
não de `CartaoService`/`ResumoCartoesResponse` — os dois convivem sem se misturar, é o front quem
decide como mostrar os dois lado a lado). Bloco novo em Eu do Presente, abaixo dos cartões
sincronizados. Frontend: `CartoesSection` nova em Configurações (mesmo padrão de
`DividasSection`/`CustoFixoSection` — formulário inline, editar, excluir).

Migration nova: `V11__cartao_manual.sql`.

**Não verificado neste ambiente:** este trabalho foi feito numa sessão sem Maven Central acessível
(rede restrita) — `mvn test`/`mvn verify` não rodaram aqui. `npm run build` e `npm run lint` do
frontend rodaram e ficaram limpos (só os avisos pré-existentes de `setState`-em-effect). Rode
`mvn test` localmente antes de commitar.
