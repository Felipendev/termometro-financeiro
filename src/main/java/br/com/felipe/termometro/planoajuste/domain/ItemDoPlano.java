package br.com.felipe.termometro.planoajuste.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;

/**
 * Uma linha do plano de ajuste (RN-15): o que uma categoria precisa fazer, numa das duas formas
 * — {@link TipoDeItem#RAMPA_VARIAVEL rampar} a parte azul+amarela até o piso, ou
 * {@link TipoDeItem#ZERAR_VERMELHO zerar} a parte vermelha já no mês 1.
 *
 * @param dor                  o que a RN-15 usa para priorizar: VERMELHA = 1 (mais fácil, é
 *                             impulso já reconhecido pelo próprio usuário), AMARELA = 2 (mais
 *                             difícil, é hábito recorrente). AZUL nunca vira item — não entra no
 *                             plano
 * @param economiaMensalFinal  {@code valorAtual - alvoFinal} — quanto sobra por mês quando a
 *                             rampa (ou o corte) chega ao fim
 */
public record ItemDoPlano(
        String categoria, TipoDeItem tipo, Dinheiro valorAtual, Dinheiro alvoFinal,
        List<AlvoMensal> alvosMensais, boolean rampaAlongada, int dor, Dinheiro economiaMensalFinal) {

    public ItemDoPlano {
        Objects.requireNonNull(categoria, "categoria não pode ser nula");
        Objects.requireNonNull(tipo, "tipo não pode ser nulo");
        Objects.requireNonNull(valorAtual, "valorAtual não pode ser nulo");
        Objects.requireNonNull(alvoFinal, "alvoFinal não pode ser nulo");
        Objects.requireNonNull(alvosMensais, "alvosMensais não pode ser nulo");
        Objects.requireNonNull(economiaMensalFinal, "economiaMensalFinal não pode ser nulo");
        if (categoria.isBlank()) {
            throw new IllegalArgumentException("categoria não pode ser vazia");
        }
        alvosMensais = List.copyOf(alvosMensais);
        if (alvosMensais.isEmpty()) {
            throw new IllegalArgumentException("alvosMensais não pode ser vazio");
        }
        if (dor < 1) {
            throw new IllegalArgumentException("dor deve ser >= 1: " + dor);
        }
    }

    public enum TipoDeItem {
        /** Rampa geométrica da soma azul+amarelo até o piso humano. */
        RAMPA_VARIAVEL,
        /** Zera a parte vermelha da categoria já no mês 1 — sem rampa (RN-15). */
        ZERAR_VERMELHO
    }
}
