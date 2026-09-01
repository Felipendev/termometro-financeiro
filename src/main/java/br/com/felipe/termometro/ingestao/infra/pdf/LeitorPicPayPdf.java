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

/** Leitor da fatura PicPay Mastercard textual. Mantém cada adicional no mesmo identificador escolhido no upload. */
@Component
public final class LeitorPicPayPdf extends LeitorPdfBase {
    public static final String FORMATO = "PICPAY_PDF";
    private static final Pattern TOTAL = Pattern.compile("Total da sua fatura\\s*R\\$\\s*([\\d.]+,\\d{2})");
    private static final Pattern LANCAMENTO = Pattern.compile("^(\\d{2}/\\d{2})\\s+(.+?)\\s+(-?[\\d.]+,\\d{2})$");
    private static final Pattern INTERNACIONAL_INICIO = Pattern.compile("^(\\d{2}/\\d{2})\\s+(.+)$");
    private static final Pattern DATA_ISOLADA = Pattern.compile("^\\d{2}/\\d{2}$");
    private static final Pattern INTERNACIONAL_VALOR = Pattern.compile("^[\\d.]+,\\d{2}\\s+([\\d.]+,\\d{2})$");

    @Override public String formato() { return FORMATO; }

    @Override ResultadoDaLeitura lerTexto(String texto) {
        List<TransacaoBruta> transacoes = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Dinheiro total = total(texto);
        var referencia = dataDaFatura(texto);
        SecaoFatura secao = SecaoFatura.IGNORAR;
        String dataInternacional = null;
        String descricaoInternacional = null;
        for (String linha : linhas(texto)) {
            if (linha.startsWith("Transações Nacionais")) { secao = SecaoFatura.CARTAO; continue; }
            if (linha.startsWith("Transações Internacionais")) { secao = SecaoFatura.INTERNACIONAL; continue; }
            if (linha.startsWith("Subtotal dos lançamentos") || linha.startsWith("Total geral")) { secao = SecaoFatura.IGNORAR; continue; }
            if (secao == SecaoFatura.IGNORAR || linha.startsWith("Data Estabelecimento")) continue;
            if (secao == SecaoFatura.INTERNACIONAL) {
                Matcher inicio = INTERNACIONAL_INICIO.matcher(linha);
                if (inicio.matches()) { dataInternacional = inicio.group(1); descricaoInternacional = inicio.group(2); continue; }
                if (DATA_ISOLADA.matcher(linha).matches()) { dataInternacional = linha; descricaoInternacional = null; continue; }
                Matcher valorInternacional = INTERNACIONAL_VALOR.matcher(linha);
                if (valorInternacional.matches() && dataInternacional != null) {
                    transacoes.add(transacao(dataInternacional, descricaoInternacional,
                            valor(valorInternacional.group(1)), secao, referencia));
                    dataInternacional = null;
                    descricaoInternacional = null;
                } else if (dataInternacional != null && descricaoInternacional == null
                        && !linha.startsWith("Data ") && !linha.startsWith("Dólar:")) {
                    descricaoInternacional = linha;
                }
                continue;
            }
            Matcher entrada = LANCAMENTO.matcher(linha);
            if (entrada.matches()) {
                String descricao = entrada.group(2);
                SecaoFatura secaoDaEntrada = descricao.toUpperCase().contains("PAGAMENTO DE FATURA")
                        ? SecaoFatura.PAGAMENTO : SecaoFatura.CARTAO;
                transacoes.add(transacao(entrada.group(1), descricao, valor(entrada.group(3)), secaoDaEntrada, referencia));
            }
        }
        if (transacoes.isEmpty()) avisos.add("não encontrei lançamentos nacionais no layout PicPay");
        return resultado(transacoes, total, avisos);
    }

    private static Dinheiro total(String texto) {
        Matcher matcher = TOTAL.matcher(texto);
        return matcher.find() ? valor(matcher.group(1)) : null;
    }
}
