/**
 * Persistência da classificação.
 *
 * <p><b>Sentido da dependência:</b> {@code classificacao} conhece {@code ingestao} — ela lê
 * {@code TransacaoBruta} para decidir o que a transação é. O contrário nunca: a ingestão não pode
 * saber que classificação existe, senão as duas viram uma só e o parser passa a ter opinião sobre
 * categoria.
 *
 * <p>Por isso a gravação usa {@code TransacaoJpaEntity.aplicaClassificacao(...)} com tipos
 * primitivos: o acoplamento fica no banco, que já é compartilhado, e não nos tipos de domínio.
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.classificacao.infra;
