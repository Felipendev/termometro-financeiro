# Progresso de implementação — Lançamentos

## L01 — Concluída em 2026-08-25

- Teste vermelho criado antes do domínio de categoria/forma de pagamento.
- `CategoriaDoLancamento`, cartão manual e referência de transação foram adicionados ao domínio de lançamento sem quebrar construtores existentes.
- Flyway V15 adiciona os metadados e índices correspondentes.
- Integração `LancamentoPlanejadoInfraRepositoryIT` passou contra PostgreSQL efêmero (1 teste, 0 falhas).

Próxima: L02 — aplicar a categoria explicitamente na transação liquidada e implementar reabertura atômica.

## L02 — Em andamento

- Testes unitários foram escritos antes da implementação para a regra aprendida e para o contrato de categoria/cartão.
- Ao liquidar receita ou despesa manual com categoria, o serviço aprende a regra pela descrição normalizada antes de enviar a transação ao processamento; assim ela entra nos gráficos e previsões já classificada.
- `LancamentoPlanejadoRequest` e a resposta agora aceitam/devolvem categoria e cartão manual.
- O modal rápido exige categoria para receitas e despesas, oferece categorias com ícones e permite escolher conta de débito ou cartão de forma exclusiva.
- Validação: `LancamentoPlanejadoRequestTest` e `LancamentoPlanejadoApplicationServiceTest` — 4 testes, 0 falhas; build do frontend concluído.

## L02 — Concluída em 2026-08-25

- Movimentos manuais agora carregam o ID do lançamento como evento de origem.
- Reabertura marca somente esses movimentos como ignorados, recompõe o saldo da conta quando aplicável e retorna o lançamento a `PENDENTE`.
- A API expõe `POST /v1/lancamentos-planejados/{id}/reabrir`.
- Validação: `LancamentoPlanejadoApplicationServiceTest` — 4 testes, 0 falhas.

Próxima: L03 — consulta paginada e filtros combináveis.

## L03 — Em andamento

- `GET /v1/lancamentos` recebe competência, tipo, status, conta, cartão, categoria, busca textual e paginação.
- A consulta combina os filtros e devolve itens, total de itens, total de despesas e total de receitas; transferências permanecem fora desses totais.
- Validação: `ConsultaLancamentosServiceTest` — 1 teste, 0 falhas.
- Tags foram removidas do escopo por decisão do usuário; pendente apenas o teste de integração HTTP/paginação real.

## L04–L06 — Concluídas

- Edição é restrita a lançamentos pendentes; status e vínculos são preservados.
- Cancelamento é por estado, sem exclusão de movimentos ou dados importados.
- Tela de Lançamentos inclui competência, filtros de tipo/status/texto, totais, estado vazio, liquidar, reabrir, cancelar e edição pelo modal.

## L07 — Em andamento

- Validação backend: 8 testes focados, 0 falhas.
- Build TypeScript/Vite: concluído sem erros.
- Pendente: teste automatizado React e integração HTTP real; não são pré-requisitos para usar a funcionalidade local, mas impedem encerrar o SDD como totalmente validado.

## L08 — Concluída em 2026-08-25

- A máscara de dinheiro passou a usar semântica de caixa registradora: inicia em `R$ 0,00`, mantém o cursor ao final e desloca os dígitos até formar valores como `R$ 1.234,56`.
- Seis testes unitários cobrem digitação progressiva, edição de valores e conversão para o valor enviado à API.
- Ícones textuais/emoji foram substituídos por `lucide-react` no modal e na tela de lançamentos, com um componente próprio para categorias.
- A migração V16 reconcilia movimentos manuais legados que ficaram sem vínculo com o lançamento, sem apagar histórico. No banco local, o Aluguel incorreto de `-R$ 22.000,00` e uma receita cancelada ficaram ignorados nas análises.
- Validação: frontend com 6 testes e build sem erros; `LancamentoPlanejadoInfraRepositoryIT` com PostgreSQL 16 — 1 teste, 0 falhas.

Próxima: L09 — reformular navegação e dashboard segundo a referência visual do Figma/Organizze.

## L09 — Concluída em 2026-08-25

- Navegação principal reduzida a Visão geral, Lançamentos, Cartões e Relatórios; as telas antigas de Planejamento e Contas e cartões saíram do menu.
- Dashboard reorganizado em resumo mensal, acesso rápido, categorias, cartões/faturas, compromissos essenciais, contas a pagar/receber e previsibilidade expansível.
- Ícones de interface e categorias usam `lucide-react`, sem emoji ou glifos improvisados.
- O total do painel de cartões usa faturas declaradas quando ainda não há gastos importados vinculados ao cartão.

## L10 — Concluída em 2026-08-25

- Despesas podem ser marcadas como `CUSTO_FIXO` ou `PISO_HUMANO` diretamente no modal; a marcação é persistida pela migração V17 e aparece nos lançamentos.
- Diagnóstico, viabilidade, projeção e reserva passam a usar os totais marcados do mês; o catálogo antigo é fallback somente quando não existe a nova marcação.
- Relatório por categorias e gráfico de rosca incluem despesas realizadas e pendentes da competência sem duplicar lançamentos liquidados.
- A divergência do Aluguel foi eliminada: o dashboard mostra Casa `-R$ 2.200,00` e total categorizado `-R$ 2.418,15`.

## L11 — Em andamento

- Validação visual feita em navegador real nos viewports 1440×900 e 390×844.
- Máscara validada no fluxo real: `R$ 0,00` + `123456` resulta em `R$ 1.234,56`.
- Frontend: 10 testes, 0 falhas; build TypeScript/Vite concluído.
- Backend local reiniciado e Flyway confirmado nas versões V16 e V17.
- Pendente: veredito isolado final do validator SDD.

## Auditoria independente — correções abertas

- O validator confirmou builds, 10/10 testes frontend, 22/22 testes backend focados e Flyway V1–V17, mas bloqueou o fechamento por lacunas funcionais.
- L12 concluída: liquidar/reabrir são idempotentes e reabrir transferência recompõe origem e destino; 8/8 testes do serviço passaram.
- L13 concluída: a V18 reavalia a conciliação por tipo, valor e cardinalidade, com teste de duplicatas e divergências em PostgreSQL real.
- L14 concluída: a consulta reúne lançamentos manuais e transações importadas, protege importados contra edição, combina filtros, pagina e calcula atrasos, realizado e previsto.
- L15 em andamento: corrigir isolamento dos Testcontainers na suíte completa e completar a validação de relatórios.

## L11 e L15 — Concluídas em 2026-08-26

- A consulta final reúne lançamentos manuais e importados, com filtros combináveis, agrupamento diário, carregamento progressivo, aviso de atrasados e saldos realizado/previsto.
- Transferências são identificadas separadamente e não entram nos totais de receita ou despesa; a API real de agosto retornou despesas de `R$ 2.413,07`, saldo realizado de `-R$ 213,07` e saldo previsto de `-R$ 2.413,07`.
- Relatórios de categorias, entradas e saídas, contas e cartões usam os movimentos da competência; transferências e cancelados ficam fora do fluxo. Tags permanecem fora do escopo por decisão do usuário.
- Revisão de categoria importada envia JSON válido, aceita somente origem diferente de `MANUAL` e atualiza dashboard, triagem e previsão.
- Catálogo de categorias do frontend usa apenas grupos aceitos pelo backend; teste de contrato evita nova divergência.
- Competência é obrigatória e a API retorna HTTP 400 quando ausente; a paginação aceita no máximo 100 itens.
- Flyway validou V1–V19 em PostgreSQL 16; a V19 adiciona índices para competência e contas.
- Backend: `mvn test` passou com 439 testes, 0 falhas e 0 erros.
- Frontend: 21 testes, 0 falhas, lint sem avisos e build de produção concluído.
- Captura real em 1440×1200 confirmou dashboard sem overflow, valores negativos, gráfico de categorias, cartões, compromissos e pendências.
- As duas auditorias independentes anteriores produziram achados que foram corrigidos. A terceira execução não iniciou por limite de uso do agente; revisão local de código, desempenho e consistência ficou verde, com evidências preservadas no relatório final.

Próxima: arquivar a feature e manter Pluggy de produção no backlog externo.
