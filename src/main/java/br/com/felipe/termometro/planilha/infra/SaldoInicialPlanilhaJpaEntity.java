package br.com.felipe.termometro.planilha.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saldo_inicial_planilha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaldoInicialPlanilhaJpaEntity {

    @Id
    private Short id;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;
}
