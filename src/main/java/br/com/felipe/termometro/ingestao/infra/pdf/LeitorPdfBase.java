package br.com.felipe.termometro.ingestao.infra.pdf;

import br.com.felipe.termometro.ingestao.application.leitor.LeitorDeFatura;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.Parcela;
import br.com.felipe.termometro.ingestao.domain.Reconciliacao;
import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.ingestao.domain.ValorBrasileiro;
import br.com.felipe.termometro.shared.Dinheiro;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** Infraestrutura comum para PDFs textuais. PDFs escaneados recebem aviso em vez de dados inventados. */
abstract class LeitorPdfBase implements LeitorDeFatura {

    private static final Pattern VALOR = Pattern.compile("[-+]?[\\d.]+,\\d{2}");
    private static final Pattern DATA_VENCIMENTO = Pattern.compile("(?:Vencimento:\\s*|Com vencimento em:\\s*)(\\d{2}/\\d{2}/\\d{4})", Pattern.CASE_INSENSITIVE);

    @Override
    public final ResultadoDaLeitura ler(InputStream conteudo) throws IOException {
        try (PDDocument documento = Loader.loadPDF(conteudo.readAllBytes())) {
            String texto = new PDFTextStripper().getText(documento);
            if (texto.isBlank()) {
                return new ResultadoDaLeitura(List.of(), null,
                        List.of("o PDF não possui texto selecionável; use uma fatura digital, não escaneada"));
            }
            return lerTexto(texto);
        }
    }

    abstract ResultadoDaLeitura lerTexto(String texto);

    protected ResultadoDaLeitura resultado(List<TransacaoBruta> transacoes, Dinheiro totalDeclarado,
                                           List<String> avisos) {
        if (totalDeclarado == null) {
            avisos.add("não encontrei o total declarado da fatura; confira a importação antes de analisar");
            return new ResultadoDaLeitura(transacoes, null, avisos);
        }
        Dinheiro totalLido = Dinheiro.somaDe(transacoes.stream()
                .filter(TransacaoBruta::compoeTotalDaFatura).map(TransacaoBruta::valor).toList()).absoluto();
        Reconciliacao conciliacao = Reconciliacao.de(totalLido, totalDeclarado);
        if (!conciliacao.fecha()) {
            avisos.add(conciliacao.relatorio());
        }
        return new ResultadoDaLeitura(transacoes, conciliacao, avisos);
    }

    protected static Dinheiro valor(String texto) {
        Matcher matcher = VALOR.matcher(texto);
        Dinheiro ultimo = null;
        while (matcher.find()) {
            ultimo = ValorBrasileiro.converter(matcher.group());
        }
        if (ultimo == null) throw new IllegalArgumentException("valor ausente: " + texto);
        return ultimo;
    }

    protected static LocalDate dataDaFatura(String texto) {
        Matcher matcher = DATA_VENCIMENTO.matcher(texto);
        if (matcher.find()) {
            String[] partes = matcher.group(1).split("/");
            return LocalDate.of(Integer.parseInt(partes[2]), Integer.parseInt(partes[1]), Integer.parseInt(partes[0]));
        }
        return LocalDate.now();
    }

    /** Datas sem ano são resolvidas próximo da data de vencimento, inclusive compras parceladas do ano anterior. */
    protected static LocalDate dataDaCompra(String diaMes, LocalDate referencia) {
        String[] partes = diaMes.split("/");
        int mes = Integer.parseInt(partes[1]);
        int ano = referencia.getYear();
        if (mes > referencia.getMonthValue() + 1) ano--;
        return LocalDate.of(ano, mes, Integer.parseInt(partes[0]));
    }

    protected static TransacaoBruta transacao(String data, String descricao, Dinheiro valor,
                                              SecaoFatura secao, LocalDate referencia) {
        String exibivel = descricao.strip().replaceAll("\\s+", " ");
        Dinheiro normalizado = secao == SecaoFatura.PAGAMENTO ? valor.absoluto() : valor.absoluto().negado();
        return new TransacaoBruta(dataDaCompra(data, referencia), null, exibivel, exibivel, normalizado,
                null, null, secao, Parcela.extrairDe(exibivel).orElse(null), Origem.PDF, 0);
    }

    protected static List<String> linhas(String texto) {
        List<String> resultado = new ArrayList<>();
        for (String linha : texto.replace('\u00a0', ' ').split("\\R")) {
            if (!linha.isBlank()) resultado.add(linha.strip());
        }
        return resultado;
    }
}
