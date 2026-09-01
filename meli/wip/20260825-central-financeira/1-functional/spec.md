# Especificação funcional - Central financeira

## Objetivo

Transformar a tela inicial em uma central financeira rápida, inspirada no fluxo de visão geral do Organizze: importar ou registrar algo deve atualizar, sem passos técnicos extras, as análises, categorias, cartões e previsibilidade.

## Escopo

Inclui processamento automático após importação, contas manuais, lançamentos manuais, transferências, contas a pagar/receber e a nova visão inicial. Não inclui acesso de produção à Pluggy, pagamentos reais, Pix ou multiusuário.

## Histórias e critérios de aceite

### F-01 - Importação que vira análise

Como usuário, ao importar CSV ou PDF, quero ver o resultado imediatamente na competência correta para confiar que o arquivo foi aproveitado.

- A importação processa todas as competências encontradas no arquivo: classifica, concilia não-gasto, gera compromissos de parcelas e tria.
- O retorno informa lançamentos novos, competências processadas, categorias pendentes de revisão e se a reconciliação da fatura fechou.
- A home atualiza sem recarregar a página; se o mês visível não foi afetado, explica qual competência contém os lançamentos.
- Uma reimportação idêntica não duplica dados nem reprocessa sem necessidade.

### F-02 - Resumo operacional do mês

Como usuário, quero ver rapidamente receitas, despesas e saldo consolidado para decidir o que posso gastar.

- A home mostra saudação, competência selecionada, receitas, despesas e saldo de todas as contas ativas.
- Valores podem ser ocultados e reexibidos na mesma sessão.
- O total de despesas não duplica pagamento de fatura ou transferência entre contas próprias.

### F-03 - Acesso rápido

Como usuário, quero registrar uma despesa, receita, transferência ou arquivo pela home.

- Os quatro atalhos abrem formulários claros e acessíveis sem navegar para Configurações.
- Despesa e receita podem ser únicas ou agendadas; transferência cria débito e crédito vinculados e não entra em gasto/receita do mês.
- Importar aceita Nubank CSV e faturas PDF Itaú/PicPay.
- O modal de despesa/receita permite selecionar categoria, conta ou cartão e oferece as ações "Salvar" e "Salvar e criar outra".
- A categoria selecionada é aplicada ao lançamento recém-criado antes da triagem; portanto ele aparece no resumo e no gráfico mesmo quando não há regra automática para sua descrição.

### F-04 - Contas e cartões

Como usuário, quero ver cada conta e cartão com seu contexto atual.

- Contas mostram nome, tipo, saldo e origem (manual ou banco).
- Cartões mostram nome, limite, limite disponível, fatura atual e vencimento quando conhecidos.
- Um cartão manual pode ser associado ao identificador usado na importação; seus gastos importados aparecem no mesmo cartão, sem soma duplicada com a fatura declarada.

### F-05 - Contas a pagar e receber

Como usuário, quero saber minhas pendências por vencimento.

- Despesas agendadas aparecem em Atrasadas, Vence hoje ou Próximas, ordenadas por vencimento.
- Receitas agendadas aparecem em Contas a receber e apresentam estado vazio útil quando não houver itens.
- Marcar um item como pago/recebido cria o lançamento financeiro correspondente uma única vez.

### F-06 - Gastos e orçamento

Como usuário, quero identificar rapidamente onde estou gastando e quanto do limite mensal já consumi.

- Maiores gastos usam as categorias existentes do backend, com rosca e percentuais.
- Limite de gastos compara despesas variáveis classificadas com a verba mensal; sem orçamento, mostra instrução para configurá-lo.
- A análise detalhada e a projeção continuam acessíveis, mas não dominam a primeira dobra da home.

## Estados vazios e falhas

- Sem conta: orientar o cadastro de uma conta manual.
- Sem dados no mês: mostrar atalhos para registrar ou importar.
- Importação que não reconcilia: manter dados importados marcados e exibir aviso inequívoco.
- Falha de processamento: retornar a importação salva e permitir repetir apenas o processamento.

## Checklist funcional de aceite

- [ ] Um CSV/PDF importado aparece em sua competência e no gráfico sem acionar endpoint manual.
- [ ] Pagamentos e transferências não inflacionam despesas.
- [ ] Cartão associado exibe gasto importado, fatura e limite sem duplicidade.
- [ ] Uma despesa agendada muda de Próximas para Atrasadas/Vence hoje conforme a data.
- [ ] Marcar pendência como paga é idempotente.
- [ ] Home é navegável por teclado, responsiva e apresenta estados vazios.
