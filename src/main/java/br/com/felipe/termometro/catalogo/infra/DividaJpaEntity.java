package br.com.felipe.termometro.catalogo.infra;

import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "divida_ativa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DividaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "valor_parcela", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "competencia_ultima_parcela", nullable = false)
    private LocalDate competenciaUltimaParcela;

    @Column(name = "observacao")
    private String observacao;

    DividaJpaEntity(Divida divida) {
        this.id = divida.id();
        this.nome = divida.nome();
        this.valorParcela = divida.valorParcela().valor();
        this.competenciaUltimaParcela = divida.competenciaUltimaParcela().primeiroDia();
        this.observacao = divida.observacao();
    }

    Divida paraDominio() {
        return new Divida(id, nome, Dinheiro.de(valorParcela),
                Competencia.de(competenciaUltimaParcela), observacao);
    }
}
