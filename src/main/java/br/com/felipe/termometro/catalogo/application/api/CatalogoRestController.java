package br.com.felipe.termometro.catalogo.application.api;

import br.com.felipe.termometro.catalogo.application.api.request.CustoFixoItemRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.DividaRotativaRequest;
import br.com.felipe.termometro.catalogo.application.api.request.PisoHumanoRequest;
import br.com.felipe.termometro.catalogo.application.api.request.RendaRequest;
import br.com.felipe.termometro.catalogo.application.api.response.CustoFixoItemResponse;
import br.com.felipe.termometro.catalogo.application.api.response.DividaResponse;
import br.com.felipe.termometro.catalogo.application.api.response.DividaRotativaResponse;
import br.com.felipe.termometro.catalogo.application.api.response.PisoHumanoResponse;
import br.com.felipe.termometro.catalogo.application.api.response.RendaResponse;
import br.com.felipe.termometro.catalogo.application.service.CatalogoService;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CatalogoRestController implements CatalogoAPI {

    private final CatalogoService catalogoService;

    @Override
    public List<CustoFixoItemResponse> getCustoFixo() {
        return catalogoService.listaCustoFixo().stream().map(CustoFixoItemResponse::new).toList();
    }

    @Override
    public List<PisoHumanoResponse> getPisoHumano() {
        return catalogoService.listaPisoHumano().stream().map(PisoHumanoResponse::new).toList();
    }

    @Override
    public RendaResponse getRenda(String competencia) {
        return new RendaResponse(catalogoService.buscaRenda(competenciaDe(competencia)));
    }

    @Override
    public List<DividaResponse> getDividasAtivas(String competencia) {
        return catalogoService.listaDividasAtivas(competenciaDe(competencia)).stream()
                .map(DividaResponse::new)
                .toList();
    }

    @Override
    public List<DividaRotativaResponse> getDividasRotativasAtivas() {
        return catalogoService.listaDividasRotativasAtivas().stream()
                .map(DividaRotativaResponse::new)
                .toList();
    }

    @Override
    public RendaResponse putRenda(String competencia, RendaRequest request) {
        return new RendaResponse(catalogoService.declaraRenda(competenciaDe(competencia), request));
    }

    @Override
    public CustoFixoItemResponse putCustoFixo(UUID id, CustoFixoItemRequest request) {
        return new CustoFixoItemResponse(catalogoService.salvaCustoFixo(id, request));
    }

    @Override
    public PisoHumanoResponse putPisoHumano(String categoria, PisoHumanoRequest request) {
        return new PisoHumanoResponse(catalogoService.salvaPisoHumano(categoria, request));
    }

    @Override
    public void deletePisoHumano(String categoria) {
        catalogoService.removePisoHumano(categoria);
    }

    @Override
    public DividaResponse putDivida(UUID id, DividaRequest request) {
        return new DividaResponse(catalogoService.salvaDivida(id, request));
    }

    @Override
    public void deleteDivida(UUID id) {
        catalogoService.removeDivida(id);
    }

    @Override
    public DividaRotativaResponse putDividaRotativa(UUID id, DividaRotativaRequest request) {
        return new DividaRotativaResponse(catalogoService.salvaDividaRotativa(id, request));
    }

    @Override
    public void deleteDividaRotativa(UUID id) {
        catalogoService.removeDividaRotativa(id);
    }

    private Competencia competenciaDe(String competencia) {
        try {
            return Competencia.parse(competencia);
        } catch (DateTimeParseException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Competência inválida: '" + competencia + "'. Use o formato AAAA-MM.", e);
        }
    }
}
