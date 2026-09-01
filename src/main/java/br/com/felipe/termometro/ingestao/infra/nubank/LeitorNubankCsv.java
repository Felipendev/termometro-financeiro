package br.com.felipe.termometro.ingestao.infra.nubank;

import br.com.felipe.termometro.ingestao.application.leitor.LeitorDeFatura;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.Parcela;
import br.com.felipe.termometro.ingestao.domain.Reconciliacao;
import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.ingestao.domain.ValorBrasileiro;
import br.com.felipe.termometro.shared.Dinheiro;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Leitor do CSV de fatura do Nubank: {@code date,title,amount}.
 *
 * <p>Três armadilhas do formato, todas encontradas nos arquivos reais:
 *
 * <ol>
 *   <li><b>Sinal invertido.</b> O Nubank exporta despesa como <i>positivo</i> e pagamento como
 *       negativo. A RN-01 manda o contrário, então o leitor inverte. Sem isso, o sistema leria
 *       um mês inteiro de gastos como se fosse receita.</li>
 *   <li><b>Espaço depois do sinal:</b> {@code "- 2.625,03"}.</li>
 *   <li><b>{@code "Pagamento recebido"} é o único lançamento que não é gasto</b>, e a
 *       identificação tem que ser por descrição exata. Casar por
 *       {@code contains("PAGAMENTO")} descartaria {@code "PIX Nu Pagamentos SA"} — que é uma
 *       transferência de R$ 2.436,11 feita no crédito, ou seja, gasto real.</li>
 * </ol>
 */
@Component
public class LeitorNubankCsv implements LeitorDeFatura {

    public static final String FORMATO = "NUBANK_CSV";
    private static final String CABECALHO_ESPERADO = "date,title,amount";
    private static final String PAGAMENTO_RECEBIDO = "PAGAMENTO RECEBIDO";
    private static final int COLUNAS = 3;

    private final @Nullable Dinheiro totalDeclarado;

    public LeitorNubankCsv() {
        this(null);
    }

    /** @param totalDeclarado total impresso na fatura, para reconciliar (opcional) */
    public LeitorNubankCsv(@Nullable Dinheiro totalDeclarado) {
        this.totalDeclarado = totalDeclarado;
    }

    @Override
    public String formato() {
        return FORMATO;
    }

    @Override
    public ResultadoDaLeitura ler(InputStream conteudo) throws IOException {
        List<TransacaoBruta> transacoes = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(conteudo, StandardCharsets.UTF_8))) {
            String cabecalho = leitor.readLine();
            if (cabecalho == null) {
                avisos.add("arquivo vazio");
                return new ResultadoDaLeitura(List.of(), null, avisos);
            }
            if (!removerBom(cabecalho).strip().equalsIgnoreCase(CABECALHO_ESPERADO)) {
                avisos.add("cabeçalho inesperado: '" + cabecalho + "' (esperado '"
                        + CABECALHO_ESPERADO + "')");
            }
            String linha;
            int numeroDaLinha = 1;
            while ((linha = leitor.readLine()) != null) {
                numeroDaLinha++;
                if (linha.isBlank()) {
                    continue;
                }
                interpretar(linha, numeroDaLinha, avisos).ifPresent(transacoes::add);
            }
        }

        Reconciliacao reconciliacao = null;
        if (totalDeclarado != null) {
            Dinheiro lido = Dinheiro.somaDe(transacoes.stream()
                    .filter(TransacaoBruta::compoeTotalDaFatura)
                    .map(TransacaoBruta::valor)
                    .toList()).absoluto();
            reconciliacao = Reconciliacao.de(lido, totalDeclarado);
            if (!reconciliacao.fecha()) {
                avisos.add(reconciliacao.relatorio());
            }
        }
        return new ResultadoDaLeitura(transacoes, reconciliacao, avisos);
    }

    private Optional<TransacaoBruta> interpretar(String linha, int numero, List<String> avisos) {
        List<String> campos = separarCsv(linha);
        if (campos.size() < COLUNAS) {
            avisos.add("linha " + numero + " com menos de " + COLUNAS + " colunas, ignorada");
            return Optional.empty();
        }
        LocalDate data;
        try {
            data = LocalDate.parse(campos.get(0).strip());
        } catch (DateTimeParseException e) {
            avisos.add("linha " + numero + " com data inválida '" + campos.get(0) + "', ignorada");
            return Optional.empty();
        }
        String titulo = campos.get(1).strip();
        Optional<Dinheiro> valorDoArquivo = ValorBrasileiro.tentarConverter(campos.get(2));
        if (valorDoArquivo.isEmpty()) {
            avisos.add("linha " + numero + " com valor inválido '" + campos.get(2) + "', ignorada");
            return Optional.empty();
        }

        boolean pagamentoDaFatura = titulo.toUpperCase(Locale.ROOT).equals(PAGAMENTO_RECEBIDO);
        // RN-01: no Nubank despesa vem positiva; aqui ela passa a ser negativa.
        Dinheiro valor = valorDoArquivo.get().negado();

        return Optional.of(new TransacaoBruta(
                data,
                null,                                   // o CSV do Nubank não traz hora (RN-12)
                titulo,
                titulo,
                valor,
                null,
                null,
                pagamentoDaFatura ? SecaoFatura.PAGAMENTO : SecaoFatura.CARTAO,
                Parcela.extrairDe(titulo).orElse(null),
                Origem.CSV,
                0));
    }

    /** CSV mínimo: campos entre aspas podem conter vírgula, que é como o valor é exportado. */
    private static List<String> separarCsv(String linha) {
        List<String> campos = new ArrayList<>(COLUNAS);
        StringBuilder atual = new StringBuilder();
        boolean dentroDeAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char caractere = linha.charAt(i);
            if (caractere == '"') {
                boolean aspaEscapada = dentroDeAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"';
                if (aspaEscapada) {
                    atual.append('"');
                    i++;
                } else {
                    dentroDeAspas = !dentroDeAspas;
                }
            } else if (caractere == ',' && !dentroDeAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(caractere);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    private static String removerBom(String texto) {
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }
}
