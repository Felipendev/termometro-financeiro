# Termômetro Financeiro

Sistema de previsibilidade financeira pessoal: entende o padrão de gasto, separa o que é humano do que é
desperdício, diz **quando a dívida acaba** e se a meta de guardar 25–30% da renda é alcançável sem baixar o
padrão de vida.

O plano de execução está em **`MVP.md`** — leia esse primeiro. A especificação completa está em
`ESPEC-termometro-financeiro.md` (SDD v2.0). Toda regra citada no código
por código (`RN-08`, `RN-14`, …) está lá.

## Stack

Java 21 · Spring Boot 3.3 · Maven · PostgreSQL 16 · JUnit 5 · AssertJ · jqwik · Docker Compose

## Rodar

```bash
docker compose up -d          # Postgres (usado a partir da fatia 2)
mvn test                      # suíte completa
mvn spring-boot:run           # sobe a API em :8080
```

## Onde estamos

| Fatia | Descrição | Estado |
|---|---|---|
| **1** | **`Dinheiro`, `Competencia`, `Percentual`** | ✅ **feita** |
| **2a** | **Kernel de ingestão + leitor Nubank CSV + deduplicação** | ✅ **feita** |
| **M2** | **Motor da verba diária (`orcamento`)** | ✅ **feita** |
| M3 | Persistência (Postgres + Flyway + Testcontainers) | próxima |
| M4 | Adapter Pluggy — *depende da sua conta Meu Pluggy* | |
| 2b | Leitores PDF (Itaú, PicPay) — adiado, fora do MVP | |
| 3 | Categorização por regra + triagem 3 cores | |
| 4 | Piso humano + saldo de sobrevivência + semáforo | |
| 5 | Viabilidade do padrão de vida (a pergunta central) | |
| 6–13 | recorrências, compromissos, padrões, projeção, plano de ajuste, comparativo, Pluggy, dashboard | |

## Fatia 1 — o shared kernel

Três value objects imutáveis, sem Spring e sem JPA. É a base aritmética de todo o resto: se o rateio de
centavos estiver errado aqui, a data de quitação da dívida sai errada lá na frente e ninguém descobre.

### `Dinheiro`

`BigDecimal` com escala 2 e `HALF_EVEN`. **Nunca `double`.**

```java
Dinheiro saldo = Dinheiro.de("7400")
        .subtrair(Dinheiro.de("4100"))      // fixos
        .subtrair(Dinheiro.de("2300"))      // piso variável
        .subtrair(Dinheiro.de("1380"));     // serviço da dívida
// -R$ 380,00 → déficit

saldo.absoluto().arredondarParaCima(Dinheiro.de(50));   // RN-08 → R$ 400,00
```

O que merece atenção:

- **`ratear(int)` e `ratear(List<BigDecimal>)` nunca perdem centavo.** `Dinheiro.de(100).ratear(3)` devolve
  `[33,34 · 33,33 · 33,33]`, não três vezes 33,33 com um centavo evaporado. O rateio por pesos usa o método
  do maior resto e é o que aloca amortização entre dívidas (RN-09) e parte a transação que cruza o piso
  humano (RN-05).
- **A igualdade ignora a escala de origem:** `Dinheiro.de("10.5").equals(Dinheiro.de("10.50"))` é `true`,
  ao contrário de `BigDecimal.equals`.
- **`sobre(Dinheiro)` lança `ArithmeticException` se o total for zero** em vez de devolver 0% ou 100%.
  Consumo sobre meta zero é indefinido, e a regra de negócio precisa tratar isso (edge case 26).
- **É BRL-only.** Conversão de moeda acontece no adapter de ingestão; o domínio só vê reais.

### `Competencia`

Envolve `YearMonth` e carrega o **run-rate** — o guarda-corpo da RN-10 contra comparar mês parcial com mês
fechado.

```java
Competencia set = Competencia.parse("2026-09");
set.projetarRunRate(Dinheiro.de("210"), relogio);   // dia 3 → R$ 2.100,00
set.diasDecorridos(relogio);                        // 3
set.ehParcial(relogio);                             // true
```

- **Todo método sensível a "hoje" recebe um `Clock`.** Nada chama `LocalDate.now()` sem clock — regra que
  consulta o relógio do sistema não tem como ser testada em dia 3, dia 28 e dia 31, e são esses os dias em
  que ela erra.
- **`fatorRunRate` lança em competência futura** em vez de devolver zero. Extrapolar a partir de zero dia
  de dados não tem significado, e devolver zero faria o mês futuro parecer gasto zero.
- Mês fechado tem fator 1 — a chamada é segura em qualquer contexto.
- `quantidadeDeDias()` usa `lengthOfMonth()`; nunca 30 fixo (edge case 18).

### `Percentual`

Fração com 6 casas (`0,250000` = 25%). Existe para tirar percentual em `double` das comparações contra
limiares: taxa de economia (RN-14), lift (RN-13), taxa máxima de viabilidade (RN-16).

```java
Dinheiro economiaMaxima = renda.subtrair(custoMinimoDeVida);
economiaMaxima.sobre(renda).formatado(2);                       // "10,98%"
economiaMaxima.sobre(renda).menorQue(Percentual.dePontos("25")); // VIAVEL_PARCIALMENTE
```

`deFracao("0.25")` e `dePontos("25")` produzem o mesmo valor. As duas fábricas existem porque as duas
leituras aparecem no domínio, e deixar isso ambíguo gera erro de 100×.

## Testes

```
DinheiroTest             construção, aritmética, RN-08, rateio, apresentação
DinheiroPropriedades     invariantes em jqwik: o rateio preserva o total para qualquer
                         valor e qualquer número de partes; soma e subtração são inversas;
                         arredondar para cima nunca reduz e sempre cai num múltiplo
CompetenciaTest          calendário, navegação, RN-10 (run-rate), RN-14 (ritmo)
PercentualTest           conversões e os cenários de aceite de RN-14 e RN-16
```

Os testes de `PercentualTest.RegrasDeNegocio` e `CompetenciaTest.Ritmo` são os cenários Gherkin da
especificação, com os mesmos números — inclusive os do caso real da queda de R$ 14k para R$ 10k.

## Convenções

- **Sinal (RN-01):** saída negativa, entrada positiva, sempre.
- **JSON:** valores monetários e percentuais trafegam como **string**. Number em JSON vira `double` no
  front e volta com centavo errado.
- **Nulo:** valores monetários nunca são `null` — o default é `Dinheiro.ZERO`. `Optional` só em porta de
  saída, nunca em campo de entidade ou parâmetro. `@NullMarked` (JSpecify) por pacote.
- **Erro de programação lança cedo:** múltiplo inválido, partes ≤ 0, pesos negativos, divisão por zero.
  Regra de negócio violada vira RFC 7807 na camada de API (fatia 2 em diante).


## Fatia 2a — ingestão

Escrita contra os arquivos de verdade: 2 faturas Itaú, 3 extratos Nubank e 1 fatura PicPay. O
**Anexo C da especificação** documenta cada formato; o que segue são as decisões que os arquivos
forçaram.

### `ValorBrasileiro`

Cada banco escreve o valor de um jeito: `"14,96"`, `"-3.212,29"` (Itaú), `"- 2.625,03"` — com espaço
depois do sinal (Nubank), `"R$ 5.627,73"`. Um `replace(",", ".")` ingênuo passa em quase todos e
transforma **mil duzentos e trinta e quatro reais em um real e vinte e três**. Existe um teste só
para esse caso.

### `Normalizador`

A fatura do Itaú **cola a cidade no fim do nome do estabelecimento**: `SUPERMERCADO ARRUDAJOAO`,
`LaisDeAraujoJOAO PESSOA`, `UBER * PENDINGSAO PAULO`. O campo é truncado em largura fixa, então o
sufixo aparece cortado em qualquer tamanho.

Isso não é cosmético. Nos dados reais, `SUPERMERCADO ARRUDA` (10 ocorrências) ficou separado de
`SUPERMERCADO ARRUDAJOAO` (3) — **e nenhum dos dois cruzou o limiar de 3 ocorrências da RN-07
sozinho**. Sem desgrudar, o detector de recorrência simplesmente não vê o mercado onde você mais vai.

### `Deduplicador` e o ordinal

`Dinheiro.de(100).ratear(3)` não perde centavo; o dedupe não perde transação. As faturas têm **quatro
cobranças de `SMARTBLUE JP` no mesmo dia** e pares de corridas de Uber com valor idêntico no mesmo
dia. Sem o `ordinal` da RN-02, o hash colapsaria as quatro em uma e o sistema apagaria despesa real
— errando **para baixo**, que é o pior jeito de errar num diagnóstico de dívida.

Reimportar o mesmo arquivo é idempotente. O mesmo lançamento vindo por duas fontes mantém a de maior
confiança: `OPEN_FINANCE > OFX > CSV > PDF > MANUAL`.

### A armadilha do `PAGAMENTO`

`PIX Nu Pagamentos SA` contém a palavra "Pagamentos" e **não é** pagamento de fatura — é uma
transferência de R$ 2.436,11 feita no crédito, ou seja, gasto real (e, no caso, a fatura do Nubank
sendo paga com o cartão do Itaú). Identificar pagamento por `contains("PAGAMENTO")` apagaria isso.
A identificação é por seção + descrição exata, e existe um teste com esse nome.

### `Reconciliacao` — a rede de segurança

Todo leitor confere a soma do que leu contra o total impresso na fatura, com tolerância de um
centavo (RN-02.1). Um parser que perde lançamentos **não estoura**: devolve um número menor e
plausível, e o diagnóstico inteiro passa a mentir para baixo. Sem essa conferência, a primeira
leitura da fatura de julho teria entrado com R$ 4.465,28 no lugar dos R$ 4.091,57 impressos — e
ninguém saberia.

### `SecaoFatura`

A fatura do Itaú lista, no fim, as parcelas que só vencem nos próximos meses. Elas são
`compromisso_futuro` (RN-04), **não** gasto do mês: somá-las inflaria agosto em R$ 944,85 de dinheiro
que ainda nem foi cobrado.
