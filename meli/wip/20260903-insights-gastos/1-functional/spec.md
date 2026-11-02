# Especificação funcional — Insights de gastos

## Objetivo

Logo abaixo do gráfico de bolinhas “Seus gastos hoje e as referências”, apresentar dicas baseadas exclusivamente nos gastos e limites reais do usuário. A seção deve responder: qual limite está sendo respeitado ou ultrapassado, qual é o valor que falta ajustar e onde estão os gastos que compõem essa oportunidade.

## Premissas confirmadas no código atual

- O gráfico já usa lançamentos manuais e importados da competência como fonte prioritária; só recorre ao catálogo quando não há gasto real classificado.
- Moradia, Alimentação e Assinaturas têm referências calibradas de bom, ideal e ruim. Os demais grupos não receberão limite artificial.
- Ao abrir uma linha do gráfico, o usuário já vê os lançamentos que formam aquele valor.
- O plano de ajuste do dashboard já prioriza categorias variáveis pelo histórico dos três meses fechados, mas a dica não deve afirmar uma economia sem mostrar a base do cálculo.

## Histórias e critérios

### IG-01 — Entender a situação de cada limite

Como usuário, quero ver, abaixo do gráfico, uma leitura curta dos grupos que possuem referência, para saber se estou dentro do ideal, próximo ao limite ou acima dele.

- Cada dica identifica o grupo, o gasto atual e a referência utilizada (ideal, bom ou ruim).
- Até o limite ideal: apresentar reforço positivo, sem sugerir corte.
- Entre ideal e bom: informar quanto falta reduzir para chegar ao ideal, sem linguagem de alarme.
- Acima do bom: informar quanto falta reduzir para voltar ao limite bom.
- Acima do ponto ruim: marcar como prioritário e informar o valor para voltar ao bom.
- Não apresentar dica de limite para grupo sem referência calibrada.

### IG-02 — Encontrar onde economizar

Como usuário, quero saber que gastos compõem uma dica de excesso, para decidir conscientemente onde reduzir.

- Toda dica de redução traz uma ação “Ver gastos do grupo”.
- A ação abre ou direciona para o detalhamento já existente daquela linha do gráfico, sem duplicar nem inventar transações.
- Se houver mais de uma oportunidade, exibir no máximo três, ordenadas pela maior redução necessária para chegar ao limite aplicável.
- Cada sugestão explica a conta em reais: gasto atual menos a referência-alvo.

### IG-03 — Não criar pressão com dados insuficientes

Como usuário, quero que o sistema seja honesto quando meus dados não permitem uma recomendação.

- Sem gastos ou sem grupo com referência: não renderizar a seção de dicas.
- Quando a comparação usa o catálogo por falta de gasto real, identificar que a leitura é uma estimativa e não uma recomendação sobre o mês realizado.
- Valores cancelados, transferências e itens classificados como `NAO_E_GASTO` continuam fora do cálculo, como já ocorre no gráfico.

### IG-04 — Usar com teclado e em telas pequenas

Como usuário, quero abrir o detalhamento das dicas em desktop ou celular.

- Botões têm rótulos claros para leitor de tela e mantêm foco visível.
- O bloco se adapta a uma coluna estreita sem esconder valor, status ou ação.
- As cores reforçam o estado, mas texto e valor tornam a mensagem compreensível sem cor.

## Checklist de aceite

- [ ] As dicas usam a mesma competência, renda de referência e itens do gráfico de bolinhas.
- [ ] O valor a reduzir é matematicamente consistente com a referência exibida.
- [ ] Há no máximo três oportunidades, priorizadas pelo impacto financeiro.
- [ ] Não há meta, limite ou gasto inventado para categorias sem referência.
- [ ] Estados vazio, estimativa por catálogo, desktop, mobile e teclado são cobertos por testes.
