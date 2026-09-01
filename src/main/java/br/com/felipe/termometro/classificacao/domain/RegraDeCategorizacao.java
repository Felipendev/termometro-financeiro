package br.com.felipe.termometro.classificacao.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Uma regra de classificação: se o texto casar com o padrão, a transação é desta categoria.
 *
 * @param prioridade menor número decide primeiro; empate é resolvido pela origem
 */
public record RegraDeCategorizacao(
        int prioridade,
        TipoDeRegra tipo,
        String padrao,
        Categoria categoria,
        OrigemDaRegra origem) {

    private static final int ESCALA_DA_CONFIANCA = 2;

    public RegraDeCategorizacao {
        Objects.requireNonNull(tipo, "tipo não pode ser nulo");
        Objects.requireNonNull(padrao, "padrão não pode ser nulo");
        Objects.requireNonNull(categoria, "categoria não pode ser nula");
        Objects.requireNonNull(origem, "origem não pode ser nula");
        if (padrao.isBlank()) {
            throw new IllegalArgumentException("padrão não pode ser vazio");
        }
        if (tipo == TipoDeRegra.DESCRICAO) {
            compilar(padrao);
        }
    }

    public static RegraDeCategorizacao doSistema(int prioridade, TipoDeRegra tipo, String padrao,
                                                 Categoria categoria) {
        return new RegraDeCategorizacao(prioridade, tipo, padrao, categoria, OrigemDaRegra.SISTEMA);
    }

    /**
     * Nasce quando o usuário classifica uma transação e manda aplicar ao grupo (RN-12). Prioridade
     * negativa: aprendizado e correção manual passam na frente de qualquer regra do catálogo.
     */
    public static RegraDeCategorizacao aprendida(String estabelecimento, Categoria categoria) {
        return new RegraDeCategorizacao(-100, TipoDeRegra.ESTABELECIMENTO, estabelecimento,
                categoria, OrigemDaRegra.APRENDIZADO);
    }

    public static RegraDeCategorizacao doUsuario(TipoDeRegra tipo, String padrao, Categoria categoria) {
        return new RegraDeCategorizacao(-200, tipo, padrao, categoria, OrigemDaRegra.USUARIO);
    }

    /**
     * @param texto texto já normalizado ({@code Normalizador.chaveDeEstabelecimento})
     */
    public boolean casaCom(String texto, String cnpj, String categoriaDoBanco) {
        return switch (tipo) {
            case CNPJ -> cnpj != null && somenteDigitos(cnpj).equals(somenteDigitos(padrao));
            case ESTABELECIMENTO -> texto != null && texto.equalsIgnoreCase(padrao);
            case DESCRICAO -> texto != null && compilar(padrao).matcher(texto).find();
            case CATEGORIA_DO_BANCO -> categoriaDoBanco != null
                    && categoriaDoBanco.strip().equalsIgnoreCase(padrao);
        };
    }

    public BigDecimal confianca() {
        return origem.confiancaBase()
                .multiply(tipo.multiplicador())
                .setScale(ESCALA_DA_CONFIANCA, RoundingMode.HALF_EVEN);
    }

    private static Pattern compilar(String padrao) {
        try {
            return Pattern.compile(padrao, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("padrão de regex inválido: '" + padrao + "'", e);
        }
    }

    private static String somenteDigitos(String texto) {
        return texto.replaceAll("\\D", "");
    }

    /** Regras mais específicas primeiro; empate de prioridade decide pela origem mais forte. */
    public static java.util.Comparator<RegraDeCategorizacao> ordemDeAvaliacao() {
        return java.util.Comparator.comparingInt(RegraDeCategorizacao::prioridade)
                .thenComparing(r -> -r.origem().ordinal())
                .thenComparing(r -> r.padrao().toLowerCase(Locale.ROOT));
    }
}
