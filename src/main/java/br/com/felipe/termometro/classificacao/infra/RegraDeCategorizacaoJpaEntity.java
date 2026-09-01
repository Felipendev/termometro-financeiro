package br.com.felipe.termometro.classificacao.infra;

import br.com.felipe.termometro.classificacao.domain.Categoria;
import br.com.felipe.termometro.classificacao.domain.GrupoDeCategoria;
import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.classificacao.domain.OrigemDaRegra;
import br.com.felipe.termometro.classificacao.domain.RegraDeCategorizacao;
import br.com.felipe.termometro.classificacao.domain.TipoDeRegra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Regra criada pelo usuário ou aprendida a partir de uma correção dele (RN-12).
 *
 * <p>O catálogo do sistema <b>não</b> vive aqui: ele é código ({@code CatalogoDeRegrasPadrao}),
 * versionado junto com o resto, para que uma mudança de regra apareça no diff em vez de sumir
 * dentro de um INSERT de migration.
 */
@Entity
@Table(name = "regra_categorizacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RegraDeCategorizacaoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "prioridade", nullable = false)
    private int prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDeRegra tipo;

    @Column(name = "padrao", nullable = false)
    private String padrao;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo", nullable = false)
    private GrupoDeCategoria grupo;

    @Enumerated(EnumType.STRING)
    @Column(name = "natureza", nullable = false)
    private Natureza natureza;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false)
    private OrigemDaRegra origem;

    RegraDeCategorizacaoJpaEntity(RegraDeCategorizacao regra) {
        this.id = UUID.randomUUID();
        this.prioridade = regra.prioridade();
        this.tipo = regra.tipo();
        this.padrao = regra.padrao();
        this.categoria = regra.categoria().nome();
        this.grupo = regra.categoria().grupo();
        this.natureza = regra.categoria().natureza();
        this.origem = regra.origem();
    }

    RegraDeCategorizacao paraDominio() {
        return new RegraDeCategorizacao(prioridade, tipo, padrao,
                new Categoria(categoria, grupo, natureza), origem);
    }
}
