package br.com.felipe.termometro.compromissofuturo.infra;

import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
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
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "compromisso_futuro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CompromissoFuturoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "identificador_conta", nullable = false)
    private String identificadorConta;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "descricao_normalizada", nullable = false)
    private String descricaoNormalizada;

    @Column(name = "categoria")
    private @Nullable String categoria;

    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "parcela_numero", nullable = false)
    private int parcelaNumero;

    @Column(name = "parcela_total", nullable = false)
    private int parcelaTotal;

    @Column(name = "confirmado", nullable = false)
    private boolean confirmado;

    CompromissoFuturoJpaEntity(CompromissoFuturo compromisso) {
        this.id = UUID.randomUUID();
        this.identificadorConta = compromisso.identificadorConta();
        this.descricao = compromisso.descricao();
        this.descricaoNormalizada = compromisso.descricaoNormalizada();
        this.categoria = compromisso.categoria();
        this.competencia = compromisso.competencia().primeiroDia();
        this.valor = compromisso.valor().valor();
        this.parcelaNumero = compromisso.parcelaNumero();
        this.parcelaTotal = compromisso.parcelaTotal();
        this.confirmado = compromisso.confirmado();
    }

    CompromissoFuturo paraDominio() {
        return new CompromissoFuturo(id, identificadorConta, descricao, descricaoNormalizada, categoria,
                Competencia.de(competencia), Dinheiro.de(valor), parcelaNumero, parcelaTotal, confirmado);
    }
}
