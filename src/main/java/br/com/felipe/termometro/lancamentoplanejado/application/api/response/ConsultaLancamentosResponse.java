package br.com.felipe.termometro.lancamentoplanejado.application.api.response;

import br.com.felipe.termometro.lancamentoplanejado.application.service.ConsultaLancamentosService;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;

public record ConsultaLancamentosResponse(
        List<LancamentoPlanejadoResponse> itens,
        int totalDeItens,
        Dinheiro totalDespesas,
        Dinheiro totalReceitas,
        Dinheiro saldoRealizado,
        Dinheiro saldoPrevisto,
        int quantidadeAtrasados,
        int pagina,
        int tamanho,
        boolean temMais) {

    public ConsultaLancamentosResponse(ConsultaLancamentosService.Resultado resultado) {
        this(resultado.itens().stream().map(item -> {
                    ConsultaLancamentosService.MetadadosConsulta metadados = resultado.metadados().get(item.id());
                    return new LancamentoPlanejadoResponse(item, metadados.contaOuCartao(),
                            metadados.editavel(), metadados.origem());
                }).toList(),
                resultado.totalDeItens(), resultado.totalDespesas(), resultado.totalReceitas(),
                resultado.saldoRealizado(), resultado.saldoPrevisto(), resultado.quantidadeAtrasados(),
                resultado.pagina(), resultado.tamanho(), resultado.temMais());
    }
}
