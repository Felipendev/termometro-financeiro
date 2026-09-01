/**
 * Persistência do orçamento.
 *
 * <p><b>Por que existe entidade JPA separada do domínio.</b> No produdoro a classe de domínio
 * <i>é</i> o documento Mongo. Aqui não dá: {@code VerbaMensal} e {@code Evento} são
 * {@code record}s — imutáveis, sem construtor vazio, com invariantes validadas na construção — e
 * JPA exige exatamente o contrário. Em vez de degradar o domínio para agradar o ORM, a tradução
 * fica aqui, em {@code infra}, que é o lugar dela. O domínio continua testável sem banco, sem
 * Spring e sem contexto.
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.orcamento.infra;
