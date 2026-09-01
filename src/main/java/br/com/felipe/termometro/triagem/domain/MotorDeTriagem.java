package br.com.felipe.termometro.triagem.domain;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Decide a etiqueta de cada transação já classificada do mês (RN-05), e resume os totais por
 * categoria para leitura.
 *
 * <p>Dispatch por natureza: {@code FIXO} é sempre AZUL (indispensável, spec explícita); {@code
 * NAO_E_GASTO} é sempre VERDE (amortização, aporte — nunca entra em corte); {@code VARIAVEL} com
 * piso definido passa pelo {@link AlgoritmoDoPiso}; {@code VARIAVEL} sem piso vira {@code
 * NAO_TRIADA} — o sistema não tem base para decidir sozinho, e a spec não define uma estimativa
 * para este caso (diferente da RN-08, que estima piso ausente via percentil 25 para o agregado do
 * diagnóstico — trazer essa estimativa para a triagem por transação é uma decisão maior, deixada
 * de fora desta fatia).
 *
 * <p>Uma promoção manual para VERMELHA nunca é sobrescrita: é a única etiqueta que o algoritmo
 * automático jamais atribui, o que a torna um marcador seguro de "isto foi decisão humana".
 */
public final class MotorDeTriagem {

    private MotorDeTriagem() {
    }

    public static Map<UUID, Etiqueta> triar(List<TransacaoClassificada> doMes,
                                            Map<String, Dinheiro> pisoPorCategoria) {
        Objects.requireNonNull(doMes, "transações não podem ser nulas");
        Objects.requireNonNull(pisoPorCategoria, "piso por categoria não pode ser nulo");

        Map<UUID, Etiqueta> resultado = new HashMap<>(doMes.size());

        Map<String, List<TransacaoClassificada>> porCategoria = doMes.stream()
                .filter(t -> !t.promovidaManualmenteParaVermelha())
                .collect(Collectors.groupingBy(TransacaoClassificada::categoria));

        for (Map.Entry<String, List<TransacaoClassificada>> entrada : porCategoria.entrySet()) {
            List<TransacaoClassificada> transacoes = entrada.getValue();
            Natureza natureza = naturezaUnica(entrada.getKey(), transacoes);
            switch (natureza) {
                case FIXO -> transacoes.forEach(t -> resultado.put(t.id(), Etiqueta.AZUL));
                case NAO_E_GASTO -> transacoes.forEach(t -> resultado.put(t.id(), Etiqueta.VERDE));
                case VARIAVEL -> {
                    Dinheiro piso = pisoPorCategoria.get(entrada.getKey());
                    if (piso == null) {
                        transacoes.forEach(t -> resultado.put(t.id(), Etiqueta.NAO_TRIADA));
                    } else {
                        AlgoritmoDoPiso.aplicar(transacoes, piso)
                                .forEach(r -> resultado.put(r.transacaoId(), r.etiqueta()));
                    }
                }
            }
        }

        doMes.stream().filter(TransacaoClassificada::promovidaManualmenteParaVermelha)
                .forEach(t -> resultado.put(t.id(), Etiqueta.VERMELHA));

        return resultado;
    }

    public static List<ResumoDeCategoria> resumir(List<TransacaoClassificada> doMes,
                                                   Map<String, Dinheiro> pisoPorCategoria) {
        Objects.requireNonNull(doMes, "transações não podem ser nulas");
        Objects.requireNonNull(pisoPorCategoria, "piso por categoria não pode ser nulo");

        Map<String, List<TransacaoClassificada>> porCategoria = doMes.stream()
                .collect(Collectors.groupingBy(TransacaoClassificada::categoria));

        List<ResumoDeCategoria> resumo = new ArrayList<>(porCategoria.size());
        for (Map.Entry<String, List<TransacaoClassificada>> entrada : porCategoria.entrySet()) {
            String categoria = entrada.getKey();
            List<TransacaoClassificada> transacoes = entrada.getValue();
            Natureza natureza = naturezaUnica(categoria, transacoes);

            Dinheiro azul = Dinheiro.ZERO;
            Dinheiro amarelo = Dinheiro.ZERO;
            Dinheiro vermelho = Dinheiro.ZERO;
            Dinheiro verde = Dinheiro.ZERO;
            Dinheiro naoTriada = Dinheiro.ZERO;

            switch (natureza) {
                case FIXO -> azul = Dinheiro.somaDe(valores(transacoes));
                case NAO_E_GASTO -> verde = Dinheiro.somaDe(valores(transacoes));
                case VARIAVEL -> {
                    Dinheiro piso = pisoPorCategoria.get(categoria);
                    if (piso == null) {
                        naoTriada = Dinheiro.somaDe(valores(transacoes));
                    } else {
                        Map<UUID, TransacaoClassificada> porId = transacoes.stream()
                                .collect(Collectors.toMap(TransacaoClassificada::id, t -> t));
                        for (ResultadoDoPiso r : AlgoritmoDoPiso.aplicar(transacoes, piso)) {
                            TransacaoClassificada original = porId.get(r.transacaoId());
                            if (original.promovidaManualmenteParaVermelha()) {
                                // a promoção move o valor inteiro da transação para vermelho, mesmo
                                // que uma parte tivesse caído em azul pelo algoritmo — uma vez
                                // VERMELHA, a transação deixa de ser "a parte azul e a parte
                                // amarela de uma AMARELA" e passa a ser uma coisa só
                                vermelho = vermelho.somar(r.parteAzul()).somar(r.parteAmarela());
                            } else {
                                azul = azul.somar(r.parteAzul());
                                amarelo = amarelo.somar(r.parteAmarela());
                            }
                        }
                    }
                }
            }

            resumo.add(new ResumoDeCategoria(categoria, natureza, azul, amarelo, vermelho, verde, naoTriada));
        }
        return resumo;
    }

    private static List<Dinheiro> valores(List<TransacaoClassificada> transacoes) {
        return transacoes.stream().map(TransacaoClassificada::valor).toList();
    }

    private static Natureza naturezaUnica(String categoria, List<TransacaoClassificada> transacoes) {
        Natureza primeira = transacoes.get(0).natureza();
        boolean divergente = transacoes.stream().anyMatch(t -> t.natureza() != primeira);
        if (divergente) {
            throw new IllegalStateException("categoria '" + categoria
                    + "' tem transações com naturezas diferentes — inconsistência de classificação");
        }
        return primeira;
    }
}
