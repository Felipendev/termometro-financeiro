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
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * RN-17: as premissas vêm semeadas na migration V3, calibradas com a planilha, mas "o primeiro
 * caso real de mudança" (Javadoc antigo desta interface) chegou na fatia 13 — agora tem escrita.
 * Todo {@code PUT} é upsert por chave (competência, id ou categoria, conforme o recurso): cria se
 * a chave não existe, atualiza se existe. Sem {@code POST} separado — o cliente já traz a chave
 * (id gerado no front para recursos novos), então um verbo idempotente cobre os dois casos.
 * {@code CustoFixoItem} não tem {@code DELETE}: "remover" é editar {@code ativo: false} pelo
 * próprio {@code PUT} — o campo já existe no domínio pra isso.
 */
@RestController
@RequestMapping("/v1/catalogo")
public interface CatalogoAPI {

    @GetMapping("/custo-fixo")
    @ResponseStatus(HttpStatus.OK)
    List<CustoFixoItemResponse> getCustoFixo();

    @GetMapping("/piso-humano")
    @ResponseStatus(HttpStatus.OK)
    List<PisoHumanoResponse> getPisoHumano();

    @GetMapping("/renda")
    @ResponseStatus(HttpStatus.OK)
    RendaResponse getRenda(@RequestParam String competencia);

    @GetMapping("/dividas")
    @ResponseStatus(HttpStatus.OK)
    List<DividaResponse> getDividasAtivas(@RequestParam String competencia);

    @GetMapping("/dividas-rotativas")
    @ResponseStatus(HttpStatus.OK)
    List<DividaRotativaResponse> getDividasRotativasAtivas();

    @PutMapping("/renda/{competencia}")
    @ResponseStatus(HttpStatus.OK)
    RendaResponse putRenda(@PathVariable String competencia, @RequestBody @Valid RendaRequest request);

    @PutMapping("/custo-fixo/{id}")
    @ResponseStatus(HttpStatus.OK)
    CustoFixoItemResponse putCustoFixo(@PathVariable UUID id, @RequestBody @Valid CustoFixoItemRequest request);

    @PutMapping("/piso-humano/{categoria}")
    @ResponseStatus(HttpStatus.OK)
    PisoHumanoResponse putPisoHumano(@PathVariable String categoria, @RequestBody @Valid PisoHumanoRequest request);

    @DeleteMapping("/piso-humano/{categoria}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePisoHumano(@PathVariable String categoria);

    @PutMapping("/dividas/{id}")
    @ResponseStatus(HttpStatus.OK)
    DividaResponse putDivida(@PathVariable UUID id, @RequestBody @Valid DividaRequest request);

    @DeleteMapping("/dividas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDivida(@PathVariable UUID id);

    @PutMapping("/dividas-rotativas/{id}")
    @ResponseStatus(HttpStatus.OK)
    DividaRotativaResponse putDividaRotativa(@PathVariable UUID id, @RequestBody @Valid DividaRotativaRequest request);

    @DeleteMapping("/dividas-rotativas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDividaRotativa(@PathVariable UUID id);
}
