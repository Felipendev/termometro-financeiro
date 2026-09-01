/**
 * Classificação — decide o que cada transação é.
 *
 * <p>Existe por uma razão prática: sem ela, a verba diária (RN-19) contaria o aluguel, o imposto e
 * a parcela do celular como gasto do dia. No dia 5, quando essas coisas caem, a verba de setembro
 * apareceria estourada — e o usuário desistiria do sistema na primeira semana, com razão.
 *
 * <p>Domínio puro: regras determinísticas, sem ML, sem banco, sem Spring. A escolha é deliberada —
 * uma classificação que erra precisa ser <b>explicável e corrigível</b>, e uma correção do usuário
 * precisa virar regra (RN-12), não peso de modelo.
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.classificacao.domain;
