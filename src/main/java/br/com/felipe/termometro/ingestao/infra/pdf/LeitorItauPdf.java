package br.com.felipe.termometro.ingestao.infra.pdf;

import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Leitor da fatura Itaú textual, incluindo compras, internacionais e Pix feitos no crédito. */
@Component
public final class LeitorItauPdf extends LeitorPdfBase {
    public static final String FORMATO = "ITAU_PDF";
    private static final Pattern TOTAL = Pattern.compile("Total desta fatura\\s+([\\d.]+,\\d{2})");
    private static final Pattern LANCAMENTO = Pattern.compile("^(\\d{2}/\\d{2})\\s+(.+?)\\s+([\\d.]+,\\d{2})$");
    private static final Pattern IOF = Pattern.compile("^Repasse de IOF em R\\$\\s*([\\d.]+,\\d{2})$");

    @Override public String formato() { return FORMATO; }

    @Override ResultadoDaLeitura lerTexto(String texto) {
        List<TransacaoBruta> transacoes = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Dinheiro total = total(texto);
        var referencia = dataDaFatura(texto);
        SecaoFatura secao = SecaoFatura.IGNORAR;

        for (String linha : linhas(texto)) {
            if (linha.startsWith("Lançamentos: compras e saques")) { secao = SecaoFatura.CARTAO; continue; }
            if (linha.startsWith("Lançamentos internacionais")) { secao = SecaoFatura.INTERNACIONAL; continue; }
            if (linha.startsWith("Lançamentos: produtos e serviços")) { secao = SecaoFatura.PRODUTOS_SERVICOS; continue; }
            if (linha.startsWith("Compras parceladas - próximas faturas")) { secao = SecaoFatura.FUTURO; continue; }
            if (linha.startsWith("Limites de crédito") || linha.startsWith("Encargos cobrados")
                    || linha.startsWith("Total dos lançamentos atuais")) { secao = SecaoFatura.IGNORAR; continue; }

            if (secao == SecaoFatura.INTERNACIONAL) {
                Matcher iof = IOF.matcher(linha);
                if (iof.matches()) {
                    transacoes.add(transacao("01/%02d".formatted(referencia.getMonthValue()),
                            "IOF de compra internacional", valor(iof.group(1)), secao, referencia));
                    continue;
                }
            }
            if (secao == SecaoFatura.IGNORAR) continue;
            Matcher entrada = LANCAMENTO.matcher(linha);
            if (entrada.matches()) {
                transacoes.add(transacao(entrada.group(1), entrada.group(2), valor(entrada.group(3)), secao, referencia));
            }
        }
        if (transacoes.isEmpty()) avisos.add("não encontrei lançamentos no layout Itaú");
        return resultado(transacoes, total, avisos);
    }

    private static Dinheiro total(String texto) {
        Matcher matcher = TOTAL.matcher(texto);
        return matcher.find() ? valor(matcher.group(1)) : null;
    }
}
