package br.com.felipe.termometro.triagem.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * RN-05 — algoritmo do piso: dentro de uma categoria variável com piso definido, as transações do
 * mês são ordenadas por data; enquanto o acumulado ≤ piso, a transação é AZUL; a partir do momento
 * em que ultrapassa, é AMARELA por padrão.
 *
 * <p>A transação que cruza o piso recebe etiqueta AMARELA inteira (é ela quem "ultrapassa"), mas
 * seu valor é dividido logicamente entre azul e amarelo só para fins de agregado — exatamente como
 * a spec pede ("sem alterar o registro"). É por isso que este algoritmo devolve as duas partes
 * junto com a etiqueta: quem persiste usa só a etiqueta; quem soma totais usa as partes.
 *
 * <p>Empate de data (mais de uma transação no mesmo dia): a spec não define a ordem. Aqui, ordena
 * pelo {@code id} como desempate — decisão arbitrária, mas determinística, o que importa para um
 * algoritmo cujo resultado depende de ordem cumulativa.
 */
public final class AlgoritmoDoPiso {

    private AlgoritmoDoPiso() {
    }

    public static List<ResultadoDoPiso> aplicar(List<TransacaoClassificada> transacoesDaCategoria, Dinheiro piso) {
        Objects.requireNonNull(transacoesDaCategoria, "transações não podem ser nulas");
        Objects.requireNonNull(piso, "piso não pode ser nulo");

        List<TransacaoClassificada> ordenadas = transacoesDaCategoria.stream()
                .sorted(Comparator.comparing(TransacaoClassificada::data)
                        .thenComparing(t -> t.id().toString()))
                .toList();

        List<ResultadoDoPiso> resultado = new ArrayList<>(ordenadas.size());
        Dinheiro acumuladoAntes = Dinheiro.ZERO;
        for (TransacaoClassificada transacao : ordenadas) {
            Dinheiro valor = transacao.valor();
            Dinheiro acumuladoDepois = acumuladoAntes.somar(valor);

            if (acumuladoDepois.menorOuIgualA(piso)) {
                resultado.add(new ResultadoDoPiso(transacao.id(), Etiqueta.AZUL, valor, Dinheiro.ZERO));
            } else if (acumuladoAntes.maiorOuIgualA(piso)) {
                resultado.add(new ResultadoDoPiso(transacao.id(), Etiqueta.AMARELA, Dinheiro.ZERO, valor));
            } else {
                Dinheiro parteAzul = piso.subtrair(acumuladoAntes);
                Dinheiro parteAmarela = valor.subtrair(parteAzul);
                resultado.add(new ResultadoDoPiso(transacao.id(), Etiqueta.AMARELA, parteAzul, parteAmarela));
            }
            acumuladoAntes = acumuladoDepois;
        }
        return resultado;
    }
}
