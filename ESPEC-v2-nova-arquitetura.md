# Nova Arquitetura, Fim do Pluggy, Dízimo e Gráfico Comparativo — Adendo SDD v2.0

**Autor:** Felipe (com Claude) · **Data:** 2026-08-28 · **Status:** em execução — Fases 0–3 do checklist concluídas; RN-24 reaproveita `LancamentoPlanejado` já existente.

> Adendo a `ESPEC-termometro-financeiro.md` e a `ESPEC-planilha-viva.md` (adendo v1.0, RN-18 a RN-25).
> Continua a numeração de onde parou: RN-26 em diante. Não reabre nada já implementado nas fatias
> 1–13 exceto o que está explicitamente marcado como **revisado** abaixo.

---

## 0. A mudança que muda tudo: sem Pluggy

**Decisão:** o sistema deixa de contar com sincronização bancária automática. As duas fontes de dado
passam a ser **lançamento manual** (RN-24, já especificada) e **importação de fatura assistida** (RN-27,
nova, abaixo) — uma vez por mês, quando cada fatura fecha.

Isso não é um ajuste de escopo pequeno — mata um módulo inteiro e revive outro:

| O que morre | O que revive |
|---|---|
| Módulo `ingestao`, adapter Pluggy inteiro (fatia 12 do roadmap original) | Os leitores de PDF/CSV do **Anexo C** — que o roadmap já tinha marcado como "superado na prática pelo Pluggy" voltam a ser o caminho principal, não plano B |
| `SincronizacaoAutomaticaScheduler` (RN-22, sync 4×/dia) e os dois gatilhos de alerta que dependiam dele ("verba baixa", "transação alta") | Os mesmos dois gatilhos, mas disparados no momento do lançamento manual ou da confirmação de importação — mesmo `DecisorDeAlerta`, ponto de disparo diferente |
| `origem` da transação incluindo `OPEN_FINANCE` | `origem` vira `MANUAL \| ARQUIVO_CSV \| ARQUIVO_PDF` |
| Aba/tab "Cartões" como tela própria | Vira seção dentro de **Relatórios** + um bloco "Compras por cartão" em **Visão Geral** |
| `Cartao.origemFatura` com a flag reservada `PLUGGY_BILL` (nunca alcançável) | Flag removida — só existe `MANUAL` agora, deixa de ser flag e vira o único caminho |

**O que não muda:** RN-02 (dedupe), RN-02.1 (reconciliação contra o total impresso), RN-04 (compromissos
futuros de parcela) e toda a leitura de PDF do Anexo C continuam valendo exatamente como estavam
especificadas — só passam a ser acionadas por upload manual em vez de sync.

---

## 1. RN-26 — Arquitetura de navegação por domínio

Quatro abas, cada uma dona de uma responsabilidade, sem sobreposição:

| Aba | Responde | Não faz |
|---|---|---|
| **Visão Geral** | "O que está acontecendo agora?" — leitura rápida, sem profundidade | Não edita nada além de compras por cartão; não mostra dívida, viabilidade, plano de ajuste |
| **Lançamentos** | "Registrar uma entrada ou saída" | Não mostra histórico agregado nem projeção — é só a porta de escrita |
| **Planilha** | "A grade viva, célula a célula" (RN-24/25) | Não é onde você lança pela primeira vez — reflete o que Lançamentos e Importação já gravaram |
| **Relatórios** | "Profundidade — diagnóstico, dívida, projeção, estratégia" | Não é ação rápida — é leitura de apoio à decisão |

### Conteúdo de Visão Geral (revisado a partir do que foi listado como removido)

Removido de Visão Geral: Contas a receber, Minhas contas, Triagem por categoria, Plano de ajuste, Saldo
de sobrevivência, Vampiros, Dívidas ativas, Cartões (como bloco de detalhe), Fatura declarada à mão.
Todos migram para **Relatórios**, que passa a ser a única aba de profundidade — sem duplicar leitura em
dois lugares.

Fica em Visão Geral:

1. **Acesso rápido** — botão Importar (RN-27) + atalho para novo Lançamento
2. **Mês atual** — resumo do dia e do mês corrente (mesmo dado de `SaldoSobrevivencia`, mas sem o rótulo
   "Saldo de Sobrevivência" nem a profundidade de diagnóstico — só o número)
3. **Categorias** — gasto do mês por categoria
4. **Forma de pagamento** — cartões cadastrados, clicáveis; clicar leva para a seção Cartões dentro de
   **Relatórios**
5. **Compras por cartão no mês** — as transações da fatura já identificadas, com data, hora (quando a
   fonte trouxer) e categoria; editável inline para descrever compras sem detalhe suficiente
6. **Gráfico comparativo** (RN-30, novo) — atual × bom × ideal, abaixo de Categorias/Forma de pagamento

### Lançamentos

Dois botões — **Despesa** (vermelho) / **Receita** (verde) — abrindo o mesmo formulário curto de
`lancamento_manual` (RN-24) já especificado, com o `tipo` pré-selecionado pelo botão clicado. Nenhuma
regra de negócio nova aqui — é a porta de entrada da RN-24 ganhando sua própria aba, porque hoje ela só
existe embutida na composição da planilha.

---

## 2. RN-27 — Importação de fatura com detecção assistida

Um botão só ("Importar"), sem escolha manual de banco. O fluxo tem duas etapas — **proposta, depois
confirmação** — porque você pediu para o sistema adivinhar e mostrar antes de gravar, não decidir sozinho.

### RN-27.1 — Proposta (não persiste nada)

```
POST /v1/importacoes/propor
multipart: arquivo
```

Detecção por conteúdo, reaproveitando as assinaturas já mapeadas no Anexo C:

| Sinal | Banco detectado |
|---|---|
| Cabeçalho CSV `date,title,amount` | Nubank |
| PDF com tabela de duas colunas e cabeçalho `DATA` no x0 característico | Itaú |
| PDF com bloco `Picpay Card` | PicPay |

```java
public record PropostaImportacaoResponse(
        UUID propostaId,                       // token efêmero, mesmo espírito do RN-23.2
        String bancoDetectado,                 // ITAU | NUBANK | PICPAY | DESCONHECIDO
        Confianca confianca,                   // ALTA | MEDIA | BAIXA
        YearMonth competenciaDetectada,
        Dinheiro totalReconciliado,
        boolean reconciliacaoFechou,           // RN-02.1
        int transacoesEncontradas,
        List<TransacaoPropostaResponse> amostra,   // 5 primeiras, para conferência visual
        List<CartaoResponse> cartoesCandidatos) {} // se houver mais de um cartão do banco detectado

public enum Confianca { ALTA, MEDIA, BAIXA }
```

Se `bancoDetectado = DESCONHECIDO` ou `confianca = BAIXA`, a resposta inclui a lista de bancos suportados
para escolha manual — **fallback, não caminho padrão**.

### RN-27.2 — Confirmação

```
POST /v1/importacoes/{propostaId}/confirmar
{ "cartaoId": "..." }   -- obrigatório só se houver mais de um cartão candidato
```

Só aqui as `transacao` são de fato gravadas, vinculadas ao `Cartao` manual escolhido, com RN-02 (dedupe),
RN-02.1 (reconciliação) e RN-04 (compromissos futuros de parcela) rodando exatamente como já especificado.
Sem confirmação, nada é persistido — mesmo padrão do simulador de decisão (RN-23.1/23.2).

### RN-27.3 — Lembrete de fechamento

Como o `Cartao` manual não guardava dia de fechamento (decisão de escopo tomada quando essa feature ainda
era stopgap do spike do Pluggy — ver roadmap, extensão de 2026-08-24), essa decisão é **revisada agora**:
`Cartao` ganha `diaFechamento` (int, 1–31, opcional). Quando presente, alimenta um gatilho no scheduler de
alertas já existente (RN-22): 1 dia antes do fechamento, notifica "sua fatura do Itaú fecha amanhã, hora
de importar" — mesmo canal (Telegram) já implementado.

---

## 3. RN-28 — Meta de contribuição com autorização progressiva (dízimo e ofertas)

**Não é a rampa geométrica de RN-15.** RN-15 corta um gasto existente até um piso, num horizonte fixo.
Aqui é o oposto: **aumentar** um compromisso de doação até uma meta, e o ritmo não é um calendário — é a
projeção quem autoriza o próximo passo, mês a mês, conforme sobra espaço no fluxo de caixa.

```sql
meta_contribuicao(
    id uuid pk, usuario_id uuid fk,
    nome text check (nome in ('DIZIMO','OFERTA')),
    percentual_alvo numeric(5,4) not null,       -- 0.10 = 10% da renda bruta, por categoria
    percentual_atual numeric(5,4) not null default 0,
    passo_incremento numeric(5,4) not null default 0.02,
    base_calculo text default 'RENDA_BRUTA',
    unique(usuario_id, nome)
)
```

### RN-28.1 — Algoritmo de autorização

```
para cada meta_contribuicao com percentual_atual < percentual_alvo:
    disponivel(m+1) = SaldoSobrevivencia(m+1) projetado [RN-08 aplicado sobre RN-09],
                       já contando o percentual_atual como saída comprometida
    se disponivel(m+1) > colchao_minimo E percentual_atual < percentual_alvo:
        proximoPercentual = min(percentual_atual + passo_incremento, percentual_alvo)
        propõe o incremento — não efetiva sozinho (P1: sistema mede, não decide)
    senão:
        mantém percentual_atual, sem propor
```

`colchao_minimo` é a mesma margem de segurança que já orienta RN-08 (não inventar um número novo) —
proponho reaproveitar `MinimoVariavel` como piso, sujeito a validação quando formos codar.

Uma vez autorizado e confirmado por você, o novo `percentual_atual` passa a entrar como saída comprometida
em `SaldoSobrevivencia` e no `MotorDeProjecao` dos meses seguintes — o mesmo efeito cascata de um
`compromisso_futuro`, só que revisado mês a mês em vez de fixo desde o início.

```java
public record MetaContribuicaoResponse(
        UUID id, String nome,
        Percentual percentualAtual, Percentual percentualAlvo,
        Dinheiro valorMensalAtual,
        ProximoPassoResponse proximoPassoSugerido) {}   // null se não há espaço este mês

public record ProximoPassoResponse(
        YearMonth competencia, Percentual percentualProposto,
        Dinheiro valorProposto, Dinheiro disponivelProjetado) {}
```

```
GET  /v1/metas-contribuicao
POST /v1/metas-contribuicao/{id}/autorizar-proximo-passo
```

**Etiqueta:** contribuição é VERDE (RN-05) — não é gasto para fins de corte, é destino de renda, mesma
categoria de amortização de dívida e aporte em reserva já prevista na spec original.

### Gherkin

```gherkin
Funcionalidade: Autorização progressiva de contribuição

  Cenário: Sem espaço este mês, mantém o percentual atual
    Dado que o percentual atual de DIZIMO é 2%
    E a projeção do próximo mês mostra disponível abaixo do colchão mínimo
    Quando eu consultar as metas de contribuição
    Então o próximo passo sugerido é nulo
    E o percentual atual permanece 2%

  Cenário: Espaço surge daqui a dois meses, sistema propõe o incremento
    Dado que o percentual atual de DIZIMO é 2% e o alvo é 10%
    E a projeção mostra disponível acima do colchão mínimo daqui a 2 meses
    Quando eu consultar as metas de contribuição
    Então o próximo passo sugerido aponta a competência daqui a 2 meses
    E o percentual proposto é 4%

  Cenário: Confirmar o passo altera a saída comprometida dali em diante
    Dado um próximo passo sugerido de 2% para 4% em 2026-11
    Quando eu autorizar esse passo
    Então a projeção de novembro em diante passa a considerar 4% como saída comprometida
    E o percentual atual da meta é atualizado para 4%
```

---

## 4. RN-29 — Classificação por papel na vida

Pré-requisito do gráfico comparativo (RN-30). **Não é uma reclassificação manual nova** — é uma leitura
sobre o que RN-05 (triagem 4 cores) e `categoria.natureza`/`categoria.grupo` já produzem, evitando duplicar
trabalho de categorização que você já faz.

| Grupo | Derivado de |
|---|---|
| `ESSENCIAL` | AZUL + `natureza = FIXO` |
| `IMPORTANTE_AJUSTAVEL` | AZUL + `natureza = VARIAVEL` (dentro do piso, mas flexível) |
| `QUALIDADE_DE_VIDA` | subconjunto de `IMPORTANTE_AJUSTAVEL` cuja `categoria.grupo` é lazer/restaurante/hobby |
| `REDUTIVEL` | AMARELA |
| `EVITAVEL` | VERMELHA |
| `DIVIDA_COMPROMISSO` | VERDE (dívida, reserva, e agora também `meta_contribuicao`) |

```java
public enum PapelNaVida { ESSENCIAL, IMPORTANTE_AJUSTAVEL, QUALIDADE_DE_VIDA, REDUTIVEL, EVITAVEL, DIVIDA_COMPROMISSO }
```

---

## 5. RN-30 — Gráfico comparativo (atual × bom × ideal)

**Fonte dos números de referência, decidida com você:** o método 50/30/20 como moldura geral, mais os
benchmarks que a spec original já declarava (RN-16: moradia ≤30%, transporte ≤15%, serviço da dívida
≤30%) onde já existem. Nada inventado além disso — onde não houver referência pública nem benchmark já
declarado, o gráfico não mostra um ponto "ideal" fabricado, mostra só "atual" até você definir um alvo.

```
Essencial + Dívida/Compromisso        → teto de referência: 50% da renda líquida
Importante-ajustável + Qualidade de vida → teto de referência: 30%
Redutível + Evitável                  → sem teto "bom" — ideal tende a 0%
Dízimo/Oferta                          → não entra no 50/30/20; usa o alvo declarado em RN-28 (10%+10%)
```

```java
public record PontoComparativoResponse(
        String categoria, PapelNaVida papel,
        Percentual atual, Percentual bom, Percentual ideal) {}

public record ComparativoCategoriasResponse(
        YearMonth competencia, List<PontoComparativoResponse> pontos) {}
```

```
GET /v1/visao-geral/comparativo-categorias?competencia=
```

Renderizado como gráfico de pontos (categoria no eixo vertical, % da renda no eixo horizontal, um ponto
por cenário) — mesmo formato do exemplo que você trouxe.

**Nota de calibração:** os limiares exatos por categoria individual (quanto é "bom" gastar em streaming,
por exemplo) não estão nesta spec porque dependem das categorias reais que você tem cadastradas hoje —
isso entra na conversa de implementação, categoria por categoria, não como número fixo aqui.

---

## 6. Regras removidas ou reduzidas nesta rodada (consolidação)

Retomando a auditoria anterior, com o que muda pela saída do Pluggy:

| RN | Situação |
|---|---|
| RN-06 (Efeito Choque) | Removida — nunca implementada, tradução verbal do que a planilha já mostra |
| RN-13 (Padrões temporais) | Removida — mesma razão |
| RN-10 (narrativa) | Removida; mantém só o rollup anual (estilo aba Economia) dentro de Relatórios |
| RN-14 por categoria | Removida; mantém só a faixa global (RN-16) |
| RN-12 (fila com cartão de contexto rico) | Reduzida — sem Pluggy trazendo hora/co-ocorrência em tempo real, a fila mostra a transação crua da fatura importada; classificação por regra + histórico de estabelecimento continua |
| RN-01/02 (fonte `OPEN_FINANCE`) | Revisada — fontes agora são `MANUAL`, `ARQUIVO_CSV`, `ARQUIVO_PDF` |
| Módulo `ingestao` (adapter Pluggy) | Removido |
| Aba "Cartões" | Removida como tela própria; vira seção de Relatórios + bloco em Visão Geral |
| `SincronizacaoAutomaticaScheduler` | Removido; gatilhos de alerta migram para o momento do lançamento/importação |

---

## 7. Impacto no roadmap

| # | Fatia | RN |
|---|---|---|
| 18 | Reorganização de navegação em 4 abas (front) | RN-26 |
| 19 | Importação assistida (proposta + confirmação), substituindo o adapter Pluggy | RN-27, RN-27.1, RN-27.2 |
| 20 | Lembrete de fechamento de fatura | RN-27.3 |
| 21 | Meta de contribuição com autorização progressiva | RN-28, RN-28.1 |
| 22 | Classificação por papel na vida | RN-29 |
| 23 | Gráfico comparativo em Visão Geral | RN-30 |
| — | Remoção do módulo `ingestao` (Pluggy) e do `SincronizacaoAutomaticaScheduler` | — |

**Ordem sugerida:** 19 primeiro (sem ela não há dado nenhum entrando no sistema) → 18 em paralelo (é só
front, não bloqueia nem é bloqueada) → 29 antes de 23 (o gráfico depende da classificação) → 20 e 21 por
último, são incrementais e não bloqueiam o resto.

Fatias 14–17 (planilha viva, diário override, crédito×débito, simulador — adendo v1.0) continuam válidas
sem alteração; a única dependência nova é que RN-24 (célula do dia) agora soma `lancamento_manual` **e**
`transacao` vindas de importação de fatura, não mais de sync — o contrato de leitura (`GET /v1/planilha`)
não muda, só a origem dos dados que ele agrega.
