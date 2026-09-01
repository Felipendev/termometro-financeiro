package br.com.felipe.termometro.catalogo.application.repository;

import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída do catálogo. Outros módulos (hoje, {@code diagnostico} e {@code projecao})
 * dependem desta interface, nunca de {@code catalogo.infra} — é o que permite ler as premissas
 * sem acoplar em Postgres.
 */
public interface CatalogoRepository {

    List<CustoFixoItem> buscaCustoFixoAtivo();

    List<PisoHumano> buscaPisoHumano();

    Optional<Renda> buscaRenda(Competencia competencia);

    /**
     * Histórico de renda até {@code ate} (inclusive), da competência mais recente para a mais
     * antiga, limitado a {@code quantidade} registros. Base da detecção de queda estrutural
     * (RN-16.1): precisa de ao menos 6 para comparar duas medianas de 3.
     */
    List<Renda> buscaHistoricoDeRenda(Competencia ate, int quantidade);

    /** Dívidas de parcela fixa que ainda têm parcela caindo na competência informada (RN-08). */
    List<Divida> buscaDividasAtivas(Competencia competencia);

    /** Saldos rotativos com saldo devedor positivo — o que o motor de projeção simula (RN-09). */
    List<DividaRotativa> buscaDividasRotativasAtivas();

    // ------------------------------------------------------------------ escrita (fatia 13, CRUD)

    /** Upsert por competência — {@code renda.competencia()} é a chave. */
    void salvaRenda(Renda renda);

    /** Upsert por {@code item.id()} — id sempre vem preenchido (gerado no cliente pra item novo). */
    CustoFixoItem salvaCustoFixo(CustoFixoItem item);

    /**
     * Upsert por {@code piso.categoria()} — chave natural do domínio. A UUID interna do banco
     * (surrogate key da tabela) é resolvida aqui dentro: reaproveitada se a categoria já existe,
     * gerada se for nova. Ninguém acima desta porta precisa saber que essa UUID existe.
     */
    PisoHumano salvaPisoHumano(PisoHumano piso);

    /** Idempotente — categoria inexistente não é erro. */
    void removePisoHumano(String categoria);

    /** Upsert por {@code divida.id()}. */
    Divida salvaDivida(Divida divida);

    /** Idempotente — id inexistente não é erro. Só corrige cadastro; quitação é natural por data. */
    void removeDivida(UUID id);

    /** Upsert por {@code dividaRotativa.id()}. */
    DividaRotativa salvaDividaRotativa(DividaRotativa dividaRotativa);

    /** Idempotente — id inexistente não é erro. Quitar de verdade é editar o saldo pra 0. */
    void removeDividaRotativa(UUID id);
}
