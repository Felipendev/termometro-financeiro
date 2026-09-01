package br.com.felipe.termometro.catalogo.infra;

import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
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
@Table(name = "custo_fixo_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CustoFixoItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "forma_pagamento")
    private String formaPagamento;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    CustoFixoItemJpaEntity(CustoFixoItem item) {
        this.id = item.id();
        this.nome = item.nome();
        this.valor = item.valor().valor();
        this.formaPagamento = item.formaPagamento();
        this.observacao = item.observacao();
        this.ativo = item.ativo();
    }

    CustoFixoItem paraDominio() {
        return new CustoFixoItem(id, nome, Dinheiro.de(valor), formaPagamento, observacao, ativo);
    }
}
