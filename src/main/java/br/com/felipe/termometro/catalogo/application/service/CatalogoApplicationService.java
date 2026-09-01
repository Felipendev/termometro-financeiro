package br.com.felipe.termometro.catalogo.application.service;

import br.com.felipe.termometro.catalogo.application.api.request.CustoFixoItemRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRotativaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.PisoHumanoRequest;
import br.com.felipe.termometro.catalogo.application.api.request.RendaRequest;
import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogoApplicationService implements CatalogoService {

    private final CatalogoRepository catalogoRepository;

    @Override
    public List<CustoFixoItem> listaCustoFixo() {
        return catalogoRepository.buscaCustoFixoAtivo();
    }

    @Override
    public List<PisoHumano> listaPisoHumano() {
        return catalogoRepository.buscaPisoHumano();
    }

    @Override
    public Renda buscaRenda(Competencia competencia) {
        return catalogoRepository.buscaRenda(competencia)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Nenhuma renda declarada para " + competencia + "."));
    }

    @Override
    public List<Divida> listaDividasAtivas(Competencia competencia) {
        return catalogoRepository.buscaDividasAtivas(competencia);
    }

    @Override
    public List<DividaRotativa> listaDividasRotativasAtivas() {
        return catalogoRepository.buscaDividasRotativasAtivas();
    }

    @Override
    public Renda declaraRenda(Competencia competencia, RendaRequest request) {
        log.info("[inicia] CatalogoApplicationService - declaraRenda [{}]", competencia);
        Renda renda = request.paraDominio(competencia);
        catalogoRepository.salvaRenda(renda);
        log.info("[finaliza] CatalogoApplicationService - declaraRenda [{}]", competencia);
        return renda;
    }

    @Override
    public CustoFixoItem salvaCustoFixo(UUID id, CustoFixoItemRequest request) {
        log.info("[inicia] CatalogoApplicationService - salvaCustoFixo [{}]", id);
        CustoFixoItem item = catalogoRepository.salvaCustoFixo(request.paraDominio(id));
        log.info("[finaliza] CatalogoApplicationService - salvaCustoFixo [{}]", id);
        return item;
    }

    @Override
    public PisoHumano salvaPisoHumano(String categoria, PisoHumanoRequest request) {
        log.info("[inicia] CatalogoApplicationService - salvaPisoHumano [{}]", categoria);
        PisoHumano piso = catalogoRepository.salvaPisoHumano(request.paraDominio(categoria));
        log.info("[finaliza] CatalogoApplicationService - salvaPisoHumano [{}]", categoria);
        return piso;
    }

    @Override
    public void removePisoHumano(String categoria) {
        log.info("[inicia] CatalogoApplicationService - removePisoHumano [{}]", categoria);
        catalogoRepository.removePisoHumano(categoria);
        log.info("[finaliza] CatalogoApplicationService - removePisoHumano [{}]", categoria);
    }

    @Override
    public Divida salvaDivida(UUID id, DividaRequest request) {
        log.info("[inicia] CatalogoApplicationService - salvaDivida [{}]", id);
        Divida divida = catalogoRepository.salvaDivida(paraDivida(id, request));
        log.info("[finaliza] CatalogoApplicationService - salvaDivida [{}]", id);
        return divida;
    }

    @Override
    public void removeDivida(UUID id) {
        log.info("[inicia] CatalogoApplicationService - removeDivida [{}]", id);
        catalogoRepository.removeDivida(id);
        log.info("[finaliza] CatalogoApplicationService - removeDivida [{}]", id);
    }

    @Override
    public DividaRotativa salvaDividaRotativa(UUID id, DividaRotativaRequest request) {
        log.info("[inicia] CatalogoApplicationService - salvaDividaRotativa [{}]", id);
        DividaRotativa dividaRotativa = catalogoRepository.salvaDividaRotativa(request.paraDominio(id));
        log.info("[finaliza] CatalogoApplicationService - salvaDividaRotativa [{}]", id);
        return dividaRotativa;
    }

    @Override
    public void removeDividaRotativa(UUID id) {
        log.info("[inicia] CatalogoApplicationService - removeDividaRotativa [{}]", id);
        catalogoRepository.removeDividaRotativa(id);
        log.info("[finaliza] CatalogoApplicationService - removeDividaRotativa [{}]", id);
    }

    /**
     * {@code DividaRequest.paraDominio} chama {@code Competencia.parse}, que lança
     * {@code DateTimeParseException} não verificada — traduzida aqui pro mesmo formato RFC 7807
     * (400) que todo parse de competência do sistema usa, em vez de vazar como 500.
     */
    private Divida paraDivida(UUID id, DividaRequest request) {
        try {
            return request.paraDominio(id);
        } catch (DateTimeParseException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Competência inválida: '" + request.competenciaUltimaParcela()
                            + "'. Use o formato AAAA-MM.", e);
        }
    }
}
