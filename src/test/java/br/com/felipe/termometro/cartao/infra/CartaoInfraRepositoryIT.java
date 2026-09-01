package br.com.felipe.termometro.cartao.infra;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.support.BancoDeTesteIT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("CartaoInfraRepository — Postgres de verdade")
class CartaoInfraRepositoryIT extends BancoDeTesteIT {

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private CartaoSpringDataJpaRepository cartaoSpringDataJpaRepository;

    @Test
    @DisplayName("salva cria e aparece nos ativos; upsert com o mesmo id atualiza em vez de duplicar")
    void salvaEhUpsert() {
        UUID id = UUID.randomUUID();

        cartaoRepository.salva(new Cartao(id, "Nubank", Dinheiro.de("5000.00"), Dinheiro.de("1200.00"),
                "principal", true));
        assertThat(cartaoRepository.buscaAtivos())
                .filteredOn(c -> c.id().equals(id))
                .hasSize(1)
                .first()
                .satisfies(c -> assertThat(c.valorFatura()).isEqualTo(Dinheiro.de("1200.00")));

        cartaoRepository.salva(new Cartao(id, "Nubank", Dinheiro.de("5000.00"), Dinheiro.de("1850.30"),
                "fatura de setembro fechou maior", true));
        assertThat(cartaoRepository.buscaAtivos())
                .filteredOn(c -> c.id().equals(id))
                .hasSize(1)
                .first()
                .satisfies(c -> assertThat(c.valorFatura()).isEqualTo(Dinheiro.de("1850.30")));
    }

    @Test
    @DisplayName("remove é soft delete: some da lista de ativos, mas a linha continua no banco")
    void removeEhSoftDelete() {
        UUID id = UUID.randomUUID();
        cartaoRepository.salva(new Cartao(id, "PicPay", null, Dinheiro.de("300.00"),
                null, true));

        cartaoRepository.remove(id);

        assertThat(cartaoRepository.buscaAtivos()).extracting(Cartao::id).doesNotContain(id);
        assertThat(cartaoSpringDataJpaRepository.findById(id))
                .as("a linha precisa continuar no banco, só marcada como inativa")
                .hasValueSatisfying(entidade -> assertThat(entidade.isAtivo()).isFalse());
    }

    @Test
    @DisplayName("remove num id inexistente não lança (idempotente)")
    void removeIdempotente() {
        cartaoRepository.remove(UUID.randomUUID());
    }

    @Test
    @DisplayName("remove chamado duas vezes seguidas não lança")
    void removeDuasVezesNaoLanca() {
        UUID id = UUID.randomUUID();
        cartaoRepository.salva(new Cartao(id, "Itaú", null, Dinheiro.de("100.00"),
                null, true));

        cartaoRepository.remove(id);
        cartaoRepository.remove(id);

        assertThat(cartaoRepository.buscaAtivos()).extracting(Cartao::id).doesNotContain(id);
    }
}
