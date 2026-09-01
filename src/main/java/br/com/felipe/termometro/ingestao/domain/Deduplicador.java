package br.com.felipe.termometro.ingestao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Atribui o ordinal da RN-02 e remove duplicatas entre importações.
 *
 * <p>Dois lançamentos idênticos <i>dentro do mesmo lote</i> são duas compras reais e recebem
 * ordinais diferentes. O mesmo lançamento chegando <i>em lotes diferentes</i> (reimportação do
 * arquivo, ou o mesmo mês vindo por Open Finance depois de ter vindo por CSV) é duplicata e é
 * descartado — mantendo a versão de origem mais confiável.
 */
public final class Deduplicador {

    private final Map<String, TransacaoBruta> porChave = new LinkedHashMap<>();
    private final List<String> avisos = new ArrayList<>();
    private final String identificadorDaConta;
    private int duplicadas;
    private int substituidas;

    public Deduplicador(String identificadorDaConta) {
        this.identificadorDaConta = Objects.requireNonNull(identificadorDaConta,
                "conta não pode ser nula");
    }

    /**
     * Numera lançamentos idênticos de um mesmo lote. A ordem de entrada é preservada — é ela que
     * define qual compra é a "primeira" do dia, e ela precisa ser estável entre reimportações do
     * mesmo arquivo, senão o hash muda e a dedupe deixa de funcionar.
     */
    public static List<TransacaoBruta> numerar(List<TransacaoBruta> lote) {
        Objects.requireNonNull(lote, "lote não pode ser nulo");
        Map<String, Integer> vistos = new HashMap<>();
        List<TransacaoBruta> numeradas = new ArrayList<>(lote.size());
        for (TransacaoBruta transacao : lote) {
            String assinatura = assinaturaDe(transacao.data(), transacao.valor(),
                    transacao.descricaoOriginal());
            int ordinal = vistos.merge(assinatura, 0, (anterior, ignorado) -> anterior + 1);
            numeradas.add(transacao.comOrdinal(ordinal));
        }
        return List.copyOf(numeradas);
    }

    /** Absorve um lote já numerado, descartando o que já existe. */
    public Deduplicador absorver(List<TransacaoBruta> lote) {
        for (TransacaoBruta candidata : numerar(lote)) {
            String chave = ChaveDeDeduplicacao.calcular(identificadorDaConta, candidata);
            TransacaoBruta existente = porChave.get(chave);
            if (existente == null) {
                porChave.put(chave, candidata);
            } else if (candidata.origem().maisConfiavelQue(existente.origem())) {
                porChave.put(chave, candidata);
                substituidas++;
                avisos.add("substituída por origem mais confiável (%s > %s): %s"
                        .formatted(candidata.origem(), existente.origem(), candidata.descricao()));
            } else {
                duplicadas++;
            }
        }
        return this;
    }

    public List<TransacaoBruta> transacoes() {
        return List.copyOf(porChave.values());
    }

    public int duplicadasDescartadas() {
        return duplicadas;
    }

    public int substituidasPorOrigemMelhor() {
        return substituidas;
    }

    public List<String> avisos() {
        return List.copyOf(avisos);
    }

    private static String assinaturaDe(LocalDate data, Dinheiro valor, String descricao) {
        return data + "|" + valor.centavos() + "|" + Normalizador.chaveDeDeduplicacao(descricao);
    }
}
