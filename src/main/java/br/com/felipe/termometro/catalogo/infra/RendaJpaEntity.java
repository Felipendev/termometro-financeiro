package br.com.felipe.termometro.catalogo.infra;

import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "renda_declarada")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RendaJpaEntity {

    @Id
    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "valor_liquido", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorLiquido;

    @Column(name = "observacao")
    private String observacao;

    RendaJpaEntity(Renda renda) {
        this.competencia = renda.competencia().primeiroDia();
        this.valorLiquido = renda.valorLiquido().valor();
        this.observacao = renda.observacao();
    }

    Renda paraDominio() {
        return new Renda(Competencia.de(competencia), Dinheiro.de(valorLiquido), observacao);
    }
}
