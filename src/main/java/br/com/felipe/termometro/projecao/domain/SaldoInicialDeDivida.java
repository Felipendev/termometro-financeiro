package br.com.felipe.termometro.projecao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;

/**
 * Ponto de partida de uma dívida na simulação: quanto se deve hoje e a que taxa mensal os
 * juros correm. É a entrada do motor — quem monta essa lista (empréstimo do catálogo, rotativo
 * de cartão, o que for) é responsabilidade de quem chama {@link MotorDeProjecao}.
 *
 * @param nome apenas identificação para o relatório mês a mês; o motor não decide por nome
 * @param saldoDevedor nunca negativo — dívida quitada não entra na lista, ou entra com
 *                      {@link Dinheiro#ZERO}
 * @param taxaJurosMensal taxa efetiva ao mês (edge case 8: sem taxa informada, quem monta a
 *                        lista decide o default e marca a estimativa — o motor não assume nada)
 */
public record SaldoInicialDeDivida(String nome, Dinheiro saldoDevedor, Percentual taxaJurosMensal) {

    public SaldoInicialDeDivida {
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(saldoDevedor, "saldo devedor não pode ser nulo");
        Objects.requireNonNull(taxaJurosMensal, "taxa de juros mensal não pode ser nula");
        if (nome.isBlank()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        if (saldoDevedor.ehNegativo()) {
            throw new IllegalArgumentException("saldo devedor não pode ser negativo: " + saldoDevedor);
        }
        if (taxaJurosMensal.ehNegativo()) {
            throw new IllegalArgumentException("taxa de juros mensal não pode ser negativa: " + taxaJurosMensal);
        }
    }
}
