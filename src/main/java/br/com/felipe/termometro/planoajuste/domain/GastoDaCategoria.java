package br.com.felipe.termometro.planoajuste.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * O histórico mensal de uma categoria variável, já separado por cor — a forma que o serviço de
 * aplicação entrega ao motor depois de agregar {@code TriagemRepository.buscaClassificadasDoMes}
 * dos últimos meses fechados (RN-05) com {@code CatalogoRepository.buscaPisoHumano} (RN-08).
 *
 * @param gastosVariaveisPorMes azul + amarelo de cada mês fechado — a soma que entra na rampa em
 *                              direção ao piso. VERDE nunca aparece aqui, é filtrado antes de
 *                              chegar neste record
 * @param gastosVermelhosPorMes só a parte vermelha de cada mês — some inteira no mês 1 (RN-15),
 *                              nunca rampa. Lista vazia quando a categoria não teve nenhuma
 *                              transação promovida a VERMELHA no período
 * @param piso                  piso humano da categoria; {@code null} quando não há piso
 *                              declarado (categoria hoje cai em NAO_TRIADA na triagem) — o motor
 *                              pula essa categoria e avisa, em vez de estimar um piso que a RN-15
 *                              não define para este cálculo
 */
public record GastoDaCategoria(
        String categoria, List<Dinheiro> gastosVariaveisPorMes, List<Dinheiro> gastosVermelhosPorMes,
        @Nullable Dinheiro piso) {

    public GastoDaCategoria {
        Objects.requireNonNull(categoria, "categoria não pode ser nula");
        if (categoria.isBlank()) {
            throw new IllegalArgumentException("categoria não pode ser vazia");
        }
        Objects.requireNonNull(gastosVariaveisPorMes, "gastosVariaveisPorMes não pode ser nulo");
        Objects.requireNonNull(gastosVermelhosPorMes, "gastosVermelhosPorMes não pode ser nulo");
        gastosVariaveisPorMes = List.copyOf(gastosVariaveisPorMes);
        gastosVermelhosPorMes = List.copyOf(gastosVermelhosPorMes);
        if (piso != null && piso.ehNegativo()) {
            throw new IllegalArgumentException("piso não pode ser negativo: " + piso);
        }
    }
}
