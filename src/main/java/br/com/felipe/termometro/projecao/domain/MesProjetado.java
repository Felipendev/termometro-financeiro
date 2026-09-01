package br.com.felipe.termometro.projecao.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * RN-09 — a foto de um mês dentro da simulação: quanto entrou, quanto já estava comprometido,
 * quanto sobrou, quanto foi de juros e amortização, e o saldo acumulado de dívida e reserva ao
 * final do mês.
 *
 * @param disponivel pode ser negativo — é o gatilho de {@code apertado}
 * @param juros soma dos juros de todas as dívidas neste mês, sobre o saldo do início do mês
 * @param amortizacao zero em mês apertado (edge case 12); nunca negativa
 * @param reservaAcumulada saldo de reserva ao final deste mês — monotonicamente não decrescente
 *                          enquanto não há saque (invariante RN-09)
 * @param saldoDividaFimDoMes soma do saldo de todas as dívidas ao final do mês; nunca negativa
 * @param apertado {@code true} quando {@code disponivel <= 0} — amortização zerada e a dívida
 *                 cresceu pelos juros do mês (edge case 12)
 */
public record MesProjetado(
        Competencia competencia,
        Dinheiro entrada,
        Dinheiro saidaFixa,
        Dinheiro saidaVariavel,
        Dinheiro disponivel,
        Dinheiro juros,
        Dinheiro amortizacao,
        Dinheiro reservaAcumulada,
        Dinheiro saldoDividaFimDoMes,
        boolean apertado) {

    public MesProjetado {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(entrada, "entrada não pode ser nula");
        Objects.requireNonNull(saidaFixa, "saída fixa não pode ser nula");
        Objects.requireNonNull(saidaVariavel, "saída variável não pode ser nula");
        Objects.requireNonNull(disponivel, "disponível não pode ser nulo");
        Objects.requireNonNull(juros, "juros não pode ser nulo");
        Objects.requireNonNull(amortizacao, "amortização não pode ser nula");
        Objects.requireNonNull(reservaAcumulada, "reserva acumulada não pode ser nula");
        Objects.requireNonNull(saldoDividaFimDoMes, "saldo de dívida ao fim do mês não pode ser nulo");
        if (amortizacao.ehNegativo()) {
            throw new IllegalArgumentException("amortização não pode ser negativa: " + amortizacao);
        }
        if (saldoDividaFimDoMes.ehNegativo()) {
            throw new IllegalArgumentException(
                    "saldo de dívida não pode ser negativo: " + saldoDividaFimDoMes);
        }
    }
}
