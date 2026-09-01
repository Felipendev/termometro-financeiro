/**
 * Persistência da triagem.
 *
 * <p><b>Sentido da dependência:</b> {@code triagem} conhece {@code ingestao} (lê e escreve
 * {@code TransacaoJpaEntity}) e {@code classificacao.domain} (a etiqueta só existe depois que a
 * transação tem {@code categoria} e {@code natureza}). O contrário nunca — nem ingestão nem
 * classificação sabem que triagem existe.
 *
 * <p>Mesma técnica de {@code classificacao.infra}: a gravação usa
 * {@code TransacaoJpaEntity.aplicaEtiqueta(String)}, primitivo de propósito — o acoplamento fica
 * no banco compartilhado, não nos tipos de domínio.
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.triagem.infra;
