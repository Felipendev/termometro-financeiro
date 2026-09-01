package br.com.felipe.termometro.cartao.infra;

import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "cartao_manual")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CartaoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "limite", precision = 14, scale = 2)
    private @Nullable BigDecimal limite;

    @Column(name = "valor_fatura", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorFatura;

    @Column(name = "observacao")
    private @Nullable String observacao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    CartaoJpaEntity(Cartao cartao) {
        this.id = cartao.id();
        this.nome = cartao.nome();
        this.limite = cartao.limite() == null ? null : cartao.limite().valor();
        this.valorFatura = cartao.valorFatura().valor();
        this.observacao = cartao.observacao();
        this.ativo = cartao.ativo();
        this.atualizadoEm = OffsetDateTime.now();
    }

    /** Usado só por {@code CartaoInfraRepository#remove} — soft delete sem reconstruir o resto. */
    void desativa() {
        this.ativo = false;
        this.atualizadoEm = OffsetDateTime.now();
    }

    Cartao paraDominio() {
        return new Cartao(id, nome, limite == null ? null : Dinheiro.de(limite), Dinheiro.de(valorFatura), observacao, ativo);
    }
}
