package br.com.felipe.termometro.catalogo.application.service;

import br.com.felipe.termometro.catalogo.application.api.request.CustoFixoItemRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRotativaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.PisoHumanoRequest;
import br.com.felipe.termometro.catalogo.application.api.request.RendaRequest;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import java.util.UUID;

/**
 * Porta de entrada do catálogo (RN-17). Único módulo que ganha esta camada só agora — até a
 * fatia 13 o catálogo era leitura pura e o controller falava direto com {@code CatalogoRepository}
 * (exceção ao padrão do resto do sistema). Com escrita chegando, alinha com o padrão de todo mundo
 * ({@code OrcamentoService}, {@code TriagemService}, ...): controller fala com o serviço, nunca com
 * o repositório. Outros módulos ({@code diagnostico}, {@code projecao}, {@code reserva},
 * {@code dashboard}) continuam lendo direto de {@code CatalogoRepository} — só a API pública ganha
 * esta camada.
 */
public interface CatalogoService {

    List<CustoFixoItem> listaCustoFixo();

    List<PisoHumano> listaPisoHumano();

    /** @throws br.com.felipe.termometro.handler.APIException 404 se não houver renda para a competência */
    Renda buscaRenda(Competencia competencia);

    List<Divida> listaDividasAtivas(Competencia competencia);

    List<DividaRotativa> listaDividasRotativasAtivas();

    Renda declaraRenda(Competencia competencia, RendaRequest request);

    CustoFixoItem salvaCustoFixo(UUID id, CustoFixoItemRequest request);

    PisoHumano salvaPisoHumano(String categoria, PisoHumanoRequest request);

    void removePisoHumano(String categoria);

    Divida salvaDivida(UUID id, DividaRequest request);

    void removeDivida(UUID id);

    DividaRotativa salvaDividaRotativa(UUID id, DividaRotativaRequest request);

    void removeDividaRotativa(UUID id);
}
