package br.com.felipe.termometro.contribuicao.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meta_contribuicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetaContribuicaoJpaEntity {

    @Id
    private String nome;

    @Column(name = "percentual_alvo", nullable = false)
    private BigDecimal percentualAlvo;

    @Column(name = "percentual_atual", nullable = false)
    private BigDecimal percentualAtual;

    @Column(name = "passo_incremento", nullable = false)
    private BigDecimal passoIncremento;
}
