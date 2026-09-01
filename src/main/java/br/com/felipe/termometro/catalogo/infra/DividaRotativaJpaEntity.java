package br.com.felipe.termometro.catalogo.infra;

import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "divida_rotativa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DividaRotativaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "saldo_devedor", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoDevedor;

    @Column(name = "taxa_juros_mensal", nullable = false, precision = 8, scale = 6)
    private BigDecimal taxaJurosMensal;

    @Column(name = "taxa_estimada", nullable = false)
    private boolean taxaEstimada;

    @Column(name = "observacao")
    private String observacao;

    DividaRotativaJpaEntity(DividaRotativa dividaRotativa) {
        this.id = dividaRotativa.id();
        this.nome = dividaRotativa.nome();
        this.saldoDevedor = dividaRotativa.saldoDevedor().valor();
        this.taxaJurosMensal = dividaRotativa.taxaJurosMensal().fracao();
        this.taxaEstimada = dividaRotativa.taxaEstimada();
        this.observacao = dividaRotativa.observacao();
    }

    DividaRotativa paraDominio() {
        return new DividaRotativa(id, nome, Dinheiro.de(saldoDevedor), Percentual.deFracao(taxaJurosMensal),
                taxaEstimada, observacao);
    }
}
