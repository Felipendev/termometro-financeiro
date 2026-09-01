package br.com.felipe.termometro.ingestao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Converte o valor no formato brasileiro que vem nas faturas.
 *
 * <p>Cada banco escreve de um jeito. O Nubank exporta {@code "- 2.625,03"} — com espaço depois do
 * sinal — e o Itaú imprime {@code "-3.212,29"}. Um {@code replace(",", ".")} ingênuo passa nos dois
 * e falha em {@code "1.234,56"}, transformando mil e duzentos reais em um real e vinte e três.
 */
public final class ValorBrasileiro {

    private static final Pattern FORMATO =
            Pattern.compile("^[+-]?\\d{1,3}(?:\\.\\d{3})*,\\d{2}$|^[+-]?\\d+,\\d{2}$");
    private static final Pattern ESPACOS_E_MOEDA = Pattern.compile("[\\s\\u00a0\\u202f]|R\\$");

    private ValorBrasileiro() {
    }

    /** Converte, ou lança se o texto não for um valor em formato brasileiro. */
    public static Dinheiro converter(String texto) {
        return tentarConverter(texto).orElseThrow(() ->
                new IllegalArgumentException("valor fora do formato brasileiro: '" + texto + "'"));
    }

    public static Optional<Dinheiro> tentarConverter(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String limpo = ESPACOS_E_MOEDA.matcher(texto).replaceAll("");
        if (!FORMATO.matcher(limpo).matches()) {
            return Optional.empty();
        }
        String canonico = limpo.replace(".", "").replace(',', '.');
        return Optional.of(Dinheiro.de(new BigDecimal(canonico)));
    }

    public static boolean pareceValor(String texto) {
        return tentarConverter(texto).isPresent();
    }
}
