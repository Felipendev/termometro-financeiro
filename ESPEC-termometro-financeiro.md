# Termômetro Financeiro — Especificação (SDD v2.0)

**Autor:** Felipe · **Data:** 2026-08-20 · **Status:** aprovada · fatias 1 e 2a implementadas

> **v2.0** — o objetivo do sistema deixa de ser "entender a dívida" e passa a ser **sair do crédito e
> viver no débito, com reserva**. Cinco regras novas: **RN-18** (migração crédito → débito),
> **RN-19** (orçamento diário — o Termômetro), **RN-20** (provisão para gastos irregulares),
> **RN-21** (reserva de emergência em níveis) e **RN-22** (automação e alertas).
>
> **v1.4** — acrescenta **RN-09.1 (comparador de crédito de curto prazo)** e **RN-09.2 (detecção e
> custo da rolagem entre cartões)**. Nasceram de um fato dos dados: a rolagem que parecia
> descontrole era, medida, a **mais barata** das quatro opções de crédito disponíveis. O sistema
> precisa saber calcular isso antes de sugerir qualquer coisa.
>
> **v1.3** — acrescenta **RN-17 (o sistema pergunta o que falta)**, **RN-17.1 (detector de duplo
> lançamento)** e **RN-08.1 (descasamento de datas)**. As três nasceram do primeiro diagnóstico com
> dados reais: o sistema precisou de informação que só o Felipe tinha, e eu somei o contador duas
> vezes por não ter perguntado antes.
>
> **v1.2** — acrescenta o **Anexo C: formatos reais das faturas**, escrito a partir de 6 arquivos
> verdadeiros (2 faturas Itaú, 3 extratos Nubank, 1 fatura PicPay). Tudo ali foi descoberto lendo os
> arquivos, não a documentação dos bancos — e três descobertas mudaram regras da spec.
>
> **v1.1** — incorpora: plano de ajuste progressivo (RN-15), enriquecimento temporal e fila de não
> identificados (RN-12), detecção de padrões por dia/período/semana (RN-13), semáforo de saúde
> IDEAL/SEGUINDO BEM/RUIM/PÉSSIMO (RN-14) e o **Teste de Viabilidade do Padrão de Vida** (RN-16),
> que responde à pergunta central: *dá para economizar 25–30% ganhando R$ 10k, ou o padrão de vida
> precisa cair?*

---

## 0. Contexto

Felipe, 31 anos (25/set), desenvolvedor PJ. Receita declarada de R$ 10.000/mês bruto — o **líquido real precisa ser apurado** (impostos do Simples/PJ, contador, INSS, pró-labore). Está endividado e quer responder três perguntas, nesta ordem:

1. Para onde vai meu dinheiro, de verdade?
2. Em que mês eu quito a dívida?
3. A partir de quando eu começo a guardar?
4. **Dá para guardar 25–30% da renda com o meu padrão de vida atual, ou o padrão precisa cair?**

### O fato que muda o diagnóstico

Até fevereiro de 2026 a renda era **R$ 14.000/mês**. Hoje é **R$ 10.000** — queda de **28,6%**. As dívidas
e o custo fixo (aluguel incluído) foram dimensionados para a renda antiga, que os pagava "na risca".
Elas não se acumularam por indisciplina: **o denominador mudou e o numerador não**.

Isso reordena o produto. Antes de qualquer sugestão de cortar café, o sistema precisa responder se a
conta fecha estruturalmente (RN-16). Se não fecha, disciplina não resolve — só mexer no fixo resolve.
Se fecha, o problema está no amarelo e no vermelho, e aí sim o plano de ajuste (RN-15) é a resposta.

**Meta declarada:** economizar de **25% a 30%** da renda líquida. É este número que calibra o semáforo
(RN-14) e o veredito de viabilidade (RN-16).

A planilha atual (método Termômetro: `Data / Entrada / Saída / Diário / Saldo` por mês + aba `Economia` com % de economia sobre entradas) já dá clareza sobre **saldo diário**. O que ela não faz — e é o núcleo deste sistema — é: (a) abrir a fatura do cartão transação a transação, (b) separar o que é humano do que é desperdício, e (c) **projetar**.

### Princípios de produto (as decisões que amarram tudo)

| # | Princípio | Consequência técnica |
|---|---|---|
| P1 | O sistema mede, não julga | Toda etiqueta vermelha é decisão do usuário; o motor só sugere |
| P2 | Você é humano | Cada categoria variável tem um **piso** que o sistema jamais assume como zero |
| P3 | A projeção é o produto | O extrato é insumo; o entregável é a data de quitação |
| P4 | Nada entra sem rastro | Toda transação carrega origem, hash e idempotência |
| P5 | Mês parcial nunca compara com mês fechado | Todo comparativo é normalizado por dia decorrido |

---

## 1. Glossário do domínio (linguagem ubíqua)

| Termo | Definição |
|---|---|
| **Conexão** | Vínculo com uma instituição (item do Pluggy) ou fonte de arquivo |
| **Conta** | Conta corrente, poupança ou cartão de crédito |
| **Fatura** | Ciclo fechado de um cartão, com vencimento, total, mínimo e encargos |
| **Transação** | Lançamento individual, seja em conta ou dentro de uma fatura |
| **Compromisso Futuro** | Valor já contratado que cairá num mês futuro (parcela, financiamento, contrato) |
| **Piso Humano** | Mínimo mensal por categoria que o usuário declara não conseguir cortar. Ex.: "comer fora 2x/mês = R$ 160" |
| **Etiqueta de Triagem** | AZUL (indispensável) / AMARELA (reduzível) / VERMELHA (desnecessária) / VERDE (não é gasto) |
| **Saldo de Sobrevivência** | Renda líquida menos tudo que já está comprometido, incluindo o piso variável |
| **Vampiro** | Cobrança recorrente detectada automaticamente (assinatura, mensalidade) |
| **Cenário** | Conjunto de metas por categoria + renda extra + estratégia de amortização |
| **Marco** | Data-alvo: quitação, primeiro real guardado, reserva completa |
| **Baseline** | Média congelada dos 3 meses anteriores ao início do plano, usada em todo comparativo |
| **Grupo de Similaridade** | Conjunto de transações com descrição equivalente; classificar uma classifica todas |
| **Padrão Temporal** | Concentração estatisticamente relevante de gasto num recorte de tempo (dia, período, semana do mês) |
| **Faixa de Saúde** | IDEAL · SEGUINDO_BEM · RUIM · PÉSSIMO, por categoria e global |
| **Plano de Ajuste** | Rampa de redução mês a mês que respeita o piso humano e o limite de corte por mês |
| **Taxa Máxima** | Quanto sobraria gastando exatamente o piso em tudo — o teto teórico de economia |
| **Lacuna** | Dado que falta, está estimado, ambíguo ou contraditório, e que o sistema precisa perguntar |
| **Ponte de Caixa** | Mecanismo usado para cobrir um compromisso que vence antes da renda entrar |
| **Rolagem** | Pagar a fatura de um cartão com outro. Não é despesa; o custo implícito é |
| **Saldo Rolado** | Parte da fatura que não coube na capacidade do mês e foi empurrada adiante |
| **Capital de Giro** | Um mês do que hoje passa no cartão — o que custa migrar tudo para o débito |
| **Verba do Dia** | Quanto sobra hoje, recalculado diariamente a partir do que já foi gasto no mês |
| **Provisão** | Camada da verba reservada para o irregular — dentro do orçamento, nunca em cima |
| **Independência do Crédito** | Primeiro mês com 100% dos gastos no débito |

---

## 2. Escopo

### Dentro do v1
Ingestão (Open Finance + arquivo) · deduplicação · categorização · triagem 3 cores com piso humano ·
**fila de não identificados com contexto temporal** · **detecção de padrões de gasto (dia da semana,
período, semana do mês)** · detector de vampiros · compromissos futuros a partir de parcelas ·
diagnóstico mensal (saldo de sobrevivência + efeito choque) · **semáforo de saúde por área** ·
**teste de viabilidade do padrão de vida** · motor de projeção e quitação · **plano de ajuste
progressivo** · comparativo temporal · dashboard dos Três Eus.

### Fora do v1
Pagamentos/Pix · multiusuário real (single-tenant com auth simples) · carteira de investimentos e rentabilidade · app mobile nativo · categorização por ML (o v1 usa regras + aprendizado por estabelecimento, que é determinístico e testável).

---

## 3. Ingestão de dados — decisão e riscos

**Fonte primária: Meu Pluggy.** Confirmado: o Meu Pluggy é gratuito por tempo indeterminado para pessoa física, entrega Client ID/Secret próprios e acesso à API para conectar as **suas** contas. Cobre os três bancos por Open Finance (Nubank, Itaú, PicPay são participantes). Os planos comerciais (a partir de R$ 2.500/mês) só valem para uso de terceiros — não é o nosso caso.

Recursos da API que interessam: `items` (conexão), `accounts`, `transactions` (list by cursor), `bills` (faturas de cartão), `connectors`, `webhooks`.

Campos relevantes de `Transaction`: `id`, `date`, `description`, `descriptionRaw`, `amount`, `amountInAccountCurrency`, `balance`, `currencyCode`, `type` (DEBIT/CREDIT), `status` (PENDING/POSTED), `providerCode`, `providerId`, `operationType`, e os objetos aninhados `creditCardMetadata` (parcelamento), `paymentData` (contraparte da transferência) e `merchant` (razão social + CNPJ).

### Riscos que a arquitetura precisa absorver

| Risco | Mitigação obrigatória |
|---|---|
| `category`/`categoryId` da Pluggy exigem assinatura Pro | **Categorização é nossa.** Nunca depender do enrichment deles |
| Consentimento Open Finance expira (até 12 meses) | Job diário de alerta 30/15/7 dias antes de `consentimento_expira_em` |
| Fatura em aberto às vezes não traz todas as transações | Importador de PDF/CSV/OFX é **parte do v1**, não um extra |
| `amount` no cartão é positivo para débito (convenção invertida) | Normalização de sinal no adapter, com teste dedicado (RN-01) |
| `status = PENDING` muda de valor/some | Transação pendente entra com flag e é reconciliada no próximo sync |

---

## 4. Modelo de dados (PostgreSQL)

> Convenção de dinheiro: `numeric(14,2)` no banco, `BigDecimal` com `scale=2` e `RoundingMode.HALF_EVEN` no Java, encapsulado no value object `Dinheiro`. **Nunca `double`, nunca `float`.** Nos DTOs REST, valores monetários trafegam como **string** (`"1234.56"`) — number em JSON vira `double` no front e volta com centavo errado.

```sql
-- Identidade e renda -------------------------------------------------------
usuario(id uuid pk, nome text, email citext unique, data_nascimento date, criado_em timestamptz)

renda(id uuid pk, usuario_id uuid fk, competencia date,          -- sempre dia 1
      valor_bruto numeric(14,2), valor_liquido numeric(14,2) not null,
      fonte text, observacao text,
      unique(usuario_id, competencia))

-- Conexões e contas --------------------------------------------------------
conexao(id uuid pk, usuario_id uuid fk,
        provedor text check (provedor in ('PLUGGY','ARQUIVO')),
        item_id_externo text, instituicao text,
        status text, ultimo_sync_em timestamptz, consentimento_expira_em date)

conta(id uuid pk, conexao_id uuid fk, id_externo text,
      tipo text check (tipo in ('CORRENTE','CARTAO_CREDITO','POUPANCA')),
      nome text, numero_mascarado text, moeda char(3) default 'BRL',
      saldo numeric(14,2), limite numeric(14,2), atualizado_em timestamptz,
      unique(conexao_id, id_externo))

fatura(id uuid pk, conta_id uuid fk, competencia date,
       data_fechamento date, data_vencimento date,
       valor_total numeric(14,2), pagamento_minimo numeric(14,2),
       encargos numeric(14,2) default 0,
       status text check (status in ('ABERTA','FECHADA','PAGA','VENCIDA')),
       id_externo text, unique(conta_id, competencia))

-- Transações ---------------------------------------------------------------
transacao(id uuid pk, conta_id uuid fk, fatura_id uuid null fk, id_externo text,
          data date not null, data_lancamento date,
          descricao text not null, descricao_original text,
          valor numeric(14,2) not null,        -- RN-01: saída < 0, entrada > 0
          moeda char(3) default 'BRL',
          estabelecimento_nome text, estabelecimento_cnpj text,
          categoria_id uuid null fk,
          etiqueta text default 'NAO_TRIADA',
          etiqueta_origem text,                -- AUTOMATICA | MANUAL | HERDADA
          confianca_classificacao numeric(3,2),
          parcela_numero int, parcela_total int, compra_origem_id uuid null,
          recorrencia_id uuid null fk,
          ignorada boolean default false, motivo_ignorada text,
          pendente boolean default false,
          origem text not null,                -- OPEN_FINANCE|CSV|OFX|PDF|MANUAL
          data_hora timestamptz null,          -- RN-12: hora da compra, quando a fonte fornece
          hora_confiavel boolean default false,
          descricao_normalizada text not null, -- base do pg_trgm
          grupo_similaridade text null,        -- chave do agrupamento (RN-12)
          hash_dedupe text not null,
          versao bigint default 0,             -- optimistic locking
          criado_em timestamptz, atualizado_em timestamptz,
          unique(conta_id, hash_dedupe))
-- índices: (conta_id, data desc), (categoria_id, data), (etiqueta) where not ignorada
--          GIN (descricao_normalizada gin_trgm_ops)  -- RN-12, exige: create extension pg_trgm
--          (grupo_similaridade) where categoria_id is null

-- Catálogo e regras --------------------------------------------------------
categoria(id uuid pk, usuario_id uuid null,   -- null = categoria de sistema
          nome text, grupo text, natureza text check (natureza in ('FIXO','VARIAVEL')),
          etiqueta_padrao text, ativa boolean default true)

regra_categorizacao(id uuid pk, usuario_id uuid fk, prioridade int not null,
                    tipo text,                -- CNPJ|REGEX_DESCRICAO|CONTA|VALOR
                    padrao text, categoria_id uuid fk, etiqueta_sugerida text,
                    criada_por text)          -- SISTEMA|USUARIO|APRENDIZADO

piso_humano(id uuid pk, usuario_id uuid fk, categoria_id uuid fk,
            valor_piso numeric(14,2) not null,
            frequencia_max int, ticket_medio numeric(14,2),
            justificativa text, estimado boolean default false,
            unique(usuario_id, categoria_id))

meta(id uuid pk, usuario_id uuid fk, categoria_id uuid fk,
     competencia_inicio date, valor_meta numeric(14,2), tipo text default 'TETO')
-- invariante: valor_meta >= piso_humano.valor_piso

-- Recorrências e compromissos ---------------------------------------------
recorrencia(id uuid pk, usuario_id uuid fk, nome_normalizado text,
            categoria_id uuid null, valor_medio numeric(14,2),
            periodicidade text,               -- MENSAL|ANUAL|SEMANAL
            primeira_ocorrencia date, ultima_ocorrencia date, ocorrencias int,
            confianca numeric(3,2), status text, decisao text, decidida_em date)

compromisso_futuro(id uuid pk, usuario_id uuid fk, origem text,
                   descricao text, conta_id uuid null, categoria_id uuid null,
                   competencia date, valor numeric(14,2),
                   parcela_numero int, parcela_total int, confirmado boolean)

divida(id uuid pk, usuario_id uuid fk, nome text, credor text, tipo text,
       saldo_devedor numeric(14,2), taxa_juros_mensal numeric(8,6),
       taxa_estimada boolean default false,
       data_referencia date, parcela_valor numeric(14,2), parcelas_restantes int,
       status text default 'ATIVA')

-- Cenários e projeção ------------------------------------------------------
cenario(id uuid pk, usuario_id uuid fk, nome text, ativo boolean,
        renda_liquida_mensal numeric(14,2), renda_extra_mensal numeric(14,2) default 0,
        estrategia_amortizacao text,          -- AVALANCHE|BOLA_DE_NEVE|PROPORCIONAL
        reserva_alvo_meses int default 6, criado_em timestamptz)

cenario_meta(cenario_id uuid fk, categoria_id uuid fk, valor_mensal numeric(14,2),
             primary key(cenario_id, categoria_id))

projecao_mes(id uuid pk, cenario_id uuid fk, competencia date,
             entrada numeric(14,2), saida_fixa numeric(14,2),
             saida_variavel numeric(14,2), juros numeric(14,2),
             amortizacao numeric(14,2), reserva_acumulada numeric(14,2),
             divida_saldo_fim numeric(14,2), gerada_em timestamptz,
             unique(cenario_id, competencia))

-- Histórico para o comparativo --------------------------------------------
snapshot_mensal(id uuid pk, usuario_id uuid fk, competencia date,
                renda_liquida numeric(14,2), total_saida numeric(14,2),
                total_vermelho numeric(14,2), total_amarelo numeric(14,2),
                total_azul numeric(14,2), total_fixo numeric(14,2),
                taxa_economia numeric(6,4), dias_considerados int,
                parcial boolean, fechado_em timestamptz,
                unique(usuario_id, competencia))

snapshot_categoria(snapshot_id uuid fk, categoria_id uuid fk,
                   valor numeric(14,2), transacoes int,
                   primary key(snapshot_id, categoria_id))

-- Padrões temporais e semáforo (RN-13, RN-14) ------------------------------
padrao_temporal(id uuid pk, usuario_id uuid fk, categoria_id uuid fk,
                recorte text,                 -- DIA_SEMANA|PERIODO_DIA|SEMANA_MES|DIAS_APOS_RENDA
                chave text,                   -- 'SABADO', 'NOITE', 'SEMANA_1', ...
                concentracao numeric(6,4), esperado numeric(6,4), lift numeric(6,3),
                n_transacoes int, meses_cobertos int,
                ticket_medio numeric(14,2), economia_por_ocorrencia numeric(14,2),
                janela_inicio date, janela_fim date, calculado_em timestamptz,
                unique(usuario_id, categoria_id, recorte, chave, janela_fim))

faixa_saude_config(id uuid pk, usuario_id uuid fk,
                   escopo text,               -- CATEGORIA|GLOBAL
                   limite_ideal numeric(6,4), limite_bem numeric(6,4), limite_ruim numeric(6,4),
                   meta_economia numeric(6,4) default 0.25)

-- Plano de ajuste progressivo (RN-15) --------------------------------------
plano_ajuste(id uuid pk, usuario_id uuid fk, cenario_id uuid fk,
             competencia_inicio date, meses_rampa int default 3,
             fator_max_corte numeric(4,3) default 0.35,
             economia_alvo_mensal numeric(14,2),
             quitacao_sem_plano date, quitacao_com_plano date,
             gerado_em timestamptz, aceito_em timestamptz null)

plano_ajuste_item(plano_id uuid fk, categoria_id uuid fk, competencia date,
                  valor_atual numeric(14,2), valor_alvo numeric(14,2),
                  piso numeric(14,2), economia numeric(14,2),
                  dor int,                    -- 1 = vermelha, 2 = amarela
                  primary key(plano_id, categoria_id, competencia))

-- Viabilidade do padrão de vida (RN-16) ------------------------------------
viabilidade(id uuid pk, usuario_id uuid fk, competencia date,
            renda_liquida numeric(14,2), custo_fixo_total numeric(14,2),
            piso_variavel_total numeric(14,2), custo_minimo_vida numeric(14,2),
            economia_maxima numeric(14,2), taxa_maxima numeric(6,4),
            meta_economia numeric(6,4), veredito text,
            alvo_reducao_fixo numeric(14,2),
            queda_renda_detectada boolean, renda_anterior numeric(14,2),
            queda_pct numeric(6,4), excedente_estrutural numeric(14,2),
            calculado_em timestamptz, unique(usuario_id, competencia))
```

---

## 5. Regras de negócio

### RN-01 — Normalização de sinal
Toda `transacao.valor` segue: **saída negativa, entrada positiva**, em BRL. A Pluggy usa `amount` positivo para débito em cartão; o adapter inverte. Transação em moeda estrangeira usa `amountInAccountCurrency`; se ausente, marca `pendente = true` e não entra em nenhum agregado.

### RN-02 — Deduplicação
```
hash_dedupe = sha256(conta_id | data | valor_em_centavos | normalizar(descricao) | ordinal)
normalizar: uppercase, sem acento, espaços colapsados, remove sufixo de parcela ("3/12"),
            remove sequências numéricas com mais de 6 dígitos (NSU, autorização)
ordinal    = índice da ocorrência idêntica dentro do mesmo lote/dia (0, 1, 2, ...)
```
**O `ordinal` é obrigatório.** Sem ele, dois cafés de R$ 12,00 no mesmo dia no mesmo lugar viram um só — o sistema apagaria uma despesa real. Em conflito entre fontes, vence a de maior confiança: `OPEN_FINANCE > OFX > CSV > PDF > MANUAL`; a perdedora é descartada e o descarte é logado.

### RN-03 — Transações que não são gasto (`ignorada = true`)
Este é o erro nº 1 desse tipo de sistema — dupla contagem:

- **Pagamento de fatura de cartão**: o débito na conta corrente que quita a fatura **não é despesa**. A despesa são as transações *dentro* da fatura.
- **Transferência entre contas próprias**: casada por valor + data (±1 dia) + contraparte pertencente ao usuário.
- **Estorno/chargeback**: casa com a transação original (mesmo estabelecimento, valor oposto, ≤ 90 dias) e anula ambas.

### RN-04 — Fixo vs Variável
`FIXO` se: (a) `categoria.natureza = FIXO`; ou (b) pertence a recorrência com `confianca ≥ 0.8` **e** coeficiente de variação do valor < 0,15; ou (c) é `compromisso_futuro` confirmado. Caso contrário, `VARIAVEL`.

### RN-05 — Triagem das três cores, com piso humano
Cores:
- **AZUL** — indispensável. Aluguel, energia, remédio, e a parte do gasto variável que está **dentro do piso**.
- **AMARELA** — legítima, mas reduzível. O que passa do piso.
- **VERMELHA** — não era necessidade naquele momento.
- **VERDE** — não é gasto: amortização de dívida, aporte em reserva, investimento. Nunca entra em "total de saída para análise de corte".

**Algoritmo do piso** (é isto que faz o sistema entender que você é humano): dentro de uma categoria variável com piso definido, as transações do mês são ordenadas por data; enquanto o acumulado ≤ piso, a transação é **AZUL**; a partir do momento em que ultrapassa, é **AMARELA** por padrão. A transação que cruza o piso é **dividida logicamente** (parte azul, parte amarela) para o agregado — sem alterar o registro. O usuário promove manualmente a VERMELHA o que reconhece como impulso.

> Exemplo: piso de RESTAURANTE = R$ 160/mês ("2x por mês, ticket R$ 80"). Gastou R$ 540 em 7 idas: R$ 160 ficam azuis, R$ 380 ficam amarelos. Se o usuário marcar 2 dessas idas como impulso, elas viram vermelhas e saem do amarelo.

**Auto-sugestão** (nunca decide sozinha): regra do usuário → histórico do mesmo estabelecimento (a última decisão manual vence) → `categoria.etiqueta_padrao`. Se a confiança < 0,70, a transação fica `NAO_TRIADA` e entra na fila de revisão.

### RN-06 — Efeito Choque
Calcula `total_vermelho` do mês e acumulado 12 meses, e traduz para unidades que doem mais que reais:
```
percentual_da_divida   = total_vermelho_12m / saldo_devedor_total
parcelas_equivalentes  = total_vermelho_12m / parcela_media_mensal_da_divida
meses_de_antecipacao   = projetar(cenario, sem_vermelho) - projetar(cenario, atual)
```
Regra de tom (P1): exibir o número, sem adjetivo. "R$ 4.820 em 12 meses = 38% da sua dívida" e nada mais.

### RN-07 — Detector de vampiros
Agrupa por `estabelecimento_cnpj` → senão `merchant.name` → senão descrição normalizada. É recorrência se, nos últimos 6 meses:
- ≥ 3 ocorrências, **e**
- intervalo mediano ∈ [26, 35] dias (mensal) ou [350, 380] dias (anual), **e**
- `(max − min) / mediana ≤ 0,20` — **exceção do reajuste:** se o intervalo for regular e os valores formarem
  um degrau monotônico (todos os valores antigos abaixo de todos os novos), a recorrência é mantida, o
  `valor_medio` passa a considerar apenas o patamar atual e é emitido o evento `REAJUSTE_DETECTADO`

`confianca = 0,4·f(n_ocorrências) + 0,3·regularidade_intervalo + 0,3·estabilidade_valor`.

Sinalizadores de "cobrança silenciosa": recorrência ativa em categoria ASSINATURAS **sem decisão registrada há mais de 6 meses**, ou com `valor_medio < R$ 50` (a faixa que passa despercebida). Saída ordenada por **custo anual** (`valor_medio × 12`), com a frase "cancelar libera R$ X/mês, R$ Y/ano".

### RN-08 — Saldo de Sobrevivência
```
ComprometidoFixo(m)   = Σ compromisso_futuro(m) + Σ recorrências fixas ativas(m)
MinimoVariavel(m)     = Σ piso_humano(c) para toda categoria variável ativa c
ServicoDivida(m)      = Σ parcelas de dívida(m) + pagamento mínimo de fatura em rotativo
TotalComprometido(m)  = ComprometidoFixo(m) + MinimoVariavel(m) + ServicoDivida(m)
SaldoSobrevivencia(m) = RendaLiquida(m) − TotalComprometido(m)

se SaldoSobrevivencia < 0:
    RendaExtraNecessaria = arredondar_para_cima(|SaldoSobrevivencia|, 50)
```
**Regra crítica:** `MinimoVariavel` usa **piso**, não média histórica. Se uma categoria variável ativa não tem piso definido, o sistema usa o **percentil 25 dos últimos 6 meses**, grava `piso_humano.estimado = true` e devolve um aviso explícito no DTO. Estimativa silenciosa é o que faz um diagnóstico financeiro mentir.

### RN-09 — Projeção: "quando eu quito?"
Simulação determinística, mês a mês, horizonte padrão 60 meses:
```
para cada competência m em [m0 .. m0+59]:
    entrada     = renda_liquida(m) + renda_extra(m) + recebimentos_extras(m)
    saida_fixa  = compromissos(m) + fixos(m)
    saida_var   = Σ meta_categoria(m)          -- default = piso, se não houver meta
    disponivel  = entrada − saida_fixa − saida_var

    juros(m)    = Σ_i saldo_i × taxa_i         -- por dívida
    se disponivel <= 0:
        amortizacao = 0
        saldo_i += juros_i                     -- a dívida cresce; marca mês inviável
    senão:
        amortizacao = min(disponivel, Σ saldo_i + Σ juros_i)
        aloca(amortizacao) conforme estratégia
        reserva += disponivel − amortizacao
    registra projecao_mes(m)
```
**Estratégias:** `AVALANCHE` (maior taxa primeiro — menor custo total), `BOLA_DE_NEVE` (menor saldo primeiro — primeira vitória mais cedo), `PROPORCIONAL`. A UI mostra as duas primeiras lado a lado: *"avalanche economiza R$ X de juros; bola de neve te dá a primeira dívida zerada no mês M"*. Atenção: com disponível fixo, as duas estratégias costumam quitar **no mesmo mês** — a diferença real está nos juros pagos e no ritmo psicológico, não na data final. Não prometa meses que a matemática não entrega..

**Marcos:** `data_quitacao` (primeiro m com `Σ saldo = 0`), `primeiro_real_guardado` (primeiro m com `reserva > 0`), `reserva_completa` (`reserva ≥ MinimoVariavel + ComprometidoFixo` × `reserva_alvo_meses`).

**Inviabilidade:** se não quita em 60 meses, retorna `INVIAVEL` e calcula por **busca binária** a `renda_extra_minima` que faz quitar em ≤ 24 meses.

**Invariantes (property-based tests):** saldo de dívida nunca fica negativo · `Σ amortizações + Σ juros` reconcilia com a redução do saldo até o centavo · reserva é monotonicamente não decrescente quando não há saque · aumentar renda extra nunca atrasa a quitação.

### RN-10 — Comparativo temporal
`baseline` = média dos 3 meses anteriores ao início do plano, **congelada** em snapshot.
```
variacao_pct   = (atual − baseline) / baseline
progresso_meta = (baseline − atual) / (baseline − meta)     -- 1,0 = meta atingida
```
Classificação: `MELHOROU` se `progresso_meta ≥ 0,25` e variação < 0 · `ESTAVEL` se `|variação| < 0,05` · `PIOROU` se variação > 0,05.

**Guarda-corpo (P5):** mês corrente é normalizado por dia decorrido (run-rate: `gasto_até_hoje / dias_decorridos × dias_do_mês`) e devolvido com `parcial = true`. Sem isso, todo dia 3 do mês parece uma vitória. Com 12+ meses de histórico, o sistema também mostra a comparação ano contra ano — setembro tem seu aniversário, dezembro tem dezembro.

Frase-alvo do dashboard, gerada por esta regra: *"Há 3 meses, comida na rua era seu maior gasto (R$ 1.240). Este mês: R$ 620 — queda de 50%. Sua meta é R$ 250 (−80%). Você está em 62% do caminho."*

### RN-11 — Bloqueio do Eu do Futuro
`POST /cenarios/{id}/simular-compra` recebe valor, número de parcelas e data da primeira. O motor reprojeta e devolve o **delta na data de quitação**. Se a compra adia a quitação, a resposta traz `alerta: "adia sua quitação em N meses"` e a UI exige confirmação explícita. O sistema não bloqueia de fato — não é o banco —, mas nenhuma parcela nova entra no cenário sem que o custo em meses tenha sido mostrado.


### RN-12 — Enriquecimento temporal e fila de não identificados

O problema real: `PAG*IFD 8823` não diz nada. A descrição sozinha não classifica — o **contexto** classifica.

Toda transação carrega, quando a fonte fornece, `data_hora` (timestamptz) e `hora_confiavel`. A Pluggy
devolve `date` em ISO-8601 UTC, mas nem toda instituição preenche a hora; extrato em PDF quase nunca traz.
**Sem hora confiável, o sistema omite o recorte de período do dia em vez de inventar meia-noite.**

Cada transação sem classificação confiável (confiança < 0,70) entra na fila com um **cartão de contexto**:

| Sinal | Como é obtido |
|---|---|
| Dia da semana e período | `data_hora` → `DayOfWeek` + faixa (MADRUGADA 0–5, MANHÃ 6–11, TARDE 12–17, NOITE 18–23) |
| Grupo de similares | `grupo_similaridade`: descrição normalizada + `pg_trgm` similarity ≥ 0,6 ou prefixo comum ≥ 8 chars |
| Recorrência | "aparece 6 vezes nos últimos 3 meses, sempre entre os dias 8 e 12" |
| Ticket típico | média e desvio do grupo |
| Co-ocorrência | outra transação já classificada que aparece sistematicamente até 48h depois |
| Sugestões ranqueadas | `[{categoria, confiança}]` derivadas das regras + histórico do grupo |

**Classificar uma resolve o grupo inteiro.** Ao decidir uma transação da fila, a resposta oferece
`aplicarAoGrupo: true`, que reclassifica as N similares e cria uma `regra_categorizacao` de origem
`APRENDIZADO`. É isto que faz a triagem escalar — sem isso, categorizar 6 meses de fatura é trabalho manual
que ninguém termina.

Decisão técnica: o agrupamento por similaridade fica **no Postgres** (`pg_trgm` + índice GIN sobre
`descricao_normalizada`). Trazer milhares de descrições para a JVM e comparar em memória é o caminho fácil
que não escala.

### RN-13 — Padrões temporais de gasto

Janela de 6 meses, por categoria, em quatro recortes: **dia da semana** · **período do dia** ·
**semana do mês** (1ª a 5ª) · **dias desde a última entrada de renda**.

Um padrão só vira insight se for estatisticamente relevante — caso contrário o sistema produz ruído com
cara de descoberta:
```
concentracao = valor_no_recorte / valor_total_da_categoria
esperado     = dias_no_recorte  / dias_totais_da_janela
lift         = concentracao / esperado

reportar se:  lift >= 1,5  E  n_transacoes >= 8  E  o padrão cobre >= 3 meses distintos
```

Exemplo com os limiares reais: sábado + domingo representam 2/7 = 28,6% dos dias. Se 61% do gasto com
comida fora acontece neles, `lift = 0,61 / 0,286 = 2,13` → padrão reportado:

> *"61% do seu gasto com comida fora acontece no fim de semana, em 14 transações ao longo de 4 meses.
> Ticket médio R$ 78. Uma saída de fim de semana a menos por mês = R$ 78/mês = R$ 936/ano."*

O insight sempre traz o **valor da ação**, nunca só a observação. "Você gasta mais no fim de semana" é
inútil; "uma saída a menos por mês são R$ 936/ano" é acionável.

**Guarda-corpos:** recortes com menos de 8 transações não são reportados · o recorte de período do dia é
omitido se `hora_confiavel = false` em mais de 30% das transações da categoria · meses com menos de 15
dias de dados não entram na janela.

### RN-14 — Semáforo de saúde por área

Quatro faixas, por categoria e uma global. A base de comparação é a **meta**, não o baseline — o objetivo
é chegar onde se quer estar, não apenas melhorar em relação ao passado.

**Por categoria** (`consumo = gasto_projetado_do_mês / meta`, com run-rate se o mês for parcial):

| Faixa | Condição | Leitura |
|---|---|---|
| `IDEAL` | `consumo ≤ 0,85` | sobra folga dentro da meta |
| `SEGUINDO_BEM` | `0,85 < consumo ≤ 1,00` | vai fechar na meta |
| `RUIM` | `1,00 < consumo ≤ 1,25` | estoura, mas é recuperável no mês |
| `PESSIMO` | `consumo > 1,25` | estouro que compromete o mês |

**Ritmo** (só para o mês corrente) — responde "estou gastando rápido demais?":
```
ritmo = (gasto_até_hoje / meta) / (dias_decorridos / dias_do_mês)
ritmo ≤ 1,0 → dentro do ritmo
```
Um consumo de 0,50 no dia 5 parece ótimo, mas o ritmo é 3,0 — vai estourar. É o ritmo que alerta cedo.

**Global**, ancorado na meta de 25–30% do Felipe:
```
taxa_economia = (renda_líquida − saída_total_não_verde) / renda_líquida
```
| Faixa | Condição |
|---|---|
| `PESSIMO` | `taxa < 0` (gastando mais do que ganha) |
| `RUIM` | `0 ≤ taxa < 0,10` |
| `SEGUINDO_BEM` | `0,10 ≤ taxa < 0,25` |
| `IDEAL` | `taxa ≥ 0,25` ← a meta |

Os limiares vivem em `faixa_saude_config` e são editáveis; estes são os defaults calibrados para a meta declarada.

### RN-15 — Plano de Ajuste Progressivo

Uma projeção de corte não é um número, é uma **rampa**. "Corte 60% de comida fora a partir de amanhã" é o
tipo de plano que falha no mês 2 e leva o sistema junto.

```
Para cada categoria variável c:
    atual_c = mediana dos últimos 3 meses fechados   (mediana, não média: um mês atípico não define a régua)
    piso_c  = piso humano de c
    alvo_c  = meta(c), default piso_c
    se atual_c <= alvo_c: a categoria não entra no plano

Rampa GEOMÉTRICA (redução percentual constante mês a mês):
    N_min     = ceil( ln(alvo_c / atual_c) / ln(1 − fator_max_corte) )
    N         = min(12, max(N_solicitado, N_min))          -- default N_solicitado = 3
    razao     = (alvo_c / atual_c) ^ (1/N)
    alvo_c(m) = max( alvo_c, atual_c × razao^m )           -- m = 1..N

Exceções:
    etiqueta VERMELHA → alvo = 0 já no mês 1 (não há rampa para o que não deveria existir)
    etiqueta AZUL     → não entra no plano
```

**Por que geométrica e não linear.** Uma rampa linear parece mais simples (`corte R$ 307 por mês`), mas o
esforço percentual cresce a cada mês e estoura o limite de realismo exatamente no fim, quando a disciplina
já está desgastada. Reduzir R$ 1.240 → R$ 320 em 3 meses linearmente exige cortes de **24,7% → 32,9% →
48,9%**. A mesma meta em rampa geométrica exige **28,7% todo mês**, e o `N_min` mostra que 3 meses não
cabem no limite: são 4.

**Restrição de realismo:** nenhuma categoria cai mais de **35% de um mês para o outro** (`fator_max_corte`,
configurável). O `N_min` já garante isso por construção — se a rampa pedida for curta demais, ela é alongada
automaticamente e o plano informa o novo número de meses. Se nem 12 meses forem suficientes (alvo muito
próximo de zero), o plano entrega a rampa de 12 meses e sinaliza que o alvo não é alcançável no horizonte
sem uma mudança estrutural. Um plano que o usuário não consegue seguir é pior que nenhum plano.

**Priorização das ações** — ordena por retorno sobre dor:
```
impacto = economia_mensal / dor        dor: VERMELHA = 1 · AMARELA = 2 · AZUL = ∞ (não entra)
```
A saída (`GET /plano-ajuste`) traz a tabela mês a mês com alvo por categoria, economia acumulada, a nova
data de quitação com o plano aplicado, e as **três ações de maior impacto** em linguagem direta:

> *1. Cancelar 2 assinaturas sem uso: R$ 89/mês, dor mínima.*
> *2. Comida na rua de R$ 1.240 → R$ 800 no mês 1, → R$ 520 no mês 3 (piso R$ 320): R$ 720/mês ao final.*
> *3. Transporte por app nos dias úteis: R$ 310 → R$ 180: R$ 130/mês.*
> *Total ao final da rampa: R$ 939/mês. Quitação antecipada de 5 meses.*

### RN-16 — Teste de Viabilidade do Padrão de Vida

**A regra mais importante do sistema.** Responde: *dá para guardar 25–30% ganhando R$ 10k, ou o padrão de
vida precisa cair?*

```
CustoFixoTotal    = Σ fixos + Σ compromissos futuros + serviço da dívida
PisoVariavelTotal = Σ piso_humano(c) para toda categoria variável ativa
CustoMinimoVida   = CustoFixoTotal + PisoVariavelTotal
EconomiaMaxima    = RendaLiquida − CustoMinimoVida
TaxaMaxima        = EconomiaMaxima / RendaLiquida
```

`TaxaMaxima` é o teto: **é o quanto sobraria se você executasse com perfeição absoluta**, gastando exatamente
o piso em tudo. Comparada com `meta_economia` (0,25):

| Veredito | Condição | O que significa |
|---|---|---|
| `VIAVEL` | `TaxaMaxima ≥ meta` | Dá, sem mexer no padrão. O gap está no amarelo e no vermelho — é execução, e o plano de ajuste (RN-15) resolve |
| `VIAVEL_PARCIALMENTE` | `0 < TaxaMaxima < meta` | No melhor cenário possível você guarda X%, abaixo dos 25%. Para chegar na meta é preciso reduzir custo fixo em `alvo_reducao_fixo` |
| `INVIAVEL` | `TaxaMaxima ≤ 0` | O custo mínimo de vida excede a renda. **Nenhuma disciplina resolve** — é estrutural |

```
alvo_reducao_fixo = (meta_economia × RendaLiquida) − EconomiaMaxima
```

Quando o veredito não é `VIAVEL`, o sistema ordena os custos fixos por peso na renda líquida e destaca os
que passam dos benchmarks de referência (exibidos como referência, **nunca como regra**):
moradia ≤ 30% · transporte ≤ 15% · serviço da dívida ≤ 30%.

#### RN-16.1 — Detecção de queda estrutural de renda

Gatilho: `mediana(renda dos últimos 3 meses) < 0,85 × mediana(renda dos 3 meses anteriores a esses)`.

Quando dispara, o diagnóstico abre com esta análise, **antes de qualquer sugestão de corte**:
```
renda_anterior   = mediana da renda antes da queda
renda_atual      = mediana da renda depois da queda
queda_pct        = 1 − renda_atual / renda_anterior
custo_fixo_atual = CustoFixoTotal
peso_fixo_antes  = custo_fixo_atual / renda_anterior
peso_fixo_agora  = custo_fixo_atual / renda_atual
excedente        = custo_fixo_atual − (peso_fixo_antes × renda_atual)
```
No caso do Felipe: renda caiu de R$ 14.000 para R$ 10.000 (−28,6%). Um custo fixo que pesava 45% da renda
antiga (R$ 6.300) passa a pesar **63%** da nova, sem que nada tenha sido gasto a mais.

Mensagem gerada: *"Seu custo fixo foi dimensionado para uma renda 28,6% maior. Mantendo o padrão atual, a
conta não fecha — isso é aritmética, não falta de disciplina. Para restaurar o mesmo peso de antes, o fixo
precisa cair R$ X."*

Este bloco aparece no topo do dashboard enquanto o veredito não for `VIAVEL`. É o contexto sem o qual todo
o resto do sistema soa como culpa mal endereçada.


### RN-17 — O sistema pergunta o que falta

Um diagnóstico financeiro nunca tem todos os dados. A diferença entre um sistema útil e um sistema
que mente está em **saber o que não sabe** e perguntar — na ordem certa, uma coisa de cada vez, com o
motivo explícito.

Cada regra declara as suas dependências de dados. O motor de lacunas classifica cada dependência:

| Tipo | Significado | Exemplo real |
|---|---|---|
| `AUSENTE` | nunca foi informado | renda líquida de PJ |
| `ESTIMADO` | o sistema chutou e marcou (RN-08) | piso de MERCADO pelo percentil 25 |
| `DESATUALIZADO` | informado há mais de 6 meses | valor do aluguel |
| `AMBIGUO` | o sistema viu mas não consegue classificar | Pix recorrente de R$ 517,67 para uma pessoa física |
| `CONTRADITORIO` | duas fontes discordam | reconciliação que não fecha (RN-02.1) |

Priorização — não adianta ter a lista certa na ordem errada:
```
impacto  = nº de regras que a lacuna destrava × sensibilidade do resultado a ela
atrito   = 1 (o usuário sabe de cabeça) · 3 (precisa abrir o app) · 5 (depende de terceiro)
prioridade = impacto / atrito
```
A fila mostra **uma pergunta por vez**, ordenada por prioridade, sempre com o motivo:
*"sem a taxa de juros dessa dívida, a data de quitação é chute"*. Um formulário de 30 campos não é
perguntar — é transferir o trabalho.

**A resposta se propaga para trás.** Isto é o que separa a RN-17 de um cadastro: quando o usuário
responde `"aquele Pix de R$ 517,67 é o contador"`, o sistema (a) reclassifica **todas as ocorrências
passadas** daquele estabelecimento, (b) cria a `regra_categorizacao` de origem `APRENDIZADO`, e
(c) **recalcula os snapshots afetados**. Uma pergunta respondida conserta o histórico, não só o futuro.

Gatilhos para entrar na fila: importação nova com transações de baixa confiança · recorrência
detectada sem categoria (RN-07) · categoria variável ativa sem piso (RN-08) · dívida sem taxa (RN-09)
· veredito de viabilidade calculado com premissa estimada (RN-16) · reconciliação que não fecha.

#### RN-17.1 — Detector de duplo lançamento

Antes de somar um custo fixo declarado pelo usuário ao custo fixo total, o sistema procura uma
recorrência já detectada nas faturas com valor próximo — **±10% ou ±R$ 50, o que for maior** — e, se
achar, **pergunta se são a mesma coisa em vez de somar as duas**.

Este caso não é hipotético: no primeiro diagnóstico o contador entrou como custo fixo declarado de
R$ 500 **e** como Pix recorrente de R$ 517,67 detectado nas faturas. São o mesmo pagamento — a
diferença de R$ 17,67 é o custo de pagá-lo no crédito. Somados, inflavam o custo fixo em R$ 500/mês e
o veredito de viabilidade saía pessimista por um erro de contabilidade, não por um fato.

A regra vale nos dois sentidos: um custo fixo declarado que **não** aparece em nenhuma recorrência
também vira pergunta (*"o aluguel não aparece em nenhuma fatura nem no extrato — como você paga?"*),
porque despesa que o sistema não vê é despesa que a projeção esquece.

### RN-08.1 — Descasamento de datas (ponte de caixa)

O saldo de sobrevivência (RN-08) responde *quanto* sobra no mês. Não responde *quando* — e é no
"quando" que mora um risco que o número mensal esconde.

Para cada compromisso fixo com dia de vencimento conhecido, o sistema calcula:
```
folga(compromisso) = dia_da_entrada_de_renda − dia_do_vencimento     (em dias, podendo ser negativa)
```
Folga negativa significa que o compromisso vence **antes** do dinheiro entrar. O sistema classifica:

| Situação | Leitura |
|---|---|
| folga ≥ 5 dias | confortável |
| 0 ≤ folga < 5 | apertado — sinaliza |
| folga < 0 e existe mecanismo de ponte declarado | **ponte de caixa** — reporta o custo e a margem |
| folga < 0 e não há mecanismo | déficit de calendário — vai gerar juros ou atraso |

Quando há ponte (limite/cheque especial com carência, antecipação de recebível, cartão), o sistema
registra o **prazo de carência** e a **margem em dias**, e trata margem ≤ 1 dia como risco alto: o
custo é zero enquanto o prazo for cumprido e salta para a taxa cheia no primeiro dia de atraso.

Caso real do Felipe: aluguel de R$ 2.200 vence dia 25, a renda entra dia 5, e ele usa o limite do
Itaú com 10 dias de carência — exatamente os 10 dias de que precisa. **Margem zero.** Um pagamento de
cliente atrasado transforma R$ 0 de custo em juros de cheque especial sobre R$ 2.200.

As duas saídas que o sistema deve sugerir, nesta ordem: (1) **negociar o vencimento** para depois da
data de entrada — custo zero, resolve para sempre; (2) **construir uma reserva do tamanho do
compromisso** — o primeiro mês de sobra já cobre, e aí a ponte deixa de ser necessária.


### RN-09.1 — Comparador de crédito de curto prazo

Quando o saldo de sobrevivência é negativo (RN-08) e existe fatura maior que a capacidade de
pagamento do mês, o sistema **não sugere "corte gastos"** — o mês já está fechado, não há o que
cortar. Ele compara as formas de adiar e ordena por custo efetivo:

| Instrumento | De onde vem a taxa |
|---|---|
| Rolagem via Pix no crédito | **medida** na própria fatura: valor cobrado ÷ fatura quitada − 1 |
| Parcelamento de fatura | declarado na fatura (Itaú 12,40% a.m., PicPay 14,70% a.m.) |
| Rotativo | declarado na fatura (Itaú 15,60% a.m., PicPay 14,90% a.m.) |
| Empréstimo pessoal / consignado | informado pelo usuário |
| Antecipação de recebível (PJ) | informado pelo usuário |

Sempre expostos juntos: taxa mensal, **equivalente anual composto** e custo em reais sobre o saldo.
6,4% ao mês soa pequeno; 109,7% ao ano não soa.

Duas regras de tom, e elas importam mais do que a tabela:

1. **Se o usuário já escolheu a opção mais barata, o sistema diz isso.** No caso real, rolar no Pix
   crédito a 6,4% a.m. custa metade do parcelamento de fatura e 40% do rotativo. Chamar isso de
   descontrole é errado e destrói a confiança no diagnóstico.
2. **Nenhuma delas abate o principal** — o sistema sempre mostra, junto da tabela, a **data em que o
   saldo rolado zera** no cenário atual. É essa data que transforma "adiar" em "plano".

### RN-09.2 — Rolagem entre cartões

Uma transação é `ROLAGEM` quando o beneficiário é a instituição emissora de **outro cartão do próprio
usuário** (`PIX Nu Pagamentos SA` numa fatura do Itaú, `Picpay*Pagamento de` numa fatura do Nubank) e
existe, na conta de destino, um pagamento de fatura de valor compatível em até 3 dias.

Tratamento:

- O valor **não é despesa** — é operação financeira; entra em `ROLAGEM`, fora de todo agregado de consumo.
- O **custo implícito é despesa financeira** e esse sim entra: `valor_cobrado − fatura_quitada`. No caso
  real, R$ 2.290,36 de fatura viraram R$ 2.436,11 na fatura seguinte — R$ 145,75 de custo que não
  aparece em lugar nenhum se a transação for lida como uma compra qualquer.
- O sistema conta **rolagens consecutivas**. Uma é manobra de caixa. Duas seguidas é padrão, e o
  alerta muda de tom: mostra o saldo total rolando, o custo acumulado e a data de saída — nunca um
  julgamento.

**Contra-indicação explícita:** enquanto houver rolagem ativa, o sistema **bloqueia a sugestão de
parcelar a fatura** se a taxa do parcelamento for maior que a da rolagem. Parcelar parece
organização e, nas taxas reais, custa o dobro e prende o usuário por meses.

**Risco de limite alto.** Limite grande é combustível: com R$ 26.510 disponíveis, dá para rolar por
um ano — e rolar R$ 8.000 a 6,4% ao mês por 12 meses custa mais de R$ 8.700 só de taxa, mais que o
próprio saldo. O sistema exibe o **custo de rolar até o fim do limite** ao lado da data de saída, para
que as duas leituras apareçam juntas.


### RN-18 — Migração de crédito para débito

**Por que o crédito é usado.** Não é conveniência: é float. Uma compra no dia 5 só é paga no dia 10 do
mês seguinte — até 35 dias de prazo grátis. Quem vive apertado usa o cartão porque ele é a única
linha de crédito sem juros disponível.

Segue daí a regra central: **migrar para o débito não é uma decisão de vontade, é uma função de
caixa.** Pedir "pare de usar o cartão" para quem não tem um mês de gastos em conta é pedir o
impossível — e o sistema que faz isso perde a confiança do usuário na primeira semana.

**O custo da migração.** No mês em que um gasto sai do crédito e entra no débito, ele é pago duas
vezes: a fatura do mês anterior **mais** o gasto do mês corrente. O custo único é exatamente
**um mês do valor migrado**, e ele é despesa, não reserva — sai e não volta.
```
capital_de_giro = Σ (tudo que hoje passa no cartão)
                = fixos no cartão + verba variável
```

**Pré-requisito: a ponte de caixa primeiro (RN-08.1).** Migrar sem resolver o descasamento de datas
troca uma dependência de crédito por outra — o cartão sai, o cheque especial entra. O sistema
**bloqueia o início da migração** enquanto a reserva não cobrir o maior compromisso que vence antes da
entrada de renda.

**Ordem de migração.** Blocos, um ou mais por mês, conforme a sobra permitir, ordenados por:
```
prioridade = (frequência_de_uso × vontade_declarada) / valor_mensal
```
Frequência alta e valor baixo primeiro: constrói o hábito antes de exigir caixa. As categorias que o
usuário **declarou querer no débito** sobem na fila — motivação é insumo, não enfeite.

**Indicadores:** `pct_no_debito = migrado / total_no_cartão` e a competência de
`independencia_do_credito` (primeiro mês com `pct_no_debito = 100%`). É esse mês que o dashboard
mostra em destaque.

**Ganho colateral.** Migrar um pagamento que hoje é feito via Pix **no crédito** para Pix **no débito**
elimina a taxa da operação. No caso real, DARF e contador somam R$ 1.334/mês pagos no crédito — pela
taxa medida na RN-09.1, entre R$ 40 e R$ 85 por mês que somem só de mudar o meio de pagamento.

### RN-19 — Orçamento diário (o Termômetro)

O método da planilha que o usuário já usava, transformado em regra viva:
```
verba_do_mes        = verba_variavel − provisão (RN-20)
gasto_ate_hoje      = Σ variável do mês corrente
dias_restantes      = dias_do_mês − dia_de_hoje + 1
verba_de_hoje       = (verba_do_mes − gasto_ate_hoje) / dias_restantes
```
**A verba se recalcula todo dia.** Gastou mais ontem, a de hoje encolhe; segurou ontem, a de hoje
cresce. É um orçamento que responde, não um teto que se quebra e vira culpa.

**Semáforo do dia** (mesmas faixas da RN-14, aplicadas à verba diária): `IDEAL` se
`verba_de_hoje ≥ verba_base` · `SEGUINDO_BEM` até −15% · `RUIM` até −40% · `PESSIMO` abaixo disso.

**Tradução em ações — a parte que faz a regra funcionar.** "Você tem R$ 91,67 hoje" não muda
comportamento. O sistema converte a verba em unidades reais usando o **ticket médio medido de cada
categoria do próprio usuário**:

> *"Sobram R$ 640 para 7 dias — R$ 91 por dia. Dá para 3 saídas de restaurante (R$ 38 cada) mais o
> mercado da semana, ou 2 saídas e um delivery."*
> *"Você gastou R$ 210 em 3 dias. No ritmo de hoje o mês fecha em R$ 2.100 acima da verba. Hoje é dia
> de cozinhar."*

Tickets medidos nas faturas reais: mercado R$ 43,79 · restaurante R$ 38,16 · delivery R$ 34,20 ·
transporte por app R$ 8,63.

**Guarda-corpo:** dia 1 e 2 do mês não geram alerta de ritmo — dois dias não predizem trinta
(edge case 27).

### RN-20 — Provisão para gastos irregulares

Uma projeção que assume o mês perfeito é ficção, e ficção quebra na primeira semana. O usuário
**vai** comprar a marmita de R$ 23, **vai** pegar o Uber de R$ 55 para a viagem de sábado e **vai** ao
evento de R$ 170. Nada disso é descontrole; é vida.

A verba variável tem três camadas:

| Camada | O que é | Como é dimensionada |
|---|---|---|
| **Dia a dia** | o cotidiano | `verba_variável − provisão`, dividido por dia (RN-19) |
| **Eventos agendados** | data e valor conhecidos | informados pelo usuário ou herdados de recorrência anual |
| **Provisão** | o que ainda não tem nome | `max(P75 − P50 do gasto mensal histórico; 5% da verba)` |

**A provisão fica DENTRO da verba, nunca em cima.** Somar um colchão ao orçamento é a forma mais
comum de fazer um orçamento mentir — o número final vira o que se queria ver, não o que se pode
gastar. No caso real: verba de R$ 3.000, provisão de R$ 250, dia a dia de R$ 2.750 = **R$ 91,67/dia**.

**Evento que estoura a provisão consome o dia a dia — e o sistema avisa antes, não depois.** Os
eventos de setembro somam R$ 280 contra uma provisão de R$ 250: o excedente de R$ 30 reduz a verba
diária do mês em R$ 1,00. O aviso vai **na véspera do evento**, com o número já ajustado.

Com poucos meses de histórico o sistema usa o piso de 5% e marca a provisão como `ESTIMADA` (RN-17).

### RN-21 — Reserva de emergência em níveis

Alvo declarado: **6 meses do custo mensal, sem as dívidas atuais** — `6 × (fixo + variável)`.
No caso real, `6 × R$ 7.421,07 = R$ 44.526,42`.

Um alvo único e distante não é meta, é horizonte. A reserva é construída em níveis, cada um com um
significado concreto:

| Nível | Valor | O que ele compra |
|---|---|---|
| **0 — Ponte de caixa** | maior compromisso que vence antes da renda (R$ 2.200) | fim da dependência do limite/cheque especial (RN-08.1) |
| **1 — Um mês** | `custo_mensal` (R$ 7.421,07) | um mês inteiro sem renda sem atrasar nada |
| **2 — Três meses** | `3 × custo_mensal` (R$ 22.263,21) | aguenta um cliente atrasar ou um contrato acabar |
| **3 — Seis meses** | `6 × custo_mensal` (R$ 44.526,42) | a meta declarada |

**Ordem de alocação da sobra**, e ela não é negociável: `Nível 0` → `migração para o débito` (RN-18)
→ `Níveis 1, 2 e 3`. Poupar antes de resolver a ponte é guardar dinheiro que vai ser gasto em juros
de cheque especial no dia 25.

O sistema mostra sempre **o próximo nível**, nunca o total — e a data prevista dele.

### RN-22 — Automação e alertas

Um sistema que exige lançamento manual não sobrevive à terceira semana. A ingestão é automática por
construção (RN-01, fatia 12): sync diário via Open Finance, webhook de transação nova, e a
classificação já resolvida por regra e por grupo de similaridade (RN-12) — o usuário só decide o que
o sistema não conseguiu, e cada decisão resolve o grupo inteiro.

**O que dispara notificação**, e nada além disto:

| Gatilho | Mensagem |
|---|---|
| Verba diária cai abaixo de 60% da base | "no ritmo de hoje o mês estoura em R$ X — hoje é dia de cozinhar" |
| Verba diária acima da base com folga | "você está adiantado: dá para 3 saídas nos próximos dias, R$ X no total" |
| Evento agendado em 3 dias | "sábado tem a viagem: R$ 110 de Uber já estão reservados na provisão" |
| Marco atingido | "ponte do aluguel completa — você não precisa mais do limite dia 25" |
| Transação não identificada acima de R$ 100 | uma pergunta, com o cartão de contexto da RN-12 |
| Compra parcelada detectada | "isso adia sua independência do crédito em N meses" (RN-11) |

**Regras de silêncio.** No máximo **uma notificação por dia**; nada entre 22h e 7h; **nunca notificar
sem que haja uma decisão a tomar**. Um app que avisa "você gastou R$ 12" é um app que o usuário
silencia na primeira semana, e um app silenciado não muda comportamento nenhum.

O resumo diário sai uma vez, de manhã, com três informações e nada mais: **verba de hoje**,
**semáforo do mês**, **próximo marco e a data dele**.

---

## 6. Contratos REST

Base: `/api/v1` · JSON · datas ISO-8601 · competência no formato `YYYY-MM` · **valores monetários como string decimal**.

### Ingestão
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/conexoes/pluggy/connect-token` | Gera token para o widget |
| `POST` | `/conexoes/pluggy/callback` | `{itemId}` → cria conexão e dispara sync |
| `POST` | `/conexoes/{id}/sincronizar` | 202 Accepted, job assíncrono |
| `GET` | `/conexoes` | Status + `consentimentoExpiraEm` |
| `POST` | `/importacoes` | multipart: `arquivo`, `tipo` (CSV/OFX/PDF), `contaId` → 202 |
| `GET` | `/importacoes/{id}` | `{status, linhas, importadas, duplicadas, erros[]}` |

### Transações e triagem
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/transacoes` | filtros: `competencia`, `categoriaId`, `etiqueta`, `contaId`, `page`, `size` |
| `PATCH` | `/transacoes/{id}` | `{categoriaId?, etiqueta?, ignorada?, criarRegra?}` |
| `POST` | `/transacoes/triagem-lote` | `{ids[], etiqueta, criarRegra}` |
| `GET` | `/transacoes/pendentes-triagem` | fila de revisão (confiança < 0,70) |

### Configuração humana
`GET|PUT /pisos-humanos` · `GET|PUT /metas` · `GET|PUT /renda` · `GET|POST|PUT /dividas`

### Análise
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/diagnostico?competencia=` | Saldo de sobrevivência, efeito choque, semáforo por categoria |
| `GET` | `/viabilidade?competencia=` | **RN-16** — veredito, taxa máxima, alvo de redução de fixo, queda de renda |
| `GET` | `/transacoes/nao-identificadas` | **RN-12** — fila com cartão de contexto e sugestões ranqueadas |
| `POST` | `/transacoes/{id}/classificar` | `{categoriaId, etiqueta, aplicarAoGrupo}` → reclassifica o grupo e cria regra |
| `GET` | `/padroes?categoriaId=&recorte=` | **RN-13** — padrões com lift ≥ 1,5, ordenados por economia potencial |
| `GET` | `/lacunas` | **RN-17** — próxima pergunta da fila, com motivo e impacto |
| `POST` | `/lacunas/{id}/responder` | Responde, reclassifica o histórico e recalcula os snapshots |
| `GET` | `/calendario` | **RN-08.1** — folga por compromisso, pontes de caixa e margem em dias |
| `GET` | `/credito-curto-prazo` | **RN-09.1** — opções ordenadas por custo efetivo + data de saída |
| `GET` | `/rolagens` | **RN-09.2** — rolagens detectadas, custo implícito e contagem de consecutivas |
| `GET` | `/migracao` | **RN-18** — blocos, ordem, custo de cada um e data da independência do crédito |
| `POST` | `/migracao/{bloco}/concluir` | Marca um bloco como migrado e recalcula o plano |
| `GET` | `/hoje` | **RN-19** — verba do dia, semáforo, ritmo e a tradução em ações |
| `GET` | `/eventos` · `POST` `/eventos` | **RN-20** — gastos futuros conhecidos, com data e valor |
| `GET` | `/reserva` | **RN-21** — nível atual, próximo nível e a data prevista |
| `GET` | `/plano-ajuste?cenarioId=` | **RN-15** — rampa mês a mês + top 3 ações por impacto/dor |
| `POST` | `/plano-ajuste/{id}/aceitar` | Converte os alvos da rampa em `meta` por categoria |
| `GET` | `/vampiros` · `PATCH /vampiros/{id}` | Recorrências e decisão |
| `GET` | `/comparativo?de=&ate=` | Baseline vs. atual vs. meta, com run-rate |
| `GET` | `/dashboard/tres-eus?competencia=` | Agregado das três visões |

### Projeção
`GET|POST|PUT /cenarios` · `POST /cenarios/{id}/projetar` · `POST /cenarios/{id}/simular-compra`

### DTOs principais (records, Java 21)

```java
public record DiagnosticoResponse(
        YearMonth competencia,
        Dinheiro rendaLiquida,
        Dinheiro comprometidoFixo,
        Dinheiro minimoVariavel,
        Dinheiro servicoDivida,
        Dinheiro totalComprometido,
        Dinheiro saldoSobrevivencia,
        boolean deficit,
        Dinheiro rendaExtraNecessaria,
        EfeitoChoqueResponse efeitoChoque,
        List<CategoriaResumoResponse> categorias,
        List<AvisoResponse> avisos) {}

public record EfeitoChoqueResponse(
        Dinheiro totalVermelhoMes,
        Dinheiro totalVermelho12m,
        int transacoesVermelhas,
        BigDecimal percentualDaDivida,
        BigDecimal parcelasEquivalentes,
        Integer mesesDeAntecipacao) {}

public record CategoriaResumoResponse(
        UUID categoriaId, String nome, Natureza natureza,
        Dinheiro gastoAtual, Dinheiro piso, Dinheiro meta,
        Dinheiro baseline, BigDecimal variacaoPct, BigDecimal progressoMeta,
        StatusProgresso status, boolean parcial, boolean pisoEstimado) {}

public record ProjecaoResponse(
        UUID cenarioId,
        StatusProjecao status,               // VIAVEL | VIAVEL_COM_APERTO | INVIAVEL
        List<MesProjetadoResponse> meses,
        MarcosResponse marcos,
        Dinheiro rendaExtraMinimaSugerida,
        List<AvisoResponse> avisos) {}

public record MarcosResponse(
        YearMonth dataQuitacao,
        YearMonth primeiroRealGuardado,
        YearMonth reservaCompleta,
        Dinheiro jurosTotaisPagos,
        int mesesAteQuitacao) {}

public record ViabilidadeResponse(
        Competencia competencia,
        Dinheiro rendaLiquida,
        Dinheiro custoFixoTotal,
        Dinheiro pisoVariavelTotal,
        Dinheiro custoMinimoVida,
        Dinheiro economiaMaxima,
        Percentual taxaMaxima,
        Percentual metaEconomia,
        Veredito veredito,                   // VIAVEL | VIAVEL_PARCIALMENTE | INVIAVEL
        Dinheiro alvoReducaoFixo,
        QuedaDeRendaResponse quedaDeRenda,   // null se não detectada
        List<FixoRelevanteResponse> fixosOrdenadosPorPeso,
        String leitura) {}

public record QuedaDeRendaResponse(
        Dinheiro rendaAnterior, Dinheiro rendaAtual, Percentual quedaPct,
        Percentual pesoFixoAntes, Percentual pesoFixoAgora,
        Dinheiro excedenteEstrutural, String mensagem) {}

public record TransacaoNaoIdentificadaResponse(
        UUID id, String descricaoOriginal, Dinheiro valor, LocalDate data,
        DayOfWeek diaDaSemana, PeriodoDoDia periodo, boolean horaConfiavel,
        String grupoSimilaridade, int similaresNoGrupo,
        Dinheiro ticketMedioDoGrupo, String contextoRecorrencia, String contextoCoOcorrencia,
        List<SugestaoCategoriaResponse> sugestoes) {}

public record PadraoTemporalResponse(
        UUID categoriaId, String categoriaNome, Recorte recorte, String chave,
        Percentual concentracao, Percentual esperado, BigDecimal lift,
        int nTransacoes, int mesesCobertos,
        Dinheiro ticketMedio, Dinheiro economiaMensalSeReduzirUma,
        String insight) {}

public record SaudeResponse(
        FaixaSaude faixa,                    // IDEAL | SEGUINDO_BEM | RUIM | PESSIMO
        BigDecimal consumo, BigDecimal ritmo, boolean parcial) {}

public record PlanoAjusteResponse(
        UUID id, int mesesRampa,
        List<MesDoPlanoResponse> meses,
        List<AcaoPrioritariaResponse> topAcoes,
        Dinheiro economiaMensalAoFinal,
        YearMonth quitacaoSemPlano, YearMonth quitacaoComPlano,
        int mesesAntecipados,
        List<AvisoResponse> avisos) {}       // ex.: "rampa alongada para 5 meses pelo limite de corte"

public record AcaoPrioritariaResponse(
        int ordem, String titulo, UUID categoriaId,
        Dinheiro economiaMensal, int dor, String descricao) {}

public record HojeResponse(
        LocalDate data,
        Dinheiro verbaDeHoje,
        Dinheiro verbaBase,
        Dinheiro gastoAteHoje,
        Dinheiro restanteDoMes,
        int diasRestantes,
        FaixaSaude faixa,
        BigDecimal ritmo,
        boolean baixaConfianca,               // dias 1 e 2 não predizem trinta
        List<TraducaoEmAcoesResponse> podeFazer,
        List<EventoProximoResponse> eventosProximos,
        String mensagem) {}

public record TraducaoEmAcoesResponse(
        String categoria, Dinheiro ticketMedio, int quantidadePossivel, String frase) {}

public record MigracaoResponse(
        Percentual pctNoDebito,
        Dinheiro capitalDeGiro,
        Dinheiro jaMigrado,
        YearMonth independenciaDoCredito,
        boolean ponteDeCaixaResolvida,        // pré-requisito da RN-18
        List<BlocoDeMigracaoResponse> blocos) {}

public record BlocoDeMigracaoResponse(
        String nome, Dinheiro valorMensal, String comoMigrar,
        YearMonth previsao, StatusBloco status) {}   // PENDENTE | EM_ANDAMENTO | MIGRADO

public record ReservaResponse(
        Dinheiro saldoAtual,
        NivelDaReservaResponse nivelAtual,
        NivelDaReservaResponse proximoNivel,
        Dinheiro aporteMensal,
        List<NivelDaReservaResponse> niveis) {}

public record NivelDaReservaResponse(
        int nivel, String rotulo, Dinheiro alvo, Percentual progresso,
        YearMonth previsao, String oQueEleCompra) {}

public record SimulacaoCompraRequest(
        Dinheiro valor, int parcelas, LocalDate dataPrimeiraParcela, String descricao) {}

public record ImpactoCompraResponse(
        YearMonth quitacaoAntes, YearMonth quitacaoDepois,
        int mesesDeAtraso, Dinheiro jurosAdicionais, String alerta) {}
```

`Percentual` é um value object sobre `BigDecimal` com 4 casas na fração (`0,2500` = 25%), serializado como
string, com `deValor(parte, total)`, `aplicarSobre(Dinheiro)` e `formatado()` → `"25,0%"`. Percentual em
`double` solto pelo código é fonte garantida de bug em regra de negócio.

`Dinheiro` é um value object (`BigDecimal` scale 2 + moeda), serializado como string, com operações fechadas (`somar`, `subtrair`, `multiplicar`, `ratear`) — e `ratear` que distribui centavos residuais em vez de perdê-los.

**Erros:** RFC 7807 (`application/problem+json`) com `type` estável por regra violada (ex.: `/erros/meta-abaixo-do-piso`).

---

## 7. Edge cases (a lista que vira teste)

1. Duas transações idênticas no mesmo dia, mesmo estabelecimento, mesmo valor → ambas persistem (RN-02, `ordinal`).
2. Pagamento de fatura + transações da fatura no mesmo mês → conta uma vez só (RN-03).
3. Estorno em mês diferente da compra → anula os dois, e o comparativo do mês da compra é recalculado.
4. Transação `PENDING` que muda de valor no sync seguinte → atualiza, não duplica.
5. Compra parcelada em 12x lançada hoje → gera 12 `compromisso_futuro` e some do "variável" dos meses futuros.
6. Parcelamento da própria fatura pelo banco → vira `divida` do tipo `PARCELAMENTO_FATURA`, não despesa recorrente.
7. Categoria variável ativa sem piso definido → percentil 25 + aviso `pisoEstimado`.
8. Dívida sem taxa de juros informada → assume 0, marca `taxaEstimada`, e o aviso é explícito: rotativo de cartão em ~15% a.m. muda completamente a data de quitação.
9. Renda PJ irregular (mês sem nota, mês com dois pagamentos) → projeção usa mediana móvel de 6 meses, não o último mês.
10. Mês corrente incompleto no comparativo → run-rate + `parcial = true`.
11. Meta abaixo do piso humano → rejeita com 422 e mensagem apontando o piso.
12. `disponivel <= 0` em algum mês da projeção → `VIAVEL_COM_APERTO` ou `INVIAVEL`, nunca amortização negativa.
13. Quitação exata no último centavo → `amortizacao = min(disponivel, saldo + juros)`; saldo nunca negativo.
14. Sync do Pluggy concorrente com edição manual → optimistic locking (`versao`); a edição manual vence e vira `regra_categorizacao` de origem `APRENDIZADO`.
15. Consentimento expirado no meio de um sync → conexão vai para `REAUTENTICAR`, dados antigos preservados.
16. Transação internacional sem `amountInAccountCurrency` → `pendente = true`, fora dos agregados.
17. Cartão adicional / conta conjunta → o gasto é do usuário; a atribuição é manual, com flag.
18. Fevereiro e meses de 28/29/30/31 dias no run-rate → usar `YearMonth.lengthOfMonth()`, nunca 30 fixo.
19. Recorrência com aumento anual (Netflix reajusta) → variação > 20% não quebra a recorrência se o intervalo for regular; abre um evento `REAJUSTE_DETECTADO`.
20. Assinatura anual cobrada uma vez → detectada como `ANUAL`, e o custo mensal equivalente (`/12`) entra no fixo.
21. Fonte sem hora (PDF) → `hora_confiavel = false`; recorte de período do dia é **omitido**, não estimado.
22. Fuso: `data_hora` chega em UTC; a análise de período do dia converte para `America/Fortaleza` antes de fatiar. Compra às 23h local vira 02h UTC do dia seguinte — sem a conversão, "noite de sábado" vira "madrugada de domingo".
23. Grupo de similaridade com transações de categorias diferentes (ex.: `PAG*IFD` que às vezes é mercado, às vezes restaurante) → `aplicarAoGrupo` mostra o preview do que será alterado e permite excluir itens antes de confirmar.
24. Padrão temporal com menos de 8 transações ou menos de 3 meses de cobertura → não é reportado, mesmo com lift alto.
25. Categoria sem meta definida → o semáforo usa o piso como meta e marca `metaImplicita = true`.
26. Meta igual a zero (categoria a ser eliminada) → `consumo` é indefinido; a faixa vira `IDEAL` se o gasto for zero e `PESSIMO` caso contrário. Divisão por zero não pode vazar para o DTO.
27. Ritmo no dia 1 do mês → `dias_decorridos = 1`; ritmo é calculado mas marcado como `baixaConfianca` até o dia 5.
28. Rampa que exigiria corte acima de 35% num mês → alongada automaticamente, com aviso informando o novo número de meses.
29. Categoria cuja mediana dos 3 meses já está **abaixo** do piso → `gap = 0`, não entra no plano, e não gera "meta negativa".
30. Renda com apenas 3 meses de histórico → RN-16.1 não dispara (precisa de 6); o sistema informa que a detecção de queda estrutural exige 6 meses e usa a renda declarada.
31. Mês de transição da queda de renda (fevereiro/2026) → mediana, não média, para não deixar o mês misto contaminar as duas janelas.
32. `TaxaMaxima` calculada com pisos estimados → o veredito vem com `confiabilidade = ESTIMADA` e a UI não exibe o número como definitivo.
33. Verba diária no dia 1 e 2 → calculada, mas sem alerta de ritmo: dois dias não predizem trinta.
34. Gasto do dia maior que a verba do mês inteiro (compra grande) → a verba dos dias seguintes vai a zero, não a negativo, e o sistema propõe recompor puxando da provisão antes de declarar o mês perdido.
35. Migração iniciada sem a ponte de caixa resolvida → bloqueada, com o motivo explícito (RN-18).
36. Bloco migrado que volta para o crédito (o usuário desistiu) → `pct_no_debito` cai e a data de independência é recalculada, sem julgamento no texto.
37. Evento agendado que não acontece → o valor volta para a provisão do próprio mês, não para o dia a dia, senão vira licença para gastar.
38. Mês com 31 dias após um de 30 → a verba diária muda de valor; a comparação entre meses usa a verba total, nunca a diária.
39. Reserva usada numa emergência real → o nível cai, o sistema recalcula a data do próximo e **não** trata como fracasso; é para isso que ela existe.
40. Renda PJ que atrasa → a ponte de caixa é acionada e o sistema mostra o custo em dias, não em julgamento.

---

## 8. Critérios de aceite (Gherkin)

```gherkin
Funcionalidade: Piso humano na triagem

  Cenário: Gasto em restaurante dentro do piso é indispensável
    Dado que o piso humano de "RESTAURANTE" é R$ 160,00 por mês
    E que existem transações de R$ 80,00 em 03/09 e R$ 80,00 em 17/09
    Quando a triagem automática for executada para 2026-09
    Então ambas as transações recebem a etiqueta "AZUL"
    E o total amarelo de "RESTAURANTE" é R$ 0,00

  Cenário: Gasto acima do piso é reduzível, com a transação partida no agregado
    Dado que o piso humano de "RESTAURANTE" é R$ 160,00 por mês
    E que existem transações de R$ 80,00, R$ 100,00 e R$ 60,00 em setembro, nesta ordem
    Quando a triagem automática for executada para 2026-09
    Então o total azul de "RESTAURANTE" é R$ 160,00
    E o total amarelo de "RESTAURANTE" é R$ 80,00

Funcionalidade: Saldo de sobrevivência

  Cenário: Déficit gera meta de renda extra arredondada
    Dado uma renda líquida de R$ 7.400,00 em 2026-09
    E compromissos fixos de R$ 4.100,00
    E um mínimo variável de R$ 2.300,00
    E serviço de dívida de R$ 1.380,00
    Quando eu consultar o diagnóstico de 2026-09
    Então o saldo de sobrevivência é R$ -380,00
    E o campo deficit é verdadeiro
    E a renda extra necessária é R$ 400,00

  Cenário: Piso ausente é estimado e avisado
    Dado que a categoria variável "MERCADO" está ativa e não possui piso humano
    E que existem 6 meses de histórico para "MERCADO"
    Quando eu consultar o diagnóstico
    Então o piso de "MERCADO" é o percentil 25 do histórico
    E a resposta contém um aviso do tipo "PISO_ESTIMADO" para "MERCADO"

Funcionalidade: Não contar o mesmo gasto duas vezes

  Cenário: Pagamento de fatura não é despesa
    Dada uma fatura de R$ 3.200,00 com 42 transações no ciclo de 2026-09
    E um débito de R$ 3.200,00 na conta corrente com descrição "PAGAMENTO FATURA"
    Quando os agregados de 2026-09 forem calculados
    Então o total de saída considera apenas as 42 transações
    E o débito de R$ 3.200,00 está marcado como ignorado

Funcionalidade: Deduplicação

  Cenário: Transações idênticas no mesmo dia são preservadas
    Dado um extrato com duas transações de R$ 12,00 em "CAFETERIA X" em 10/09
    Quando o arquivo for importado
    Então existem 2 transações persistidas
    E o total do dia é R$ 24,00

  Cenário: Reimportação do mesmo arquivo não duplica
    Dado um arquivo já importado com 42 transações
    Quando o mesmo arquivo for importado novamente
    Então 0 transações são criadas
    E o relatório da importação reporta 42 duplicadas

Funcionalidade: Projeção de quitação

  Cenário: Quitação com estratégia avalanche
    Dada uma dívida A de R$ 8.000,00 a 3,5% ao mês
    E uma dívida B de R$ 2.000,00 a 1,2% ao mês
    E um disponível mensal de R$ 1.500,00
    Quando eu projetar o cenário com estratégia "AVALANCHE"
    Então a dívida A é amortizada primeiro
    E o saldo total nunca é negativo em nenhum mês
    E os juros totais pagos são menores que na estratégia "BOLA_DE_NEVE"

  Cenário: Cenário inviável sugere renda extra mínima
    Dado um disponível mensal de R$ 0,00
    E uma dívida de R$ 20.000,00 a 3,5% ao mês
    Quando eu projetar o cenário
    Então o status é "INVIAVEL"
    E é sugerida uma renda extra mínima que quita em até 24 meses

  Cenário: Simular compra parcelada mostra o custo em meses
    Dado um cenário com quitação projetada para 2027-11
    Quando eu simular uma compra de R$ 3.600,00 em 12 parcelas a partir de 2026-10
    Então a nova data de quitação é posterior a 2027-11
    E a resposta contém o alerta com o número de meses de atraso

Funcionalidade: Comparativo temporal

  Cenário: Mês corrente é normalizado por dia decorrido
    Dado que hoje é 03/09/2026
    E que foram gastos R$ 210,00 em "RESTAURANTE" até hoje
    Quando eu consultar o comparativo de "RESTAURANTE"
    Então o valor projetado do mês é R$ 2.100,00
    E o campo parcial é verdadeiro

  Cenário: Progresso em direção à meta
    Dado um baseline de R$ 1.240,00 para "RESTAURANTE"
    E uma meta de R$ 250,00
    E um gasto atual de R$ 620,00 em mês fechado
    Quando eu consultar o comparativo
    Então a variação é -50%
    E o progresso em direção à meta é 62%
    E o status é "MELHOROU"

Funcionalidade: Viabilidade do padrão de vida

  Cenário: Com o custo mínimo atual a meta de 25% é inalcançável
    Dada uma renda líquida de R$ 8.200,00
    E um custo fixo total de R$ 5.400,00
    E um piso variável total de R$ 1.900,00
    E uma meta de economia de 25%
    Quando eu consultar a viabilidade
    Então a economia máxima é R$ 900,00
    E a taxa máxima é 10,98%
    E o veredito é "VIAVEL_PARCIALMENTE"
    E o alvo de redução de custo fixo é R$ 1.150,00

  Cenário: Custo mínimo acima da renda é estrutural
    Dada uma renda líquida de R$ 10.000,00
    E um custo mínimo de vida de R$ 10.800,00
    Quando eu consultar a viabilidade
    Então o veredito é "INVIAVEL"
    E a leitura informa que nenhuma disciplina de gasto resolve o déficit

  Cenário: Queda estrutural de renda é detectada e explicada
    Dada uma renda mediana de R$ 14.000,00 nos meses de setembro a novembro de 2025
    E uma renda mediana de R$ 10.000,00 nos meses de maio a julho de 2026
    E um custo fixo total de R$ 6.300,00
    Quando eu consultar a viabilidade de 2026-08
    Então a queda de renda detectada é 28,57%
    E o peso do custo fixo passou de 45,00% para 63,00%
    E o bloco de queda de renda aparece antes de qualquer sugestão de corte

Funcionalidade: Padrões temporais

  Cenário: Concentração no fim de semana vira insight acionável
    Dadas 14 transações de "RESTAURANTE" em 4 meses, sendo 61% do valor em sábados e domingos
    Quando o detector de padrões for executado
    Então existe um padrão de recorte "DIA_SEMANA" com lift maior ou igual a 2,0
    E o insight informa a economia mensal de reduzir uma ocorrência

  Cenário: Amostra pequena não vira padrão
    Dadas 5 transações de "FARMACIA" concentradas em segundas-feiras
    Quando o detector de padrões for executado
    Então nenhum padrão é reportado para "FARMACIA"

  Cenário: Sem hora confiável, o período do dia é omitido
    Dadas 20 transações de "RESTAURANTE" importadas de PDF sem horário
    Quando o detector de padrões for executado
    Então nenhum padrão de recorte "PERIODO_DIA" é reportado
    E os padrões de recorte "DIA_SEMANA" continuam sendo calculados

Funcionalidade: Fila de não identificados

  Cenário: Classificar uma transação resolve o grupo inteiro
    Dadas 7 transações com descrição similar a "PAG*IFD 8823" sem categoria
    Quando eu classificar uma delas como "ALIMENTACAO_FORA" com aplicarAoGrupo verdadeiro
    Então as 7 transações passam a ter a categoria "ALIMENTACAO_FORA"
    E é criada uma regra de categorização de origem "APRENDIZADO"

Funcionalidade: Semáforo de saúde

  Cenário: Consumo baixo mas ritmo acelerado alerta cedo
    Dada uma meta de R$ 800,00 para "RESTAURANTE"
    E um gasto de R$ 400,00 até o dia 5 de um mês de 30 dias
    Quando eu consultar a saúde da categoria
    Então o consumo é 0,50
    E o ritmo é 3,00
    E a faixa projetada é "PESSIMO"

  Cenário: Faixa global reflete a meta de 25%
    Dada uma renda líquida de R$ 10.000,00
    E uma saída total não-verde de R$ 7.400,00
    Quando eu consultar a saúde global
    Então a taxa de economia é 26,00%
    E a faixa é "IDEAL"

Funcionalidade: Plano de ajuste progressivo

  Cenário: Rampa respeita o piso humano e o limite de corte
    Dado um gasto mediano de R$ 1.240,00 em "RESTAURANTE"
    E um piso humano de R$ 320,00
    E um fator máximo de corte de 35% ao mês
    Quando o plano de ajuste for gerado com rampa de 3 meses
    Então a rampa é alongada para 4 meses
    E os alvos mensais são R$ 883,80, R$ 629,92, R$ 448,97 e R$ 320,00
    E a redução percentual é de 28,7% em todos os meses
    E nenhum alvo mensal é inferior ao piso

  Cenário: Corte muito profundo alonga a rampa até caber no limite
    Dado um gasto mediano de R$ 1.000,00 em "LAZER"
    E um piso humano de R$ 100,00
    E um fator máximo de corte de 35% ao mês
    Quando o plano de ajuste for gerado com rampa de 2 meses
    Então a rampa é alongada para 6 meses
    E existe um aviso informando o novo número de meses
    E nenhuma redução mensal ultrapassa 35%

  Cenário: Rampa curta o suficiente não é alongada
    Dado um gasto mediano de R$ 310,00 em "TRANSPORTE"
    E um piso humano de R$ 180,00
    Quando o plano de ajuste for gerado com rampa de 3 meses
    Então a rampa permanece com 3 meses
    E a redução percentual é de 16,6% em todos os meses

  Cenário: Vermelho não tem rampa
    Dado um gasto mediano de R$ 300,00 em transações etiquetadas como "VERMELHA"
    Quando o plano de ajuste for gerado
    Então o alvo do mês 1 para essas transações é R$ 0,00

Funcionalidade: Vampiros de assinatura

  Cenário: Detecção de assinatura mensal
    Dadas 5 cobranças de "STREAMING Y" entre R$ 39,90 e R$ 44,90, com intervalo de ~30 dias
    Quando o detector de recorrências for executado
    Então existe uma recorrência "MENSAL" com confiança maior ou igual a 0,8
    E o custo anual reportado é aproximadamente R$ 500,00

  Cenário: Reajuste não quebra a recorrência
    Dadas 6 cobranças mensais de "STREAMING Y", sendo as 3 últimas 25% maiores
    Quando o detector for executado
    Então a recorrência permanece ativa
    E é registrado um evento "REAJUSTE_DETECTADO"
```

---

## 9. Requisitos não funcionais, riscos e decisões

**Performance.** A projeção é O(meses × dívidas) — 60 × 5 é irrelevante. O gargalo real é o dashboard agregando anos de transações: por isso `snapshot_mensal` e `snapshot_categoria` são materializados e recalculados por evento (`TransacaoClassificadaEvent`, `ImportacaoConcluidaEvent`), com recálculo do mês corrente sob demanda. Meta: `GET /diagnostico` em < 200 ms p95.

**Concorrência.** Sync do Pluggy e edição manual podem colidir: optimistic locking com `@Version` em `transacao`; import de arquivo protegido por `pg_advisory_xact_lock(hashtext(conta_id::text))` para serializar por conta sem travar o banco. Jobs de sync são idempotentes por construção (RN-02).

**Null safety.** Valores monetários nunca são `null` — o default é `Dinheiro.ZERO`. `Optional` só em portas de saída (repositórios), nunca em campos de entidade nem em parâmetros. `@NullMarked` (JSpecify) no `package-info.java` de cada módulo.

**Segurança.** Client ID/Secret do Pluggy em variável de ambiente (ou Vault), **nunca no banco e nunca no repositório**. Disco cifrado em repouso. `descricao_original` pode conter dados sensíveis — fora dos logs. Auth: single-user com senha + TOTP é suficiente no v1; não invente OAuth para você mesmo.

**Precisão monetária.** `numeric(14,2)` + `BigDecimal`. O rateio de centavos (dividir R$ 100,00 em 3) usa distribuição do resíduo, com teste que garante que a soma das partes é igual ao todo.

**Riscos abertos:**
- Enrichment/categoria da Pluggy é recurso pago → a categorização é nossa, por regras. **Isto é bom**: determinístico e testável.
- Cobertura de `bills` por instituição precisa de um spike com conta real antes de comprometer a fatia 6.
- Consentimento expira em até 12 meses → job de alerta.
- Fatura em aberto pode vir incompleta → o importador de PDF não é opcional.

---

## 10. Arquitetura e roadmap

**Stack:** Java 21 · Spring Boot 3.x · Maven · PostgreSQL 16 · Flyway · Spring Modulith · Testcontainers · Docker Compose.

**Monólito modular, hexagonal por módulo:**
```
termometro/
├── shared-kernel     Dinheiro, Competencia, eventos de domínio
├── ingestao          adapter Pluggy, parsers CSV/OFX/PDF, deduplicação
├── catalogo          categorias, regras, pisos humanos, metas, dívidas
├── classificacao     categorizador, triagem 3 cores, detector de recorrência
├── padroes           similaridade/fila de não identificados, padrões temporais   (RN-12, RN-13)
├── diagnostico       saldo de sobrevivência, efeito choque, semáforo, viabilidade, comparativo, snapshots
├── projecao          motor de cenários, amortização e plano de ajuste            (RN-09, RN-15)
└── api               controllers, DTOs, tratamento de erro, segurança
```
Módulos se comunicam por eventos de aplicação. `ApplicationModules.of(App.class).verify()` como teste — a fronteira que não é testada não existe.

**Fatias verticais, na ordem, cada uma outside-in com teste primeiro:**

| # | Fatia | Entregável testável | RN |
|---|---|---|---|
| 1 | **`Dinheiro`, `Competencia`, `Percentual`** | rateio de centavos, HALF_EVEN, run-rate, dias decorridos com `Clock` | — |
| 2a | **Kernel de ingestão + leitor Nubank CSV + deduplicação** | `Normalizador`, `Parcela`, `ValorBrasileiro`, `ChaveDeDeduplicacao`, `Deduplicador`, `LeitorNubankCsv`, `Reconciliacao` | RN-01, RN-02, RN-02.1 |
| 2b | Leitores PDF (Itaú duas colunas, PicPay) + `POST /importacoes` | reconciliação fecha nas 3 faturas reais | RN-01, RN-02.1, Anexo C |
| 3 | Categorização por regra + triagem manual | `PATCH /transacoes/{id}` cria regra de aprendizado | RN-03, RN-05 |
| 4 | Piso humano + saldo de sobrevivência + **semáforo** | `GET /diagnostico` com faixa e ritmo | RN-08, RN-14 |
| 5 | **Viabilidade + queda de renda + fila de lacunas** | `GET /viabilidade`, `GET /lacunas` — *a pergunta central* | RN-16, RN-17 |
| 6 | Detector de recorrência | `GET /vampiros` | RN-07 |
| 7 | Compromissos futuros via `creditCardMetadata` | parcelas somem do variável futuro | RN-04 |
| 8 | **Fila de não identificados + padrões temporais** | `GET /transacoes/nao-identificadas`, `GET /padroes` | RN-12, RN-13 |
| 9 | **Motor de projeção** | `POST /cenarios/{id}/projetar` + property-based tests | RN-09 |
| 10 | **Plano de ajuste + migração para o débito** | `GET /plano-ajuste`, `GET /migracao` | RN-15, RN-18 |
| 10a | **Orçamento diário + provisão + reserva** | `GET /hoje`, `GET /eventos`, `GET /reserva` | RN-19, RN-20, RN-21 |
| 11 | Comparativo temporal + snapshots | `GET /comparativo` com run-rate | RN-06, RN-10 |
| 12 | **Adapter Pluggy + alertas** — *sem isto o sistema não é usado* | sync diário real das 3 contas, notificação matinal | RN-01, RN-22 |
| 13 | Dashboard dos Três Eus | front | RN-11 |

A fatia **5 subiu de prioridade**: com a renda caindo de R$ 14k para R$ 10k, saber se a meta de 25–30% é
sequer alcançável vale mais que qualquer refinamento de categorização. Ela depende só das fatias 3 e 4.

**Estratégia de teste:** JUnit 5 + AssertJ + Mockito no domínio (sem Spring) · `@SpringBootTest` + Testcontainers Postgres na integração · WireMock para a API do Pluggy · **jqwik** para as invariantes do motor de projeção · ArchUnit/Modulith para as fronteiras.

**Dashboard dos Três Eus** — o mapeamento de dados para a UI:
- **Eu do Passado** → `compromisso_futuro` + `divida`: "o que eu já assinei e ainda vou pagar".
- **Eu do Presente** → `diagnostico` do mês corrente + fila de triagem + vampiros pendentes de decisão.
- **Eu do Futuro** → `projecao_mes` + `marcos` + `plano_ajuste`, com o simulador de compra (RN-11) como a única porta de entrada de novas parcelas.

Acima das três colunas, quando o veredito de viabilidade (RN-16) não for `VIAVEL`, o dashboard abre com o
bloco de **queda estrutural de renda**. Ele contextualiza tudo o que vem abaixo.

---

## 11. O que preciso de você (a fatia 1 não depende disso, as seguintes sim)

1. **Renda líquida real.** R$ 10.000 é bruto. Quanto sobra depois de imposto, contador, INSS e pró-labore?
   E qual era o líquido dos R$ 14.000 até fevereiro? Os dois números alimentam a RN-16.1.
2. **Custo fixo atual, item a item** — aluguel, condomínio, energia, internet, telefone, academia, plano de
   saúde. É a lista que o teste de viabilidade ordena por peso na renda.
3. **As dívidas, uma a uma:** credor, saldo devedor hoje, taxa de juros mensal, valor da parcela, parcelas
   restantes. Sem a taxa, a data de quitação é ficção.
4. **Faturas dos últimos 3–6 meses** (Picpay, Nubank, Itaú) em PDF ou CSV — calibram os parsers, o baseline
   e os padrões temporais. **Se alguma trouxer horário da compra, é ouro para a RN-13.**
5. **Seus pisos humanos.** Comer fora você já me deu: 2x/mês. Faltam mercado, transporte, lazer, farmácia e
   as assinaturas que ficam.
6. **Qual é a vitória?** Quitar o mais rápido possível, ou quitar com folga e reserva construída em paralelo?
   Isso define a estratégia de amortização padrão.

---


## Anexo C — Formatos reais das faturas

Escrito a partir de arquivos verdadeiros: `fatura_07.pdf` e `fatura_08.pdf` (Itaú),
`Nubank_2026{07,08,09}12.csv`, `PicPay_Fatura_082026.pdf`. Os totais lidos reconciliam com o valor
impresso em cada fatura, com uma exceção documentada abaixo.

### C.1 Nubank — CSV

```
date,title,amount
2026-08-05,Orange Shopping - Parcela 9/10,"198,80"
2026-08-07,Pagamento recebido,"- 2.625,03"
```

| Aspecto | Comportamento |
|---|---|
| Data | ISO `YYYY-MM-DD`, sem hora → `hora_confiavel = false` |
| Valor | pt-BR entre aspas; **espaço depois do sinal** em negativos: `"- 2.625,03"` |
| **Sinal** | **invertido em relação à RN-01**: despesa é positiva. O leitor nega tudo na entrada |
| Parcela | no título: `- Parcela 9/12` |
| Pagamento | descrição exata `Pagamento recebido` |
| Período | o arquivo `NubankAAAAMMDD.csv` cobre o ciclo que fecha naquela data |

**Reconciliação:** a soma das despesas do arquivo de julho (R$ 2.290,36) é exatamente o
`Pagamento recebido` que aparece no arquivo de agosto. O formato se autovalida entre ciclos — use isso.
No ciclo de agosto sobra uma diferença de **R$ 0,04** contra o pagamento registrado; fica como aviso,
não como erro.

### C.2 Itaú — PDF

O PDF tem **duas tabelas lado a lado** e é aqui que quase todo parser ingênuo se perde.

**C.2.1 A fronteira das colunas não é o meio da página.** A coluna de valores da tabela da esquerda
invade a metade direita. Cortar em `largura/2` joga o valor da esquerda na coluna da direita e produz
lançamentos sem valor e valores órfãos. A fronteira correta é o **x0 do cabeçalho `DATA` da tabela
direita menos uma folga** (~351pt numa página de 595pt). Páginas com uma tabela só herdam a fronteira
da página anterior.

**C.2.2 A fatura tem seções, e nem todas somam no total.**

| Seção | Entra no total? |
|---|---|
| `Lançamentos: compras e saques` / `Lançamentos no cartão` | sim |
| `Lançamentos internacionais` | sim |
| `Lançamentos: produtos e serviços` (Pix no crédito, boletos, saques) | sim |
| `Pagamentos efetuados` | não — é crédito |
| **`Compras parceladas - próximas faturas`** | **não** — são compromissos futuros (RN-04) |
| `Simulação de Compras`, `Encargos cobrados`, ofertas de parcelamento | não |

Conferência na fatura de agosto: `2.302,86 + 38,18 + 3.286,69 = 5.627,73` = total impresso. Somar a
seção de próximas faturas inflaria o mês com R$ 944,85 que ainda nem foram cobrados — foi exatamente
esse o erro da primeira leitura.

**C.2.3 A cidade vem colada no nome do estabelecimento.** `SUPERMERCADO ARRUDAJOAO`,
`LaisDeAraujoJOAO PESSOA`, `UBER * PENDINGSAO PAULO`, `GRUPO FRUTOS DE GOIASJO`. O campo é truncado
em largura fixa, então o sufixo pode aparecer cortado (`JOAO PESSOA` → `JOAO PES` → `JO`). **Sem
desgrudar, o mesmo mercado vira dois estabelecimentos** e o detector de recorrência nunca acumula:
nos dados reais, `SUPERMERCADO ARRUDA` (10 ocorrências) ficou separado de `SUPERMERCADO ARRUDAJOAO`
(3), e nenhum dos dois cruzou o limiar da RN-07 sozinho. A cidade completa está na linha seguinte.

**C.2.4 A fatura traz a categoria do banco.** A partir da fatura de agosto, a linha seguinte a cada
lançamento tem `categoria cidade`: `restaurante`, `supermercado`, `transporte`, `lazer`, `vestuário`,
`saúde`, `serviços`, `outros`. É **dica de alta qualidade** para a RN-12 — mas continua sendo dica: a
classificação é nossa (RN-05). A fatura de julho, mais antiga, não tem esse campo.

**C.2.5 Parcela no sufixo `NN/NN`.** `NOHA SHOES - J 01/04`, `JIM.COM 551015 03/04`,
`AIRBNB * HM8MD 06/06`. Ambíguo com data à primeira vista, mas a leitura correta é parcela: o Itaú só
repete lançamento de mês anterior quando ele é parcelado, e `JIM.COM 551015` aparece como `03/04` em
julho e `04/04` em agosto — sequência, não data.

**C.2.6 Lacuna conhecida.** Os lançamentos internacionais ficam num sub-bloco de três colunas com a
linha `Dólar de Conversão` intercalada, e o leitor atual não os captura (R$ 24,28 em julho, R$ 38,18
em agosto — 0,6% da fatura). A reconciliação acusa a diferença, que é o comportamento correto:
**falta explícita, nunca silenciosa.**

### C.3 PicPay — PDF

Layout de coluna única, mais simples, mas com particularidades:

- **Um bloco de lançamentos por cartão** (`Picpay Card`, `Picpay Card final 1034`, `final 1030`), cada
  um com seu subtotal. Cartões adicionais entram no mesmo total.
- Seções `Transações Nacionais` e `Transações Internacionais`; a internacional tem duas colunas de
  valor (US$ e R$) e uma linha `Dólar: 8,04 / Câmbio do dia: R$ 5,43` que **não é lançamento**.
- Parcela colada: `AMAZON BR PARC10/10`, `MERCADOLIVRE*MPARC08/08`, `7ME IGREJA *IPARC02/03`.
- `Total parcelado - próximas faturas` é compromisso futuro, não gasto do mês.
- Encargos declarados na própria fatura: rotativo **14,90% a.m.**, parcelamento de fatura 14,70% a.m.,
  saque 17,80% a.m. — alimentam `divida.taxa_juros_mensal` sem precisar de estimativa.
- Lacuna conhecida: uma linha `APPLE.COM/BILL` de R$ 19,90 fica fora do bloco lido (R$ 19,90 de
  R$ 786,41). Acusada pela reconciliação.

### C.4 Regras que os arquivos reais mudaram

| Descoberta | Efeito na spec |
|---|---|
| Quatro cobranças idênticas de `SMARTBLUE JP` no mesmo dia, e pares de Uber com valor igual | Confirma o **ordinal** da RN-02 como obrigatório: sem ele o sistema apagaria despesa real |
| `PIX Nu Pagamentos SA` contém a palavra "Pagamentos" | **Identificar pagamento de fatura por substring é bug.** Tem que ser por seção + descrição exata. Casar por substring descartaria R$ 2.436,11 de gasto real |
| Cidade colada no estabelecimento (Itaú) | Nova etapa de normalização antes de qualquer agrupamento (RN-12) |
| Seção de parcelas futuras dentro da fatura | Reforça a separação `SecaoFatura.compoeTotal()`; alimenta RN-04 direto |
| Fatura declara o próprio total | **Nova regra: RN-02.1 — reconciliação obrigatória** (abaixo) |
| Comer fora distribuído por todos os dias (lift de fim de semana = 1,05) | Valida o limiar de 1,5 da RN-13: o padrão que o usuário intuía **não existe nos dados** |

### RN-02.1 — Reconciliação obrigatória contra o total declarado

Todo leitor que consiga extrair o total impresso na fatura **deve** conferir a soma do que leu contra
ele, com tolerância de R$ 0,01, e devolver o resultado no `ResultadoDaLeitura`.

Um parser que perde lançamentos não estoura: ele devolve um número menor e plausível, e o diagnóstico
inteiro passa a mentir para baixo — na direção confortável, que é a pior. Importação cuja reconciliação
não fecha entra marcada como `NAO_CONFIAVEL`, aparece na fila de revisão e **não alimenta baseline,
piso estimado nem projeção** até ser resolvida.

---

## Anexo B — Verificação numérica das regras

Os números dos critérios de aceite foram validados por simulação antes desta entrega:

| Verificação | Resultado |
|---|---|
| Déficit `7.400 − (4.100 + 2.300 + 1.380)` | `−380,00` → renda extra `400,00` (arredondada para cima em R$ 50) |
| Progresso de meta `(1240−620)/(1240−250)` | `62,6%` · variação `−50,0%` |
| Run-rate `210 / 3 × 30` (setembro) | `2.100,00` |
| Recorrência de streaming `(44,90−39,90)/42,40` | `0,118` ≤ 0,20 → detectada · custo anual `508,80` |
| Avalanche vs. bola de neve (A 8k@3,5% / B 2k@1,2%, disponível 1.500) | ambas quitam em **8 meses**; juros `1.191,48` vs `1.517,22` |
| Invariante "mais renda extra nunca atrasa a quitação" | verificada de R$ 0 a R$ 1.500 de extra |
| Busca binária de renda extra mínima (20k @ 3,5% a.m., 24 meses) | `R$ 1.245,46/mês` |
| Invariante "saldo de dívida nunca negativo" | verificada em todos os cenários acima |
| **v1.1** Viabilidade `8.200 − (5.400 + 1.900)` | economia máx. `900,00` · taxa `10,98%` · alvo de redução do fixo `1.150,00` |
| **v1.1** Queda de renda `14.000 → 10.000` | `−28,57%` · peso do fixo de `45,00%` para `63,00%` |
| **v1.1** Semáforo: meta 800, gasto 400 no dia 5 de 30 | consumo `0,50` · **ritmo `3,00`** · run-rate `2.400` → faixa projetada `PESSIMO` |
| **v1.1** Faixa global `(10.000 − 7.400)/10.000` | `26,00%` → `IDEAL` |
| **v1.1** Rampa linear `1.240 → 320` em 3 meses | cortes de `24,7% → 32,9% → 48,9%` — **viola o limite de 35%**; motivou a troca para rampa geométrica |
| **v1.1** Rampa geométrica `1.240 → 320`, fator 35% | `N_min = 4` · alvos `883,80 / 629,92 / 448,97 / 320,00` · corte constante de `28,7%` |
| **v1.1** Rampa geométrica `1.000 → 100`, pedido 2 meses | alongada para `6` meses · corte constante de `31,9%` |
| **v1.3** Viabilidade com os números reais: fixo `4.421,07` + piso `1.190,00` | custo mínimo `5.611,07` · economia máx. `4.388,93` · taxa máx. **`43,9%`** → **VIÁVEL** |
| **v1.3** Projeção 14 meses, variável `3.000` | set/26 `−1.316,58` (único mês negativo) · regime `+2.578,93/mês` = **`25,8%`** · acumulado `29.903,54` |
| **v1.3** RN-17.1 duplo lançamento: contador `500` declarado vs. Pix recorrente `517,67` | diferença `3,5%` ≤ 10% → é o mesmo pagamento; somar os dois inflava o fixo em `500,00/mês` |
| **v1.4** Custo medido da rolagem: `2.436,11 / 2.290,36 − 1` | `6,36% a.m.` = **`109,7% a.a.`** — mais barato que parcelar fatura (`306,6%` Itaú, `418,5%` PicPay) e que o rotativo (`469,5%`) |
| **v1.4** Fluxo com dívida real `7.952,24` | set/26 rola `2.565,29` (custo `163,25`) · out/26 rola `960,91` (custo `61,15`) · **nov/26 zera** · custo total `224,40` |
| **v1.4** Regime pós-quitação | sobra `2.578,93/mês` = `25,8%` da renda bruta — meta atingida sem cortar nada |
| **v2.0** Verba diária: `(3.000 − 250) / 30` | `R$ 91,67/dia` · mediana real do dia com gasto `R$ 82,97` · P75 `R$ 136,32` |
| **v2.0** Estabilidade do variável: jun `2.822,16` vs jul `2.986,99` | amplitude de `6%` — a premissa de R$ 3.000 se sustenta |
| **v2.0** Concentração: 8 dias de 56 | `38,5%` de todo o gasto variável — o mês é decidido por poucos dias |
| **v2.0** Capital de giro para 100% débito | `R$ 4.866,00` = um mês do que passa no cartão |
| **v2.0** Marcos do plano | última rolagem paga `nov/26` · ponte do aluguel `dez/26` · **100% débito `fev/27`** · reserva N1 `abr/27` · N2 `out/27` · **N3 `jun/28`** |

O script da verificação vai junto com o repositório como referência dos testes de propriedade da fatia 7.

---

## Anexo A — Fontes

- [Meu Pluggy — API de Open Finance gratuita para pessoa física](https://www.pluggy.ai/meu-pluggy)
- [Pluggy — Planos e preços](https://www.pluggy.ai/precos)
- [Pluggy Docs — Transaction](https://docs.pluggy.ai/docs/transactions)
- [Pluggy Docs — índice de endpoints (bills, transactions, accounts, items, webhooks)](https://docs.pluggy.ai/llms.txt)
- Planilha Termômetro (aula 02) — método de saldo diário e aba `Economia`
