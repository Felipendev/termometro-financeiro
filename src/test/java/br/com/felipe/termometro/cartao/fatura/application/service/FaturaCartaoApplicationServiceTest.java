package br.com.felipe.termometro.cartao.fatura.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.cartao.fatura.application.api.request.PagamentoFaturaRequest;
import br.com.felipe.termometro.cartao.fatura.application.repository.FaturaDeclaradaRepository;
import br.com.felipe.termometro.cartao.fatura.application.repository.PagamentoFaturaRepository;
import br.com.felipe.termometro.cartao.fatura.domain.PagamentoFatura;
import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.ingestao.application.service.CartaoService;
import br.com.felipe.termometro.lancamentoplanejado.application.service.LancamentoPlanejadoApplicationService;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FaturaCartaoApplicationServiceTest {
    private static final Competencia SETEMBRO = Competencia.de(2026, 9);
    private static final UUID CARTAO_ID = UUID.fromString("7b14bb80-86ab-46df-a88a-fda4707f69f0");
    private static final String REFERENCIA = "MANUAL:" + CARTAO_ID;

    @Mock CartaoService cartaoImportadoService;
    @Mock CartaoRepository cartoesManuais;
    @Mock PagamentoFaturaRepository pagamentos;
    @Mock FaturaDeclaradaRepository faturasDeclaradas;
    @Mock LancamentoPlanejadoApplicationService lancamentos;
    @InjectMocks FaturaCartaoApplicationService service;

    private final List<PagamentoFatura> pagamentosSalvos = new ArrayList<>();

    @BeforeEach
    void preparaFaturaManual() {
        when(cartaoImportadoService.consultaCartoes(SETEMBRO)).thenReturn(ResumoCartoesResponse.de(List.of()));
        when(cartoesManuais.buscaAtivos()).thenReturn(List.of(
                new Cartao(CARTAO_ID, "Nubank", Dinheiro.de("5000"), Dinheiro.ZERO, null, true)));
        when(faturasDeclaradas.buscaPorCompetencia(SETEMBRO)).thenReturn(Map.of(REFERENCIA, Dinheiro.de("1200")));
        when(pagamentos.buscaPorCompetencia(SETEMBRO)).thenAnswer(invocacao -> List.copyOf(pagamentosSalvos));
        when(pagamentos.salva(any())).thenAnswer(invocacao -> {
            PagamentoFatura pagamento = invocacao.getArgument(0);
            pagamentosSalvos.add(pagamento);
            return pagamento;
        });
    }

    @Test
    void pagamentoParcialCriaSaidaNaoGastoEApareceNoSaldoDaFatura() {
        AtomicReference<LancamentoPlanejado> criado = new AtomicReference<>();
        when(lancamentos.salva(any())).thenAnswer(invocacao -> {
            LancamentoPlanejado item = invocacao.getArgument(0);
            criado.set(item);
            return item;
        });
        when(lancamentos.liquidar(any())).thenAnswer(invocacao -> criado.get().liquidar());

        var resposta = service.paga(SETEMBRO, new PagamentoFaturaRequest(
                REFERENCIA, new BigDecimal("450.00"), LocalDate.of(2026, 9, 10), null));

        assertThat(resposta.status()).isEqualTo("PARCIAL");
        assertThat(resposta.valorPago()).isEqualTo(Dinheiro.de("450"));
        assertThat(resposta.saldoAberto()).isEqualTo(Dinheiro.de("750"));
        assertThat(criado.get().categoria().natureza()).isEqualTo("NAO_E_GASTO");
        assertThat(criado.get().descricao()).isEqualTo("Pagamento de fatura - Nubank");
        verify(lancamentos).liquidar(criado.get().id());
    }
}
