package br.com.felipe.termometro.ingestao.infra;

import br.com.felipe.termometro.ingestao.domain.ChaveDeDeduplicacao;
import br.com.felipe.termometro.ingestao.domain.Normalizador;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.Parcela;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "transacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransacaoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "identificador_conta", nullable = false)
    private String identificadorConta;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "data_hora")
    private @Nullable LocalDateTime dataHora;

    @Column(name = "hora_confiavel", nullable = false)
    private boolean horaConfiavel;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "descricao_original", nullable = false)
    private String descricaoOriginal;

    /** Base do agrupamento por estabelecimento (RN-12) e do índice de similaridade. */
    @Column(name = "descricao_normalizada", nullable = false)
    private String descricaoNormalizada;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "cidade")
    private @Nullable String cidade;

    @Column(name = "categoria_banco")
    private @Nullable String categoriaBanco;

    @Enumerated(EnumType.STRING)
    @Column(name = "secao", nullable = false)
    private SecaoFatura secao;

    @Column(name = "parcela_numero")
    private @Nullable Integer parcelaNumero;

    @Column(name = "parcela_total")
    private @Nullable Integer parcelaTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false)
    private Origem origem;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    @Column(name = "evento_id")
    private @Nullable UUID eventoId;

    @Column(name = "lancamento_planejado_id")
    private @Nullable UUID lancamentoPlanejadoId;

    @Column(name = "conta_no_dia_a_dia", nullable = false)
    private boolean contaNoDiaADia;

    @Column(name = "hash_dedupe", nullable = false)
    private String hashDedupe;

    // ------------------------------------------------------- classificação (M5)
    // Preenchidos pelo módulo de classificação. Os tipos aqui são String de propósito: a
    // ingestão não conhece Categoria nem Natureza, e não pode passar a conhecer — se
    // conhecesse, o parser passaria a ter opinião sobre categoria.
    @Column(name = "categoria")
    private @Nullable String categoria;

    @Column(name = "grupo")
    private @Nullable String grupo;

    @Column(name = "natureza")
    private @Nullable String natureza;

    @Column(name = "confianca", precision = 3, scale = 2)
    private @Nullable BigDecimal confianca;

    @Column(name = "origem_regra")
    private @Nullable String origemRegra;

    @Column(name = "precisa_revisao", nullable = false)
    private boolean precisaRevisao = true;

    @Column(name = "classificado_em")
    private @Nullable OffsetDateTime classificadoEm;

    // ------------------------------------------------------------- triagem (RN-05)
    // Mesmo motivo do bloco de classificação acima: String de propósito, para que a ingestão
    // continue sem conhecer Etiqueta. Escrito pelo módulo triagem.
    @Column(name = "etiqueta")
    private @Nullable String etiqueta;

    // ---------------------------------------------------------- não é gasto (RN-03)
    // Escrito pelo módulo naogasto. Quando true, a transação não entra em NENHUM agregado — as
    // consultas de leitura de todos os módulos filtram por este campo, não cada consumidor.
    @Column(name = "ignorada", nullable = false)
    private boolean ignorada = false;

    TransacaoJpaEntity(String identificadorConta, TransacaoBruta transacao) {
        this(identificadorConta, transacao, null);
    }

    TransacaoJpaEntity(String identificadorConta, TransacaoBruta transacao,
                       @Nullable UUID lancamentoPlanejadoId) {
        this.id = UUID.randomUUID();
        this.identificadorConta = identificadorConta;
        this.data = transacao.data();
        this.dataHora = transacao.dataHora();
        this.horaConfiavel = transacao.horaConfiavel();
        this.descricao = transacao.descricao();
        this.descricaoOriginal = transacao.descricaoOriginal();
        this.descricaoNormalizada =
                Normalizador.chaveDeEstabelecimento(transacao.descricao(), transacao.cidade());
        this.valor = transacao.valor().valor();
        this.cidade = transacao.cidade();
        this.categoriaBanco = transacao.categoriaBanco();
        this.secao = transacao.secao();
        this.parcelaNumero = transacao.parcelaOpcional().map(Parcela::numero).orElse(null);
        this.parcelaTotal = transacao.parcelaOpcional().map(Parcela::total).orElse(null);
        this.origem = transacao.origem();
        this.ordinal = transacao.ordinal();
        this.eventoId = transacao.eventoId();
        this.lancamentoPlanejadoId = lancamentoPlanejadoId;
        this.contaNoDiaADia = true;     // a categorização (M5) refina isto
        this.hashDedupe = ChaveDeDeduplicacao.calcular(identificadorConta, transacao);
    }

    /**
     * Aplica o resultado da classificação (M5). Recebe String e primitivos para que este pacote
     * continue sem saber que o módulo de classificação existe.
     */
    public void aplicaClassificacao(String categoria, String grupo, String natureza,
                                    BigDecimal confianca, @Nullable String origemRegra,
                                    boolean contaNoDiaADia, boolean precisaRevisao) {
        this.categoria = categoria;
        this.grupo = grupo;
        this.natureza = natureza;
        this.confianca = confianca;
        this.origemRegra = origemRegra;
        this.contaNoDiaADia = contaNoDiaADia;
        this.precisaRevisao = precisaRevisao;
        this.classificadoEm = OffsetDateTime.now();
    }

    /** Aplica a etiqueta decidida pelo motor de triagem (RN-05), ou a promoção manual para VERMELHA. */
    public void aplicaEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Marca como não-gasto (RN-03): pagamento de fatura, transferência própria ou estorno casado. */
    public void marcaIgnorada() {
        this.ignorada = true;
    }

    public TransacaoBruta paraDominio() {
        Parcela parcela = parcelaNumero == null || parcelaTotal == null
                ? null
                : new Parcela(parcelaNumero, parcelaTotal);
        return new TransacaoBruta(data, dataHora, descricao, descricaoOriginal,
                Dinheiro.de(valor), cidade, categoriaBanco, secao, parcela, origem, ordinal);
    }
}
