package br.com.felipe.termometro.ingestao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.ingestao.application.repository.ContaRepository;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.ContaBancaria;
import br.com.felipe.termometro.ingestao.domain.TipoDeConta;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartaoApplicationService")
class CartaoApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private CartaoRepository cartaoRepository;

    @Test
    @DisplayName("compõe cada cartão com o gasto real da competência e o percentual do limite usado")
    void componhaCartaoComGastoEPercentual() {
        when(contaRepository.buscaCartoes()).thenReturn(List.of(cartao("nubank", "Nubank", "3000.00")));
        when(transacaoRepository.somaGastoDeCartaoPorConta(SETEMBRO))
                .thenReturn(Map.of("nubank", Dinheiro.de("450.00")));

        ResumoCartoesResponse resumo = servico().consultaCartoes(SETEMBRO);

        assertThat(resumo.cartoes()).hasSize(1);
        assertThat(resumo.cartoes().get(0).identificador()).isEqualTo("nubank");
        assertThat(resumo.cartoes().get(0).gastoNoMes()).isEqualTo(Dinheiro.de("450.00"));
        assertThat(resumo.cartoes().get(0).percentualUsado())
                .isEqualTo(Percentual.deValor(Dinheiro.de("450.00"), Dinheiro.de("3000.00")));
        assertThat(resumo.totalGastoEmCartoes()).isEqualTo(Dinheiro.de("450.00"));
    }

    @Test
    @DisplayName("usa zero como gasto quando o cartão não tem nenhuma transação na competência")
    void zeraGastoQuandoContaNaoApareceNoMapa() {
        when(contaRepository.buscaCartoes()).thenReturn(List.of(cartao("itau", "Itaú", "1500.00")));
        when(transacaoRepository.somaGastoDeCartaoPorConta(SETEMBRO)).thenReturn(Map.of());

        ResumoCartoesResponse resumo = servico().consultaCartoes(SETEMBRO);

        assertThat(resumo.cartoes().get(0).gastoNoMes()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.totalGastoEmCartoes()).isEqualTo(Dinheiro.ZERO);
    }

    @Test
    @DisplayName("não calcula percentual quando o banco não informa limite para o cartão")
    void percentualNuloQuandoLimiteAusente() {
        ContaBancaria semLimite = new ContaBancaria("ext-1", "picpay", "Picpay", TipoDeConta.CARTAO_CREDITO,
                null, Dinheiro.ZERO, null);
        when(contaRepository.buscaCartoes()).thenReturn(List.of(semLimite));
        when(transacaoRepository.somaGastoDeCartaoPorConta(SETEMBRO))
                .thenReturn(Map.of("picpay", Dinheiro.de("4257.74")));

        ResumoCartoesResponse resumo = servico().consultaCartoes(SETEMBRO);

        assertThat(resumo.cartoes().get(0).limite()).isNull();
        assertThat(resumo.cartoes().get(0).percentualUsado()).isNull();
    }

    @Test
    @DisplayName("não calcula percentual quando o limite informado é zero, para não dividir por zero")
    void percentualNuloQuandoLimiteZero() {
        ContaBancaria limiteZero = new ContaBancaria("ext-2", "cartao-sem-limite", "Cartão sem limite",
                TipoDeConta.CARTAO_CREDITO, null, Dinheiro.ZERO, Dinheiro.ZERO);
        when(contaRepository.buscaCartoes()).thenReturn(List.of(limiteZero));
        when(transacaoRepository.somaGastoDeCartaoPorConta(SETEMBRO))
                .thenReturn(Map.of("cartao-sem-limite", Dinheiro.de("100.00")));

        ResumoCartoesResponse resumo = servico().consultaCartoes(SETEMBRO);

        assertThat(resumo.cartoes().get(0).percentualUsado()).isNull();
    }

    @Test
    @DisplayName("soma o total gasto em todos os cartões, não só o de cada um isoladamente")
    void somaTotalDeVariosCartoes() {
        when(contaRepository.buscaCartoes()).thenReturn(List.of(
                cartao("nubank", "Nubank", "3000.00"),
                cartao("itau", "Itaú", "2000.00"),
                cartao("picpay", "Picpay", "5000.00")));
        when(transacaoRepository.somaGastoDeCartaoPorConta(SETEMBRO)).thenReturn(Map.of(
                "nubank", Dinheiro.de("2582.89"),
                "itau", Dinheiro.de("1390.61"),
                "picpay", Dinheiro.de("4257.74")));

        ResumoCartoesResponse resumo = servico().consultaCartoes(SETEMBRO);

        assertThat(resumo.totalGastoEmCartoes()).isEqualTo(Dinheiro.de("8231.24"));
    }

    @Test
    @DisplayName("importação vinculada ao UUID manual alimenta cartões e substitui o duplicado bancário")
    void incluiImportacaoFeitaPeloCartaoManual() {
        UUID id = UUID.randomUUID();
        when(contaRepository.buscaCartoes()).thenReturn(List.of(cartao("nubank", "Nubank", "3000.00")));
        when(cartaoRepository.buscaAtivos()).thenReturn(List.of(
                new Cartao(id, "Nubank", Dinheiro.de("5000"), Dinheiro.ZERO, null, true)));
        when(transacaoRepository.somaGastoDeCartaoPorConta(SETEMBRO))
                .thenReturn(Map.of(id.toString(), Dinheiro.de("450.00")));

        ResumoCartoesResponse resumo = new CartaoApplicationService(
                contaRepository, transacaoRepository, cartaoRepository).consultaCartoes(SETEMBRO);

        assertThat(resumo.cartoes()).singleElement().satisfies(cartao -> {
            assertThat(cartao.identificador()).isEqualTo(id.toString());
            assertThat(cartao.gastoNoMes()).isEqualTo(Dinheiro.de("450"));
        });
    }

    private CartaoApplicationService servico() {
        return new CartaoApplicationService(contaRepository, transacaoRepository);
    }

    private ContaBancaria cartao(String identificador, String nome, String limite) {
        return new ContaBancaria("ext-" + identificador, identificador, nome, TipoDeConta.CARTAO_CREDITO,
                null, Dinheiro.ZERO, Dinheiro.de(limite));
    }
}
