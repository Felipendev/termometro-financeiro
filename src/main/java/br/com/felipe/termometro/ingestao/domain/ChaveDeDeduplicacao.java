package br.com.felipe.termometro.ingestao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Chave de deduplicação da RN-02.
 *
 * <p><b>O ordinal não é preciosismo.</b> Nos dados reais existem quatro cobranças de
 * {@code SMARTBLUE JP} no mesmo dia, entre R$ 14,93 e R$ 14,98, e dois pares de corridas de
 * Uber com valor idêntico no mesmo dia. Sem o ordinal, o hash colapsaria transações legítimas
 * e o sistema apagaria despesa real — errando para baixo, que é o pior jeito de errar num
 * diagnóstico de dívida.
 */
public final class ChaveDeDeduplicacao {

    private static final String ALGORITMO = "SHA-256";
    private static final char SEPARADOR = '|';

    private ChaveDeDeduplicacao() {
    }

    public static String calcular(String identificadorDaConta, LocalDate data, Dinheiro valor,
                                  String descricao, int ordinal) {
        Objects.requireNonNull(identificadorDaConta, "conta não pode ser nula");
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal não pode ser negativo: " + ordinal);
        }
        String material = identificadorDaConta + SEPARADOR + data + SEPARADOR + valor.centavos()
                + SEPARADOR + Normalizador.chaveDeDeduplicacao(descricao) + SEPARADOR + ordinal;
        return HexFormat.of().formatHex(digerir(material));
    }

    public static String calcular(String identificadorDaConta, TransacaoBruta transacao) {
        return calcular(identificadorDaConta, transacao.data(), transacao.valor(),
                transacao.descricaoOriginal(), transacao.ordinal());
    }

    private static byte[] digerir(String material) {
        try {
            return MessageDigest.getInstance(ALGORITMO)
                    .digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITMO + " deveria existir em toda JVM", e);
        }
    }
}
