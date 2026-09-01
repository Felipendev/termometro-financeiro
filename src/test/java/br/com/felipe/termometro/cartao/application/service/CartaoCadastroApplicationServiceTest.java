package br.com.felipe.termometro.cartao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.cartao.application.api.request.CartaoRequest;
import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartaoCadastroApplicationService")
class CartaoCadastroApplicationServiceTest {

    @Mock
    private CartaoRepository cartaoRepository;

    private CartaoCadastroApplicationService servico() {
        return new CartaoCadastroApplicationService(cartaoRepository);
    }

    @Test
    @DisplayName("salva constrói o domínio com o id do path, sempre ativo e sempre origem MANUAL")
    void salvaConstroiComIdDoPathEForcaOrigemManual() {
        UUID id = UUID.randomUUID();
        CartaoRequest request = new CartaoRequest("Nubank", new BigDecimal("5000.00"),
                new BigDecimal("1234.56"), "cartão principal");
        Cartao persistido = new Cartao(id, "Nubank", Dinheiro.de("5000.00"), Dinheiro.de("1234.56"),
                "cartão principal", true);
        when(cartaoRepository.salva(any())).thenReturn(persistido);

        Cartao resultado = servico().salva(id, request);

        ArgumentCaptor<Cartao> captor = ArgumentCaptor.forClass(Cartao.class);
        verify(cartaoRepository).salva(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(id);
        assertThat(captor.getValue().ativo()).isTrue();
        assertThat(resultado).isEqualTo(persistido);
    }

    @Test
    @DisplayName("salva aceita limite nulo (cartão sem limite declarado)")
    void salvaAceitaLimiteNulo() {
        UUID id = UUID.randomUUID();
        CartaoRequest request = new CartaoRequest("PicPay", null, new BigDecimal("300.00"), null);
        when(cartaoRepository.salva(any()))
                .thenReturn(new Cartao(id, "PicPay", null, Dinheiro.de("300.00"), null, true));

        servico().salva(id, request);

        ArgumentCaptor<Cartao> captor = ArgumentCaptor.forClass(Cartao.class);
        verify(cartaoRepository).salva(captor.capture());
        assertThat(captor.getValue().limiteOpcional()).isEmpty();
    }

    @Test
    @DisplayName("remove delega direto (idempotência é responsabilidade do repositório)")
    void removeDelega() {
        UUID id = UUID.randomUUID();

        servico().remove(id);

        verify(cartaoRepository).remove(id);
    }

    @Test
    @DisplayName("listaAtivos delega sem transformar nada")
    void listaAtivosDelega() {
        List<Cartao> ativos = List.of();
        when(cartaoRepository.buscaAtivos()).thenReturn(ativos);

        assertThat(servico().listaAtivos()).isSameAs(ativos);
    }
}
