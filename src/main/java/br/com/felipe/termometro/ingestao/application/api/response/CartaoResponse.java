package br.com.felipe.termometro.ingestao.application.api.response;

import br.com.felipe.termometro.ingestao.domain.ContaBancaria;
import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;

/**
 * Gasto real de um cartão na competência — soma bruta das transações da seção CARTAO
 * (ver {@code SecaoFatura}), não depende de classificação/não-gasto/triagem terem rodado.
 *
 * <p><b>Puramente informativo: nunca some com custo fixo ou dívida em outro lugar do sistema.</b>
 * Parte do custo fixo já é paga pelo próprio cartão (ex.: item com {@code formaPagamento=CARTAO}
 * no catálogo) — somar os dois duplicaria o mesmo gasto.
 *
 * @param limite         {@code null} se o banco não informa limite para esta conta
 * @param percentualUsado {@code null} se {@code limite} for {@code null} ou zero
 */
public record CartaoResponse(
        String identificador,
        String nome,
        Dinheiro limite,
        Dinheiro gastoNoMes,
        Percentual percentualUsado) {

    public static CartaoResponse de(ContaBancaria conta, Dinheiro gastoNoMes) {
        Percentual percentualUsado = conta.limiteOpcional()
                .filter(limite -> !limite.ehZero())
                .map(limite -> Percentual.deValor(gastoNoMes, limite))
                .orElse(null);
        return new CartaoResponse(conta.identificador(), conta.nome(),
                conta.limiteOpcional().orElse(null), gastoNoMes, percentualUsado);
    }

    public static CartaoResponse de(Cartao cartao, Dinheiro gastoNoMes) {
        Percentual percentualUsado = cartao.limiteOpcional()
                .filter(limite -> !limite.ehZero())
                .map(limite -> Percentual.deValor(gastoNoMes, limite))
                .orElse(null);
        return new CartaoResponse(cartao.id().toString(), cartao.nome(), cartao.limite(),
                gastoNoMes, percentualUsado);
    }
}
