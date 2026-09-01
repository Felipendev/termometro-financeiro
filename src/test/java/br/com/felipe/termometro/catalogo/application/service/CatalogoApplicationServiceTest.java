package br.com.felipe.termometro.catalogo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.catalogo.application.api.request.CustoFixoItemRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRotativaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.PisoHumanoRequest;
import br.com.felipe.termometro.catalogo.application.api.request.RendaRequest;
import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoApplicationService")
class CatalogoApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock
    private CatalogoRepository catalogoRepository;

    private CatalogoApplicationService servico() {
        return new CatalogoApplicationService(catalogoRepository);
    }

    @Nested
    @DisplayName("renda")
    class Renda_ {

        @Test
        @DisplayName("declaraRenda constrói o domínio com a competência do path e delega o upsert")
        void declaraRendaDelega() {
            RendaRequest request = new RendaRequest(new BigDecimal("10000.00"), "PJ fixo");

            Renda renda = servico().declaraRenda(SETEMBRO, request);

            assertThat(renda.competencia()).isEqualTo(SETEMBRO);
            assertThat(renda.valorLiquido()).isEqualTo(Dinheiro.de("10000.00"));
            verify(catalogoRepository).salvaRenda(renda);
        }

        @Test
        @DisplayName("buscaRenda propaga o valor quando existe")
        void buscaRendaExistente() {
            Renda renda = new Renda(SETEMBRO, Dinheiro.de("10000.00"), null);
            when(catalogoRepository.buscaRenda(SETEMBRO)).thenReturn(Optional.of(renda));

            assertThat(servico().buscaRenda(SETEMBRO)).isEqualTo(renda);
        }

        @Test
        @DisplayName("buscaRenda lança 404 explícito quando não há renda declarada")
        void buscaRendaAusenteEhErro() {
            when(catalogoRepository.buscaRenda(SETEMBRO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servico().buscaRenda(SETEMBRO))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("Nenhuma renda declarada");
        }
    }

    @Nested
    @DisplayName("custo fixo")
    class CustoFixo {

        @Test
        @DisplayName("salvaCustoFixo constrói o item com o id do path e devolve o que o repositório persistiu")
        void salvaCustoFixoDelega() {
            UUID id = UUID.randomUUID();
            CustoFixoItemRequest request =
                    new CustoFixoItemRequest("Aluguel", new BigDecimal("2200.00"), "CONTA", null, true);
            CustoFixoItem persistido = new CustoFixoItem(id, "Aluguel", Dinheiro.de("2200.00"), "CONTA", null, true);
            when(catalogoRepository.salvaCustoFixo(any())).thenReturn(persistido);

            CustoFixoItem resultado = servico().salvaCustoFixo(id, request);

            ArgumentCaptor<CustoFixoItem> captor = ArgumentCaptor.forClass(CustoFixoItem.class);
            verify(catalogoRepository).salvaCustoFixo(captor.capture());
            assertThat(captor.getValue().id()).isEqualTo(id);
            assertThat(captor.getValue().ativo()).isTrue();
            assertThat(resultado).isEqualTo(persistido);
        }
    }

    @Nested
    @DisplayName("piso humano")
    class PisoHumano_ {

        @Test
        @DisplayName("salvaPisoHumano constrói o piso com a categoria do path")
        void salvaPisoHumanoDelega() {
            PisoHumanoRequest request = new PisoHumanoRequest(new BigDecimal("300.00"), "média de 3 meses", false);
            PisoHumano persistido = new PisoHumano("MERCADO", Dinheiro.de("300.00"), "média de 3 meses", false);
            when(catalogoRepository.salvaPisoHumano(any())).thenReturn(persistido);

            PisoHumano resultado = servico().salvaPisoHumano("MERCADO", request);

            ArgumentCaptor<PisoHumano> captor = ArgumentCaptor.forClass(PisoHumano.class);
            verify(catalogoRepository).salvaPisoHumano(captor.capture());
            assertThat(captor.getValue().categoria()).isEqualTo("MERCADO");
            assertThat(resultado).isEqualTo(persistido);
        }

        @Test
        @DisplayName("removePisoHumano delega direto, sem checagem de existência (idempotente)")
        void removePisoHumanoDelega() {
            servico().removePisoHumano("LAZER");

            verify(catalogoRepository).removePisoHumano("LAZER");
        }
    }

    @Nested
    @DisplayName("dívidas")
    class Dividas {

        @Test
        @DisplayName("salvaDivida parseia a competência do request e delega")
        void salvaDividaDelega() {
            UUID id = UUID.randomUUID();
            DividaRequest request = new DividaRequest("Empréstimo Nubank", new BigDecimal("2058.05"), "2026-09", null);
            Divida persistida = new Divida(id, "Empréstimo Nubank", Dinheiro.de("2058.05"), SETEMBRO, null);
            when(catalogoRepository.salvaDivida(any())).thenReturn(persistida);

            Divida resultado = servico().salvaDivida(id, request);

            ArgumentCaptor<Divida> captor = ArgumentCaptor.forClass(Divida.class);
            verify(catalogoRepository).salvaDivida(captor.capture());
            assertThat(captor.getValue().competenciaUltimaParcela()).isEqualTo(SETEMBRO);
            assertThat(resultado).isEqualTo(persistida);
        }

        @Test
        @DisplayName("salvaDivida com competência em formato inválido vira 400, não 500")
        void salvaDividaCompetenciaInvalidaEhErroDeCliente() {
            UUID id = UUID.randomUUID();
            DividaRequest request = new DividaRequest("Empréstimo", new BigDecimal("100.00"), "não-é-competência", null);

            assertThatThrownBy(() -> servico().salvaDivida(id, request))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("Competência inválida");
            verify(catalogoRepository, never()).salvaDivida(any());
        }

        @Test
        @DisplayName("removeDivida delega direto (idempotência é responsabilidade do repositório)")
        void removeDividaDelega() {
            UUID id = UUID.randomUUID();

            servico().removeDivida(id);

            verify(catalogoRepository).removeDivida(id);
        }
    }

    @Nested
    @DisplayName("dívidas rotativas")
    class DividasRotativas {

        @Test
        @DisplayName("salvaDividaRotativa constrói o domínio com o id do path e delega")
        void salvaDividaRotativaDelega() {
            UUID id = UUID.randomUUID();
            DividaRotativaRequest request = new DividaRotativaRequest(
                    "Rotativo Itaú", new BigDecimal("7952.24"), new BigDecimal("0.0636"), false, "medido na fatura");
            DividaRotativa persistida = new DividaRotativa(id, "Rotativo Itaú", Dinheiro.de("7952.24"),
                    Percentual.deFracao(new BigDecimal("0.0636")), false, "medido na fatura");
            when(catalogoRepository.salvaDividaRotativa(any())).thenReturn(persistida);

            DividaRotativa resultado = servico().salvaDividaRotativa(id, request);

            ArgumentCaptor<DividaRotativa> captor = ArgumentCaptor.forClass(DividaRotativa.class);
            verify(catalogoRepository).salvaDividaRotativa(captor.capture());
            assertThat(captor.getValue().id()).isEqualTo(id);
            assertThat(resultado).isEqualTo(persistida);
        }

        @Test
        @DisplayName("removeDividaRotativa delega direto")
        void removeDividaRotativaDelega() {
            UUID id = UUID.randomUUID();

            servico().removeDividaRotativa(id);

            verify(catalogoRepository).removeDividaRotativa(id);
        }
    }

    @Test
    @DisplayName("leituras simples delegam sem transformar nada")
    void leiturasDelegam() {
        List<CustoFixoItem> custoFixo = List.of();
        List<PisoHumano> pisos = List.of();
        List<Divida> dividas = List.of();
        List<DividaRotativa> rotativas = List.of();
        when(catalogoRepository.buscaCustoFixoAtivo()).thenReturn(custoFixo);
        when(catalogoRepository.buscaPisoHumano()).thenReturn(pisos);
        when(catalogoRepository.buscaDividasAtivas(SETEMBRO)).thenReturn(dividas);
        when(catalogoRepository.buscaDividasRotativasAtivas()).thenReturn(rotativas);

        CatalogoApplicationService servico = servico();
        assertThat(servico.listaCustoFixo()).isSameAs(custoFixo);
        assertThat(servico.listaPisoHumano()).isSameAs(pisos);
        assertThat(servico.listaDividasAtivas(SETEMBRO)).isSameAs(dividas);
        assertThat(servico.listaDividasRotativasAtivas()).isSameAs(rotativas);
    }
}
