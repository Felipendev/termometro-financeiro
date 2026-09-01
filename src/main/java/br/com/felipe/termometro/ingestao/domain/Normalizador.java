package br.com.felipe.termometro.ingestao.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza a descrição de um lançamento para servir de chave de agrupamento (RN-12) e de
 * insumo do hash de deduplicação (RN-02).
 *
 * <p>O caso que motiva a classe: a fatura do Itaú <b>cola a cidade no fim do nome do
 * estabelecimento</b> — {@code "SUPERMERCADO ARRUDAJOAO"}, {@code "LaisDeAraujoJOAO PESSOA"},
 * {@code "UBER * PENDINGSAO PAULO"}. Sem desgrudar, o mesmo mercado vira dois estabelecimentos
 * diferentes e o detector de recorrência (RN-07) nunca acumula ocorrências suficientes. Nos dados
 * reais isso separou {@code SUPERMERCADO ARRUDA} (10 ocorrências) de
 * {@code SUPERMERCADO ARRUDAJOAO} (3) — nenhum dos dois cruzando o limiar sozinho.
 */
public final class Normalizador {

    /** Cidades vistas nas faturas, da mais longa para a mais curta. */
    private static final List<String> CIDADES_CONHECIDAS = List.of(
            "RIO DE JANEIRO", "RIO DE JANEIR", "JOAO PESSOA", "SAO PAULO", "CABEDELO",
            "JOAO PESSO", "SAO PAUL", "JOAO PES", "OSASCO", "JOAO P", "JOAO", "SAO", "PB", "BR");

    /** Menos que isto sobrando não é nome de estabelecimento, é mutilação. */
    private static final int MINIMO_RESTANTE = 4;
    /**
     * A cidade que a própria fatura informou pode ser cortada até 2 caracteres — o Itaú trunca o
     * campo concatenado numa largura fixa, e {@code "GRUPO FRUTOS DE GOIAS" + "JOAO PESSOA"} chega
     * como {@code "GRUPO FRUTOS DE GOIASJO"}. Para a lista genérica o piso é 3, senão um
     * estabelecimento terminado em "BR" ou "PB" perderia letras de verdade.
     */
    private static final int MINIMO_COM_CIDADE_CONHECIDA = 2;
    private static final int MINIMO_SEM_CIDADE = 3;

    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern SUFIXO_PARCELA =
            Pattern.compile("\\s*-?\\s*(?:PARCELA|PARC)\\s*\\d{1,2}\\s*/\\s*\\d{1,2}\\s*$");
    private static final Pattern SUFIXO_FRACAO = Pattern.compile("\\s*\\d{1,2}\\s*/\\s*\\d{1,2}\\s*$");
    private static final Pattern CODIGO_LONGO = Pattern.compile("\\d{6,}");
    private static final Pattern PREFIXO_PIX = Pattern.compile("^PIX(?:\\s+NO\\s+CREDITO)?\\s*-?\\s*");
    private static final Pattern NAO_IMPRIMIVEL = Pattern.compile("[^A-Z0-9*. ]");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private Normalizador() {
    }

    /** Normalização completa: usada como chave de agrupamento por estabelecimento. */
    public static String chaveDeEstabelecimento(String descricao, String cidade) {
        String base = removerCidadeColada(descricao, cidade);
        String semAcento = ACENTOS.matcher(Normalizer.normalize(base, Normalizer.Form.NFKD))
                .replaceAll("")
                .toUpperCase(Locale.ROOT);
        String semParcela = SUFIXO_FRACAO.matcher(SUFIXO_PARCELA.matcher(semAcento).replaceAll(""))
                .replaceAll("");
        String semCodigo = CODIGO_LONGO.matcher(semParcela).replaceAll("");
        String comPix = PREFIXO_PIX.matcher(semCodigo.strip()).replaceFirst("PIX ");
        return ESPACOS.matcher(NAO_IMPRIMIVEL.matcher(comPix).replaceAll(" ")).replaceAll(" ").strip();
    }

    public static String chaveDeEstabelecimento(String descricao) {
        return chaveDeEstabelecimento(descricao, null);
    }

    /**
     * Normalização mínima para o hash de deduplicação: preserva mais do original, porque aqui o
     * objetivo é identificar a <i>mesma</i> transação, não o mesmo estabelecimento.
     */
    public static String chaveDeDeduplicacao(String descricao) {
        String semAcento = ACENTOS.matcher(Normalizer.normalize(descricao, Normalizer.Form.NFKD))
                .replaceAll("")
                .toUpperCase(Locale.ROOT);
        String semParcela = SUFIXO_PARCELA.matcher(semAcento).replaceAll("");
        String semCodigo = CODIGO_LONGO.matcher(semParcela).replaceAll("");
        return ESPACOS.matcher(semCodigo).replaceAll(" ").strip();
    }

    /**
     * Remove a cidade colada no fim do nome. Tenta primeiro a cidade que a própria fatura
     * informou; depois a lista conhecida. Aceita sufixo truncado — o Itaú corta o campo em
     * largura fixa, então {@code "JOAO PESSOA"} vira {@code "JOAO PES"} ou até {@code "JOAO"}.
     */
    static String removerCidadeColada(String descricao, String cidade) {
        String texto = descricao.strip();
        String maiusculo = texto.toUpperCase(Locale.ROOT);

        if (cidade != null && !cidade.isBlank()) {
            String candidato = tentarRemover(texto, maiusculo,
                    cidade.strip().toUpperCase(Locale.ROOT), MINIMO_COM_CIDADE_CONHECIDA);
            if (candidato != null) {
                return candidato;
            }
        }
        for (String conhecida : CIDADES_CONHECIDAS) {
            String candidato = tentarRemover(texto, maiusculo, conhecida, MINIMO_SEM_CIDADE);
            if (candidato != null) {
                return candidato;
            }
        }
        return texto;
    }

    private static String tentarRemover(String texto, String maiusculo, String cidade,
                                        int minimoDoSufixo) {
        for (int tamanho = cidade.length(); tamanho >= minimoDoSufixo; tamanho--) {
            String sufixo = cidade.substring(0, tamanho);
            if (maiusculo.endsWith(sufixo) && texto.length() - tamanho >= MINIMO_RESTANTE) {
                return texto.substring(0, texto.length() - tamanho).strip();
            }
        }
        return null;
    }
}
