# MVP — automação diária

**Objetivo:** tirar o lançamento manual do caminho. Um app que exige digitação não sobrevive à
terceira semana, e app abandonado não muda comportamento nenhum.

O que o MVP entrega, e nada além disso:

> De manhã, uma notificação: **quanto dá para gastar hoje**, **como o mês está indo**, **qual o
> próximo marco e quando**. As transações chegam sozinhas.

As perguntas de análise — viabilidade, projeção, plano de ajuste, comparativo — **já estão
respondidas na planilha** (`termometro-felipe.xlsx`) e ficam fora do MVP. Elas voltam depois, quando
houver três meses de dados diários para alimentá-las.

## Fatias do MVP

| # | Fatia | Entregável | RN | Estado |
|---|---|---|---|---|
| **M0** | Shared kernel | `Dinheiro`, `Competencia`, `Percentual` | — | ✅ |
| **M1** | Kernel de ingestão + Nubank CSV + dedupe | `Normalizador`, `Parcela`, `Deduplicador`, `Reconciliacao` | RN-01, RN-02, RN-02.1 | ✅ |
| **M2** | **Motor da verba diária** | `CalculadoraDeVerbaDiaria`, `VerbaMensal`, `Evento` | RN-19, RN-20 | ✅ |
| **M3** | **Persistência** | Postgres + Flyway + entidades JPA + repositórios + Testcontainers | RN-02 | ✅ |
| **M4** | Adapter Pluggy | connect token, sync de contas e transações, webhook | RN-01, RN-22 | ✅ |
| **M5** | Categorização automática | regras + grupo de similaridade (`pg_trgm`) + fila de não identificados | RN-05, RN-12 | ✅ |
| **M6** | `GET /hoje` + notificação matinal | uma tela, uma notificação por dia | RN-19, RN-22 | ✅ |

Depois do M6 o sistema roda sozinho. As fatias de análise da spec (viabilidade, projeção, migração,
reserva) entram na sequência, já com dados reais acumulados.

**M6 entregue com escopo reduzido de RN-22:** só o disparo matinal (`GET /hoje` + Telegram às 7h). Os
outros quatro gatilhos da RN-22 (verba baixa, evento próximo, marco atingido, transação > R$100)
ficam para depois — o `CanalDeNotificacao` já existe como porta, então plugar os outros gatilhos é
reaproveitar o mesmo canal, não reconstruir.

## Bloqueios e dependências

- **M4:** credenciais do Meu Pluggy já configuradas em `PLUGGY_CLIENT_ID` e `PLUGGY_CLIENT_SECRET`.
  Falta conectar Itaú, Nubank e PicPay pelo widget.
- **M6:** a notificação matinal só sai de verdade com `TELEGRAM_BOT_TOKEN` e `TELEGRAM_CHAT_ID` no
  ambiente. Sem eles, o app sobe normal e o disparo das 7h é só logado e pulado — crie o bot com o
  `@BotFather` no Telegram (`/newbot`), pegue o token, mande qualquer mensagem para o bot e busque seu
  `chat_id` em `https://api.telegram.org/bot<TOKEN>/getUpdates`.
- **O build roda na sua máquina.** Maven Central está bloqueado no ambiente onde escrevo, então
  verifico o domínio com `javac` puro e você roda `mvn test`.

## Como verificar o que já existe

```bash
docker compose up -d      # Postgres para rodar a aplicação
mvn test                  # precisa de Docker ligado: os testes de integração sobem o próprio Postgres
```

Espera-se: `DinheiroTest`, `DinheiroPropriedades`, `CompetenciaTest`, `PercentualTest`,
`NormalizadorTest`, `ParcelaTest`, `ValorBrasileiroTest`, `ChaveDeDeduplicacaoTest`,
`DeduplicadorTest`, `LeitorNubankCsvTest`, `CalculadoraDeVerbaDiariaTest` — todos verdes.

Se algo quebrar, me mande a saída inteira do `mvn test`. É mais rápido que descrever o erro.

## O que o M2 já faz

```java
var verba = new VerbaMensal(Competencia.de(2026, 9), Dinheiro.de(3000), Dinheiro.de(250));
var eventos = List.of(
        Evento.previsto(LocalDate.of(2026, 9, 12), "Evento", Dinheiro.de(170)),
        Evento.previsto(LocalDate.of(2026, 9, 5), "Viagem de sábado", Dinheiro.de(110)));

var hoje = CalculadoraDeVerbaDiaria.padrao().calcular(verba, gastosDoMes, eventos, relogio);
System.out.println(hoje.mensagem());
```

Saída real, com 14 lançamentos de setembro e o relógio no dia 20:

> Você tem R$ 141,64 hoje: dá para 3 refeições fora de R$ 38,16 ou 3 mercados de R$ 43,79.
> Os eventos do mês passaram da provisão em R$ 30,00, então a verba diária já veio ajustada.

`verba de hoje R$ 141,64 · base R$ 90,67 · gasto R$ 1.162,00 · restam R$ 1.558,00 em 11 dias · IDEAL · ritmo 0,64`

As três decisões que sustentam esse número:

1. **A verba usa o gasto até ontem, não até hoje.** O que já se gastou hoje sai da decisão de hoje,
   mas não pode encolher a própria verba que ainda está sendo consumida.
2. **A provisão fica dentro da verba.** R$ 3.000 com R$ 250 de provisão dá R$ 2.750 de dia a dia —
   nunca R$ 3.250 de teto. Somar um colchão ao orçamento é a forma mais comum de fazê-lo mentir.
3. **Singular e plural são declarados, não derivados.** "refeição fora" não vira "refeição foras"
   com um `+ "s"` — erro de português numa notificação diária mina a confiança no resto do número.
