package br.com.felipe.termometro.planoajuste.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;

/**
 * A saída da RN-15: o plano completo, mês a mês, com os avisos de rampa alongada / categoria
 * excluída e a priorização das três ações de maior impacto.
 *
 * @param competenciaInicio       a partir de qual competência a rampa começa a valer
 * @param avisos                  mensagens não-fatais: rampa alongada, categoria sem piso,
 *                                categoria com piso zero — nunca silenciado (nenhum corte
 *                                silencioso, mesma disciplina de "sem caps silenciosos")
 * @param economiaMensalFinalTotal soma de {@code economiaMensalFinal} de todos os itens — quanto
 *                                 sobra por mês quando toda rampa chega ao fim
 */
public record PlanoDeAjuste(
        Competencia competenciaInicio, List<ItemDoPlano> itens, List<String> avisos,
        List<AcaoPrioritaria> acoesPrioritarias, Dinheiro economiaMensalFinalTotal) {

    public PlanoDeAjuste {
        Objects.requireNonNull(competenciaInicio, "competenciaInicio não pode ser nula");
        Objects.requireNonNull(itens, "itens não pode ser nulo");
        Objects.requireNonNull(avisos, "avisos não pode ser nulo");
        Objects.requireNonNull(acoesPrioritarias, "acoesPrioritarias não pode ser nulo");
        Objects.requireNonNull(economiaMensalFinalTotal, "economiaMensalFinalTotal não pode ser nulo");
        itens = List.copyOf(itens);
        avisos = List.copyOf(avisos);
        acoesPrioritarias = List.copyOf(acoesPrioritarias);
    }
}
