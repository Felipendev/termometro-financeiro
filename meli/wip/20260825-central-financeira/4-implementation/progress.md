# Progresso de implementação

## T01 - Concluída em 2026-08-25

- Teste vermelho criado: `ImportacaoProcessadaServiceTest` (ausência do orquestrador e do resultado).
- Implementado o mínimo: `ImportacaoProcessadaService` e `ResultadoDoProcessamentoImportacao`.
- O processamento só executa para transações novas e percorre as competências em ordem cronológica.
- Próxima tarefa: T02 - integrar o orquestrador na importação e expor o resumo ao frontend.

## T02 - Concluída em 2026-08-25

- Teste vermelho atualizado no controlador de importação antes da implementação.
- A ingestão agora expõe separadamente os lançamentos efetivamente novos, mantendo a resposta de leitura compatível.
- O controlador processa somente esse subconjunto e devolve as competências processadas ao frontend.
- Próxima tarefa: T03 - contas manuais.

## T03/T04 - Em andamento em 2026-08-25

- Foram criados os domínios, migrações Flyway e endpoints de contas manuais e lançamentos planejados.
- A liquidação de despesa/receita grava somente as transações novas e aciona o mesmo pipeline de classificação, conciliação e triagem da importação.
- Transferência gera uma saída e uma entrada de mesmo valor, deixando a conciliação de não-gasto como proteção contra dupla contagem.
- Teste unitário adicionado para os dois fluxos de liquidação. Ainda faltam testes de infraestrutura/controlador e a atualização automática de saldo das contas manuais antes de concluir estas tarefas.

## Infraestrutura de testes - Atualizada em 2026-08-25

- O Docker Desktop/Engine 29 estava saudável, mas o Testcontainers 1.19.8 herdado pelo Spring Boot 3.3.5 não negociava a API 1.55 do daemon.
- Dependências de teste atualizadas para Testcontainers 2.0.5 por meio do BOM oficial.
- A integração `CartaoInfraRepositoryIT` passou contra um PostgreSQL descartável (4 testes, sem skips), confirmando a conexão e as migrações.
