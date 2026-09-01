package br.com.felipe.termometro.ingestao.domain;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parcela de uma compra: {@code 9/12}. Gera os compromissos futuros da RN-04 — as
 * {@code 12 − 9 = 3} parcelas restantes já estão contratadas e vão cair independentemente
 * de qualquer decisão futura.
 */
public record Parcela(int numero, int total) {

    /** Acima disto não é parcelamento de varejo; é ruído de descrição. */
    public static final int MAXIMO_DE_PARCELAS = 48;

    private static final Pattern EXPLICITA =
            Pattern.compile("(?:PARCELA|PARC)\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUFIXO =
            Pattern.compile("(?<![\\d/])(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*$");

    public Parcela {
        if (numero < 1 || total < 2 || numero > total || total > MAXIMO_DE_PARCELAS) {
            throw new IllegalArgumentException("parcela inválida: " + numero + "/" + total);
        }
    }

    /**
     * Extrai a parcela da descrição. Os três bancos escrevem de jeitos diferentes:
     * {@code "Amazon - Parcela 9/12"} (Nubank), {@code "AMAZON BR PARC10/10"} (PicPay),
     * {@code "NOHA SHOES - J 01/04"} (Itaú).
     */
    public static Optional<Parcela> extrairDe(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return Optional.empty();
        }
        Matcher explicita = EXPLICITA.matcher(descricao);
        if (explicita.find()) {
            return montar(explicita.group(1), explicita.group(2));
        }
        Matcher sufixo = SUFIXO.matcher(descricao.strip());
        return sufixo.find() ? montar(sufixo.group(1), sufixo.group(2)) : Optional.empty();
    }

    private static Optional<Parcela> montar(String numero, String total) {
        int n = Integer.parseInt(numero);
        int t = Integer.parseInt(total);
        if (n < 1 || t < 2 || n > t || t > MAXIMO_DE_PARCELAS) {
            return Optional.empty();
        }
        return Optional.of(new Parcela(n, t));
    }

    public int restantes() {
        return total - numero;
    }

    public boolean ehUltima() {
        return numero == total;
    }

    @Override
    public String toString() {
        return numero + "/" + total;
    }
}
