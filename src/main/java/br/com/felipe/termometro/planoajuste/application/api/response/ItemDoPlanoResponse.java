package br.com.felipe.termometro.planoajuste.application.api.response;

import br.com.felipe.termometro.planoajuste.domain.ItemDoPlano;
import java.math.BigDecimal;
import java.util.List;

public record ItemDoPlanoResponse(
        String categoria, String tipo, BigDecimal valorAtual, BigDecimal alvoFinal,
        List<AlvoMensalResponse> alvosMensais, boolean rampaAlongada, int dor,
        BigDecimal economiaMensalFinal) {

    public static ItemDoPlanoResponse de(ItemDoPlano item) {
        return new ItemDoPlanoResponse(item.categoria(), item.tipo().name(), item.valorAtual().valor(),
                item.alvoFinal().valor(), item.alvosMensais().stream().map(AlvoMensalResponse::de).toList(),
                item.rampaAlongada(), item.dor(), item.economiaMensalFinal().valor());
    }
}
