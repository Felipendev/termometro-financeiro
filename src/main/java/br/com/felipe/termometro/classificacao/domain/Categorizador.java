package br.com.felipe.termometro.classificacao.domain;

import br.com.felipe.termometro.ingestao.domain.Normalizador;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Aplica as regras e decide a categoria — e, o que mais importa para o orçamento, <b>se o gasto
 * entra na verba diária</b>.
 *
 * <p>A primeira regra que casar decide. Não há voto, soma de pesos nem desempate estatístico: uma
 * classificação errada precisa ser rastreável até a regra que a causou, para que a correção do
 * usuário possa virar uma regra nova (RN-12) em vez de um ajuste opaco.
 */
public final class Categorizador {

    private final List<RegraDeCategorizacao> regras;

    public Categorizador(Collection<RegraDeCategorizacao> regras) {
        Objects.requireNonNull(regras, "regras não podem ser nulas");
        List<RegraDeCategorizacao> ordenadas = new ArrayList<>(regras);
        ordenadas.sort(RegraDeCategorizacao.ordemDeAvaliacao());
        this.regras = List.copyOf(ordenadas);
    }

    public static Categorizador padrao() {
        return new Categorizador(CatalogoDeRegrasPadrao.regras());
    }

    /** Um categorizador com as regras do catálogo mais as que o usuário criou ou o sistema aprendeu. */
    public Categorizador com(Collection<RegraDeCategorizacao> adicionais) {
        List<RegraDeCategorizacao> todas = new ArrayList<>(regras);
        todas.addAll(adicionais);
        return new Categorizador(todas);
    }

    public Classificacao classificar(TransacaoBruta transacao) {
        Objects.requireNonNull(transacao, "transação não pode ser nula");
        String texto = Normalizador.chaveDeEstabelecimento(transacao.descricao(), transacao.cidade());
        String categoriaDoBanco = transacao.categoriaBanco();

        for (RegraDeCategorizacao regra : regras) {
            if (regra.casaCom(texto, null, categoriaDoBanco)) {
                return montar(transacao, regra.categoria(), regra.confianca(), regra.origem(),
                        "regra %s '%s'".formatted(regra.tipo(), regra.padrao()));
            }
        }
        return montar(transacao, Categoria.NAO_IDENTIFICADA, BigDecimal.ZERO, null,
                "nenhuma regra casou com '%s'".formatted(texto));
    }

    private static Classificacao montar(TransacaoBruta transacao, Categoria categoria,
                                        BigDecimal confianca, OrigemDaRegra origem, String motivo) {
        boolean conta = entraNaVerbaDiaria(transacao, categoria);
        boolean revisao = confianca.compareTo(Classificacao.LIMIAR_DE_CONFIANCA) < 0;
        return new Classificacao(categoria, confianca, origem, conta, revisao, motivo);
    }

    /**
     * A regra que faz a verba diária ser útil (RN-19).
     *
     * <p>Ficam de fora três coisas, cada uma por um motivo diferente:
     *
     * <ol>
     *   <li><b>Fixo e não-gasto</b> — aluguel, imposto, contador, assinatura, pagamento de fatura.
     *       Não são decisão de hoje; já estão no orçamento como custo fixo.</li>
     *   <li><b>Parcela</b> — foi decisão de um mês passado (o "Eu do Passado" da RN-11). Contar a
     *       parcela do celular na verba de hoje pune o usuário duas vezes pela mesma compra.</li>
     *   <li><b>Lançamento fora da fatura corrente</b> — parcelas futuras e pagamentos não compõem
     *       o gasto do mês (RN-03).</li>
     * </ol>
     *
     * <p>Sem isso, no dia 5 — quando aluguel, imposto e contador caem juntos — a verba de setembro
     * apareceria estourada, e o usuário largaria o sistema na primeira semana, com razão.
     */
    static boolean entraNaVerbaDiaria(TransacaoBruta transacao, Categoria categoria) {
        if (!transacao.ehDespesa() || !transacao.compoeTotalDaFatura()) {
            return false;
        }
        if (transacao.parcelaOpcional().isPresent()) {
            return false;
        }
        return categoria.natureza().entraNaVerbaDiaria();
    }
}
