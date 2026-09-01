package br.com.felipe.termometro.orcamento.infra;

import br.com.felipe.termometro.orcamento.domain.Evento;
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
@Table(name = "evento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class EventoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "realizado", nullable = false)
    private boolean realizado;

    EventoJpaEntity(Competencia competencia, Evento evento) {
        this.id = UUID.randomUUID();
        this.competencia = competencia.primeiroDia();
        this.data = evento.data();
        this.descricao = evento.descricao();
        this.valor = evento.valor().valor();
        this.realizado = evento.realizado();
    }

    Evento paraDominio() {
        return new Evento(data, descricao, Dinheiro.de(valor), realizado);
    }
}
