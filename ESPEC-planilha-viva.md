# Planilha Viva, Crédito×Débito e Decisão sob Déficit — Adendo à Especificação (SDD v1.0)

**Autor:** Felipe (com Claude) · **Data:** 2026-08-28 · **Status:** rascunho para alinhamento — nada implementado ainda

> Este documento é um **adendo** a `ESPEC-termometro-financeiro.md`. Não reabre RNs já implementadas
> (RN-01 a RN-16, RN-19 a RN-22) — estende o sistema com o que a Camada 2 ainda não cobria: a segunda
> tela do produto (planilha viva, navegável mês a mês, editável célula a célula) e duas perguntas que a
> spec original deixava em aberto — RN-18 (crédito como ferramenta vs. déficit disfarçado) e a decisão
> sob déficit (simulação de "e se eu pagar isso no crédito agora").
>
> Numeração: RN-18 já estava reservada no roadmap ("migração pro débito"). RN-23, RN-24, RN-25 são novas.
> RN-11 (bloqueio do Eu do Futuro) é **estendida**, não recriada — o dashboard já existe, falta o
> `POST /simular-compra` que a spec original já previa e nunca foi codado.

---

## 0. Contexto

O produto passa a ter três telas, não mais um dashboard único:

1. **Cartões & visão geral** — o dashboard Três Eus que já existe.
2. **Planilha** (esta spec) — a mesma estrutura `Data / Entrada / Saída / Diário / Saldo` da planilha
   Excel, meses lado a lado, com scroll horizontal, saldo em cascata entre meses, e cada célula editável
   célula a célula.
3. **Relatórios** — reorganização visual do que diagnóstico/viabilidade/projeção/plano-ajuste já entregam.
   Fora do escopo deste documento.

**Premissa de produto (a mesma do resto do sistema, P1 e P3 da spec original):** a planilha não é um
resumo do sistema, é o **sistema**. Simulador de cenário formal, semáforo por categoria e parte da
"inteligência" verbal da spec original deixam de ser obrigatórios como funcionalidade separada, porque
editar uma célula e ver a curva de saldo reagir em cascata já responde a pergunta central — "quando vou
ter grana sobrando" — sem precisar de um motor de linguagem por cima.

---

## 1. RN-24 — Célula do dia (lançamento manual avulso)

**Decisão tomada com o Felipe:** entidade nova, **não** estende `transacao`. Os mesmos motivos já
documentados para o cadastro manual de cartão se aplicam aqui: `transacao` carrega invariantes de sync
(RN-01 normalização de sinal, RN-02 dedupe por hash, `versao` para optimistic locking do Pluggy) que não
fazem sentido para um lançamento que nasce na tela e nunca vem de sincronização. Misturar as duas coisas
arrisca um lançamento manual ser tratado como duplicata de sync ou sumir num futuro reprocessamento.

```sql
lancamento_manual(
    id uuid pk, usuario_id uuid fk,
    data date not null,
    tipo text check (tipo in ('ENTRADA','SAIDA')),
    descricao text not null,
    valor numeric(14,2) not null,       -- sempre positivo; o sinal vem de `tipo`, nunca do valor
    observacao text null,
    categoria_id uuid null fk,          -- opcional; permite cruzar com triagem/piso no futuro, não obrigatório agora
    ativo boolean default true,         -- soft delete, mesmo padrão de Cartao
    criado_em timestamptz, atualizado_em timestamptz
)
-- índice: (usuario_id, data) where ativo
```

**A célula não é uma tabela — é uma composição.** `GET /v1/planilha?competencia=` (RN-24.1, abaixo)
soma, para cada dia: transações reais já sincronizadas (`TransacaoRepository`, filtrando `ignorada=false`,
todas as contas) **mais** `lancamento_manual` ativos daquele dia. O valor exibido na célula é sempre essa
soma — nunca um número persistido separadamente. Isso segue o mesmo princípio de `/diagnostico` e
`/viabilidade`: computado sob demanda, nunca um estado que pode dessincronizar do que realmente aconteceu.

**Editar ou apagar um lançamento manual é local ao dia** — decisão já validada com o Felipe: não gera
regra de aprendizado, não reclassifica nada em outro lugar. Diferente do `regra_categorizacao` que nasce
de uma decisão de triagem (RN-05), aqui a edição é pontual por natureza.

### RN-24.1 — Contrato de leitura da planilha

```
GET /v1/planilha?competencia=2026-09
```

```java
public record DiaDaPlanilhaResponse(
        LocalDate data, DayOfWeek diaDaSemana,
        Dinheiro entrada, Dinheiro saida,
        Dinheiro diario, boolean diarioSobrescrito,      // RN-25
        Dinheiro saldo, FaixaSaude faixaSaldo,           // reaproveita RN-14
        List<LancamentoResponse> lancamentos,
        String observacao) {}                            // RN-24.2

public record LancamentoResponse(
        UUID id, String origem,          // "BANCO" | "MANUAL"
        String descricao, Dinheiro valor, String tipo,   // ENTRADA | SAIDA
        OffsetDateTime registradoEm, String observacao) {}

public record PlanilhaMesResponse(
        YearMonth competencia,
        List<DiaDaPlanilhaResponse> dias,
        Dinheiro totalEntrada, Dinheiro totalSaida, Dinheiro totalDiario,
        Dinheiro saldoFinal) {}
```

`saldo` de cada dia é a cascata: `saldo(d) = saldo(d-1) + entrada(d) - saida(d) - diario(d)`, com o
primeiro dia da série pegando o `saldo` do último dia do mês anterior (ou um saldo inicial configurável,
se não houver mês anterior calculado). **Isso é o ponto central da tela** — sem essa cascata, a planilha
vira uma foto de mês isolado, exatamente o que a P5 da spec original já alertava para não fazer.

### RN-24.2 — Observação do dia

Uma observação de texto livre por dia, editável, exibida como tooltip na célula (não como coluna). Pode
ser anexada a um dia mesmo sem lançamento nenhum (ex.: "hoje" ou uma nota de contexto). Persistida junto
de qualquer `lancamento_manual` do dia, ou como registro próprio se o dia não tiver lançamento manual:

```sql
-- reaproveita lancamento_manual.observacao quando há lançamento;
-- para dia sem lançamento manual mas com nota, uma tabela mínima:
observacao_dia(usuario_id uuid fk, data date, texto text, primary key(usuario_id, data))
```

---

## 2. RN-25 — Diário como override por dia

**Decisão tomada com o Felipe:** o motor de verba diária (RN-19/20, já implementado) continua rodando e
sugerindo um valor por dia. Arrastar e digitar na planilha cria um **override editável**, que convive com
o cálculo automático — nunca desliga o motor, só sobrepõe o resultado dele naquele dia específico, e pode
ser apagado a qualquer momento pra voltar ao valor automático.

```sql
diario_override(
    usuario_id uuid fk, data date not null,
    valor numeric(14,2) not null,
    criado_em timestamptz, atualizado_em timestamptz,
    primary key (usuario_id, data)
)
```

**Leitura (RN-24.1):** `diario(d) = diario_override(d)` se existir, senão o valor do motor RN-19/20 pra
aquele dia. `diarioSobrescrito = true` no DTO sempre que houver override — é o que acende o indicador
visual (o `✎`/realce que já está no mockup).

### RN-25.1 — Preenchimento em série ("fill" estilo Excel)

```
PUT /v1/planilha/diario-serie
{ "de": "2026-09-15", "ate": "2026-09-30", "valor": "70.00" }
```

Cria/atualiza um `diario_override` pra cada dia do intervalo com o mesmo valor. **Restrição: o intervalo
não cruza mês** — a UI já trava isso arrastando só dentro do bloco do mês (mockup já implementa), mas o
backend valida de novo (`de` e `ate` no mesmo `YearMonth`), porque a spec nunca deve confiar só na
validação de front. Resposta: a lista de dias afetados, pra UI atualizar sem novo `GET`.

**Efeito cascata:** qualquer mudança em `diario_override` deve invalidar o saldo calculado a partir
daquele dia até o fim do horizonte pedido — como é sempre computado sob demanda (RN-24.1), não existe
"recalcular e persistir", só o próximo `GET /v1/planilha` já vem certo. Nenhuma migração de estado
necessária além da tabela acima.

---

## 3. RN-18 — Crédito como ferramenta vs. déficit disfarçado

**Critério descartado:** "fatura paga integral + saldo de sobrevivência do mês positivo" — testado com o
Felipe e rejeitado por ser binário demais: ignora tanto o caso "usei crédito por escolha, mesmo tendo
caixa" quanto "o mês fechou positivo, mas o dia da compra já estava sufocado".

**Critério adotado — dois sinais independentes, por transação de cartão:**

**Sinal 1 — Pix no crédito.** Regra fundamental do Felipe: *"crédito nunca deve ser tratado como renda"*.
Pix financiado pelo limite do cartão (comum em Nubank/PicPay, aparece na fatura como um lançamento tipo
"Pix Parcelado" ou com "PIX" na descrição) é, quase sempre, dinheiro que deveria estar disponível na hora.
Detecção: `descricao_normalizada` contém `"PIX"` **e** a transação pertence a conta `CARTAO_CREDITO`.
Cuidado de implementação (o mesmo já documentado no Anexo C para não confundir com pagamento de fatura):
casar por substring exata `"PIX"`, nunca por prefixo livre que colida com outra coisa.

**Sinal 2 — Caixa do dia já negativo.** Em vez de olhar o mês fechado, reaproveita o motor de verba diária
(RN-19/20) já implementado: se o **disponível projetado daquele dia**, calculado antes de considerar a
transação de cartão em questão, já estava `≤ 0`, a compra está entrando num caixa que já não tinha folga.

```
para cada transação t de conta CARTAO_CREDITO:
    sinal1 = "PIX" em t.descricao_normalizada
    sinal2 = verba_diaria_disponivel(t.data, antes_de(t)) <= 0

    classificacao(t) =
        DEFICIT_DISFARCADO   se sinal1 E sinal2
        ATENCAO              se sinal1 OU sinal2 (exatamente um dos dois)
        FERRAMENTA           se nem sinal1 nem sinal2
```

Três níveis, não dois — é a diferença entre "sinaliza mas não acusa" (`ATENCAO`) e "isso é sintoma"
(`DEFICIT_DISFARCADO`). Nunca decide por você (P1 da spec original) — só rotula a transação; a leitura
final é sempre sua.

**Onde aparece:** como um selo na transação (`GET /v1/transacoes`, campo novo `usoDeCredito`) e agregado
por mês em `/v1/planilha` (`Dinheiro totalDeficitDisfarcado`, `int transacoesEmAtencao`) — não é uma tela
nova, é uma leitura sobre dado que já existe.

```java
public enum UsoDeCredito { FERRAMENTA, ATENCAO, DEFICIT_DISFARCADO }

public record UsoDeCreditoResponse(
        UsoDeCredito classificacao,
        boolean pixNoCredito,
        boolean caixaDoDiaNegativo,
        Dinheiro verbaDiariaNoMomento) {}
```

**Fora do escopo desta fatia:** tendência do rotativo ao longo de vários meses (Sinal 3, descartado nesta
rodada) e checagem contra piso/meta da categoria (Sinal 4, descartado). Ficam documentados aqui como
próximo refinamento natural se os dois sinais atuais gerarem falso positivo demais na prática.

---

## 4. RN-23 — Decisão sob déficit (extensão de RN-11)

A spec original já previa `POST /cenarios/{id}/simular-compra` (RN-11) e nunca foi implementado — o
roadmap cita isso três vezes como lacuna (`ProjecaoAPI`, `PlanoAjusteAPI`, `TriagemAPI`), sempre pela
mesma razão: falta o conceito de `cenario` persistido. Esta fatia resolve isso **sem** persistir cenário
como entidade — mesma filosofia de todo o resto do sistema (computa sob demanda, só materializa o que o
usuário confirma).

### RN-23.1 — Simulação (não persiste nada)

```
POST /v1/planilha/simular-decisao
{
  "data": "2026-09-14",
  "valor": "500.00",
  "descricao": "Curso de formação",
  "formaPagamento": "CREDITO_PARCELADO",   -- DEBITO | CREDITO_AVISTA | CREDITO_PARCELADO
  "parcelas": 5
}
```

```java
public record SimulacaoDecisaoResponse(
        PlanilhaMesResponse cenarioReal,        // projeção atual, sem a decisão
        PlanilhaMesResponse cenarioSimulado,    // com a decisão aplicada
        List<DeltaMesResponse> diferencaPorMes,
        Integer atrasoQuitacaoEmMeses,          // reaproveita MotorDeProjecao (RN-09)
        UsoDeCreditoResponse usoDeCreditoPrevisto,  // RN-18 aplicada à decisão simulada
        String alerta) {}                        // ex.: "Isso empurra outubro pro vermelho"

public record DeltaMesResponse(YearMonth competencia, Dinheiro diferencaSaldo) {}
```

Mecanicamente: clona a série de dias já calculada por `/v1/planilha` no horizonte pedido, aplica a
decisão como um `lancamento_manual` **em memória** (nunca grava), recalcula a cascata e roda de novo o
`MotorDeProjecao` (RN-09) com essa mudança pra saber se atrasa a quitação. Reaproveita RN-18 pra já
avisar se a forma de pagamento escolhida cairia em `ATENCAO`/`DEFICIT_DISFARCADO`.

### RN-23.2 — Confirmação (aí sim persiste)

```
POST /v1/planilha/simular-decisao/{simulacaoId}/confirmar
```

Só quando confirmado, vira de fato um `lancamento_manual` (se `DEBITO`/`CREDITO_AVISTA`) ou uma série de
`compromisso_futuro` (se `CREDITO_PARCELADO`, mesma mecânica de RN-04 já implementada). Sem confirmação
explícita, a simulação nunca toca em dado real — é exatamente o que o Felipe pediu: *"comparar situação
real × cenário simulado, só incorporar depois que eu confirmar"*.

**Nota de escopo:** como não existe estado de "simulação em andamento" persistido, `simulacaoId` é
efêmero — um token que carrega o payload da simulação original (assinado ou serializado), não uma linha
de banco. Evita criar uma tabela `cenario` só pra isso, que era exatamente a armadilha que travou RN-11
até agora.

### RN-23.3 — Priorização quando o mês não fecha

Quando `/v1/planilha` mostra `SaldoSobrevivencia(m) < 0` pra algum mês (RN-08 já calcula isso), a resposta
de `/v1/planilha` inclui um bloco de priorização **reaproveitando** o que já existe, sem nova lógica de
ranqueamento:

```java
public record PriorizacaoDeficitResponse(
        List<AcaoPrioritariaResponse> cortesSugeridos,   // reaproveita PlanoAjusteService (RN-15)
        EstrategiaAmortizacaoResponse amortizacaoSugerida, // reaproveita MotorDeProjecao avalanche (RN-09)
        Dinheiro rendaExtraNecessaria) {}                 // RN-08, já existe
```

Não é uma RN nova de negócio — é composição do que RN-08, RN-09 e RN-15 já respondem, exposta no momento
exato em que a planilha mostra o mês no vermelho, em vez de exigir que o usuário vá caçar isso em três
telas diferentes.

---

## 5. Edge cases (a lista que vira teste)

1. Lançamento manual num dia que depois recebe uma transação sincronizada equivalente (ex.: você lançou
   "Farmácia R$47,70" manualmente, e semanas depois o Pluggy sincroniza a mesma compra) → **não há
   dedupe automático entre `lancamento_manual` e `transacao`** — são fontes independentes por design
   (RN-24). Dupla contagem é um risco real aqui; fica como aviso visual (`possívelDuplicata: true`) por
   similaridade de valor+data+descrição, nunca merge automático.
2. `diario_override` num dia que já passou (mês fechado) → permitido (o Felipe foi explícito: nada é
   imutável), mas recalcula a cascata de saldo pra frente, inclusive em meses já "fechados" visualmente.
3. `PUT /diario-serie` com intervalo cruzando mês → 422, mensagem apontando o corte exato.
4. Sinal 2 (RN-18) num dia sem dado de verba diária calculado ainda (mês muito futuro, fora do horizonte
   do motor RN-19/20) → `caixaDoDiaNegativo` vem `null`, classificação cai pra `ATENCAO` no máximo (nunca
   `DEFICIT_DISFARCADO` sem os dois sinais confirmados).
5. Simulação de decisão (RN-23.1) com parcelamento que ultrapassa o horizonte de 60 meses da projeção →
   trunca no horizonte e avisa, não estoura erro.
6. Observação de dia (RN-24.2) em dia sem nenhum lançamento → permitido, existe só a nota.
7. Apagar o único `lancamento_manual` de um dia que também tinha a `observacao_dia` vinculada a ele →
   observação migra para o registro solto de `observacao_dia`, não se perde.
8. Dois `diario_override` de fill-serie sobrepostos (usuário arrasta duas vezes, intervalos se cruzam) →
   último `PUT` vence, sem acumular.

---

## 6. Critérios de aceite (Gherkin)

```gherkin
Funcionalidade: Célula do dia como soma de lançamentos

  Cenário: Total do dia é a soma de banco e manual
    Dado que existe uma transação sincronizada de R$ 186,40 em 04/09
    E um lançamento manual de R$ 47,70 em 04/09 com observação "Farmácia — remédio contínuo"
    Quando eu consultar a planilha de 2026-09
    Então o total de saída do dia 04 é R$ 234,10
    E o dia tem 2 lançamentos na composição

Funcionalidade: Diário como override

  Cenário: Override substitui o cálculo automático só naquele dia
    Dado que o motor de verba diária calculou R$ 45,00 para o dia 15/09
    Quando eu definir um override de R$ 70,00 para 15/09
    Então a planilha mostra R$ 70,00 no Diário do dia 15
    E o campo diarioSobrescrito é verdadeiro
    E o dia 16/09 continua usando o valor automático do motor

  Cenário: Preenchimento em série não cruza mês
    Quando eu tentar preencher o Diário de 25/09 até 05/10 com R$ 70,00
    Então a API rejeita com 422
    E a mensagem aponta que o intervalo cruza a virada de mês

Funcionalidade: Crédito como ferramenta vs. déficit disfarçado

  Cenário: Pix no crédito com caixa do dia negativo é déficit disfarçado
    Dado que a verba diária disponível em 12/09 já estava em R$ -30,00 antes da transação
    E uma transação de R$ 500,00 com descrição "PIX Contador" na conta CARTAO_CREDITO
    Quando eu consultar o uso de crédito dessa transação
    Então a classificação é "DEFICIT_DISFARCADO"

  Cenário: Compra parcelada sem Pix e com caixa positivo é ferramenta
    Dado que a verba diária disponível no dia da compra era R$ 120,00
    E uma transação parcelada em 5x sem "PIX" na descrição
    Quando eu consultar o uso de crédito dessa transação
    Então a classificação é "FERRAMENTA"

  Cenário: Só um dos sinais gera atenção, não déficit disfarçado
    Dado que a verba diária disponível no dia da compra era R$ 200,00
    E uma transação com "PIX" na descrição na conta CARTAO_CREDITO
    Quando eu consultar o uso de crédito dessa transação
    Então a classificação é "ATENCAO"

Funcionalidade: Simulação de decisão sob déficit

  Cenário: Simular não altera dados reais
    Dado um cenário real com quitação projetada para 2027-11
    Quando eu simular uma compra de R$ 500,00 em 5x a partir de 2026-09-14
    Então a planilha real permanece inalterada
    E a resposta traz o cenário simulado lado a lado com o real
    E nenhum lançamento é persistido até a confirmação

  Cenário: Confirmar a simulação materializa a decisão
    Dado uma simulação prévia de compra parcelada em 5x
    Quando eu confirmar essa simulação
    Então são criados 5 compromissos futuros, um por parcela
    E a planilha passa a refletir a decisão sem nova simulação
```

---

## 7. Impacto no roadmap

Três fatias novas, encaixando na Camada 2 existente, sem reabrir nenhuma fatia já fechada:

| # | Fatia | RN | Depende de |
|---|---|---|---|
| 14 | Planilha viva — leitura (`GET /v1/planilha`) + lançamento manual + observação de dia | RN-24, RN-24.1, RN-24.2 | RN-08 (saldo), RN-14 (faixa), motor de verba diária (RN-19/20) — todos já prontos |
| 15 | Diário override + preenchimento em série | RN-25, RN-25.1 | Fatia 14 |
| 16 | Crédito×débito (selo por transação) | RN-18 | Motor de verba diária (RN-19/20), já pronto |
| 17 | Simulador de decisão + priorização sob déficit | RN-23, RN-23.1, RN-23.2, RN-23.3 | Fatia 14, RN-09 (projeção), RN-15 (plano de ajuste) — todos já prontos |

Nenhuma migração toca em `transacao` ou em tabelas de sync — só tabelas novas (`lancamento_manual`,
`diario_override`, `observacao_dia`). Frontend: a segunda tela (planilha) é trabalho novo de UI; a
terceira tela (relatórios) é reorganização visual do que já existe, fora do escopo deste documento.

**Ordem sugerida:** 14 antes de tudo (é a base de leitura que as outras três consomem) → 15 e 16 podem
andar em paralelo, são independentes entre si → 17 por último, porque depende da leitura da planilha
(14) já estar estável para clonar o cenário em memória.
