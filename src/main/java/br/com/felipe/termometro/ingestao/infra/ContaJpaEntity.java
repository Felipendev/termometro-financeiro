package br.com.felipe.termometro.ingestao.infra;

import br.com.felipe.termometro.ingestao.domain.ContaBancaria;
import br.com.felipe.termometro.ingestao.domain.TipoDeConta;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "conta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "identificador", nullable = false, unique = true)
    private String identificador;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDeConta tipo;

    @Column(name = "limite", precision = 14, scale = 2)
    private @Nullable BigDecimal limite;

    @Column(name = "saldo", precision = 14, scale = 2)
    private @Nullable BigDecimal saldo;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    ContaJpaEntity(UUID id, ContaBancaria conta) {
        this.id = id;
        atualiza(conta);
    }

    /** Upsert: reaproveita o id gerado na primeira vez, atualiza o que o banco reportou de novo. */
    void atualiza(ContaBancaria conta) {
        this.identificador = conta.identificador();
        this.nome = conta.nome();
        this.tipo = conta.tipo();
        this.limite = conta.limiteOpcional().map(Dinheiro::valor).orElse(null);
        this.saldo = conta.saldo() == null ? null : conta.saldo().valor();
        this.atualizadoEm = OffsetDateTime.now();
    }

    /**
     * {@code idExterno} não é persistido — só é usado dentro do sync (para chamar a Pluggy) e
     * nunca mais depois disso; reaproveita {@code identificador} aqui, sem perda prática.
     */
    public ContaBancaria paraDominio() {
        return new ContaBancaria(
                identificador,
                identificador,
                nome,
                tipo,
                null,
                saldo == null ? Dinheiro.ZERO : Dinheiro.de(saldo),
                limite == null ? null : Dinheiro.de(limite));
    }
}
