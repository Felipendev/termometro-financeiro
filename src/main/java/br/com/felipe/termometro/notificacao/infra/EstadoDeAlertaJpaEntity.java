package br.com.felipe.termometro.notificacao.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estado_de_alerta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class EstadoDeAlertaJpaEntity {

    @Id
    @Column(name = "chave", nullable = false)
    private String chave;

    @Column(name = "valor", nullable = false)
    private String valor;

    EstadoDeAlertaJpaEntity(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }
}
