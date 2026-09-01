package br.com.felipe.termometro.catalogo.infra;

import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.shared.Dinheiro;
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
@Table(name = "piso_humano")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PisoHumanoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Column(name = "valor_piso", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorPiso;

    @Column(name = "justificativa")
    private String justificativa;

    @Column(name = "estimado", nullable = false)
    private boolean estimado;

    PisoHumanoJpaEntity(java.util.UUID id, PisoHumano piso) {
        this.id = id;
        this.categoria = piso.categoria();
        this.valorPiso = piso.valorPiso().valor();
        this.justificativa = piso.justificativa();
        this.estimado = piso.estimado();
    }

    PisoHumano paraDominio() {
        return new PisoHumano(categoria, Dinheiro.de(valorPiso), justificativa, estimado);
    }
}
