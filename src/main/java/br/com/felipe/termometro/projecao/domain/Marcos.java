package br.com.felipe.termometro.projecao.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * RN-09 — os três pontos que a UI mostra na linha do tempo, além do total de juros pagos.
 *
 * @param dataQuitacao primeiro mês com {@code Σ saldo = 0}; {@code null} se o horizonte
 *                     simulado terminou sem quitar (motor devolve {@code INVIAVEL})
 * @param primeiroRealGuardado primeiro mês com {@code reserva > 0}; {@code null} se nunca sobrou
 * @param reservaCompleta primeiro mês em que a reserva atinge o alvo
 *                        ({@code MinimoVariavel + ComprometidoFixo} vezes o número de meses
 *                        alvo); {@code null} se não atingida dentro do horizonte
 * @param jurosTotaisPagos soma de todo juros incorrido durante o horizonte simulado
 * @param mesesAteQuitacao meses corridos até {@code dataQuitacao} (o próprio mês de quitação
 *                         conta como 1); {@code null} junto com {@code dataQuitacao}
 */
public record Marcos(
        @Nullable Competencia dataQuitacao,
        @Nullable Competencia primeiroRealGuardado,
        @Nullable Competencia reservaCompleta,
        Dinheiro jurosTotaisPagos,
        @Nullable Integer mesesAteQuitacao) {

    public Marcos {
        Objects.requireNonNull(jurosTotaisPagos, "juros totais pagos não pode ser nulo");
        if (jurosTotaisPagos.ehNegativo()) {
            throw new IllegalArgumentException(
                    "juros totais pagos não pode ser negativo: " + jurosTotaisPagos);
        }
        if ((dataQuitacao == null) != (mesesAteQuitacao == null)) {
            throw new IllegalArgumentException(
                    "dataQuitacao e mesesAteQuitacao devem ser ambos nulos ou ambos preenchidos");
        }
        if (mesesAteQuitacao != null && mesesAteQuitacao <= 0) {
            throw new IllegalArgumentException(
                    "meses até quitação deve ser positivo, recebido: " + mesesAteQuitacao);
        }
    }
}
