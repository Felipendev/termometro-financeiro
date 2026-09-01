package br.com.felipe.termometro.contamanual.infra;

import br.com.felipe.termometro.contamanual.domain.ContaManual;
import br.com.felipe.termometro.contamanual.domain.TipoContaManual;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "conta_manual") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
class ContaManualJpaEntity {
    @Id private UUID id;
    @Column(nullable = false) private String identificador;
    @Column(nullable = false) private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoContaManual tipo;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal saldo;
    @Column(nullable = false) private boolean ativa;
    @Column(name = "atualizado_em", nullable = false) private OffsetDateTime atualizadoEm;
    ContaManualJpaEntity(ContaManual conta) { id=conta.id(); identificador=conta.identificador(); nome=conta.nome(); tipo=conta.tipo(); saldo=conta.saldo().valor(); ativa=conta.ativa(); atualizadoEm=OffsetDateTime.now(); }
    void desativa() { ativa=false; atualizadoEm=OffsetDateTime.now(); }
    ContaManual paraDominio() { return new ContaManual(id, identificador, nome, tipo, Dinheiro.de(saldo), ativa); }
}
