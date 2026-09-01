package br.com.felipe.termometro.orcamento.infra;

import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "verba_mensal")
@Getter
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class VerbaMensalJpaEntity {

    /** A competência é a chave: existe no máximo uma verba por mês. */
    @Id
    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "verba_variavel", nullable = false, precision = 14, scale = 2)
    private BigDecimal verbaVariavel;

    @Column(name = "provisao", nullable = false, precision = 14, scale = 2)
    private BigDecimal provisao;

    @Column(name = "criado_em", updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void aoCriar() {
        OffsetDateTime agora = OffsetDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = OffsetDateTime.now();
    }

    VerbaMensalJpaEntity(VerbaMensal verba) {
        this.competencia = verba.competencia().primeiroDia();
        this.verbaVariavel = verba.verbaVariavel().valor();
        this.provisao = verba.provisao().valor();
    }

    void atualizaCom(VerbaMensal verba) {
        this.verbaVariavel = verba.verbaVariavel().valor();
        this.provisao = verba.provisao().valor();
    }

    VerbaMensal paraDominio() {
        return new VerbaMensal(Competencia.de(competencia), Dinheiro.de(verbaVariavel),
                Dinheiro.de(provisao));
    }
}
