package br.com.felipe.termometro.contribuicao.application.api;

import br.com.felipe.termometro.contribuicao.application.api.response.MetaContribuicaoResponse;
import br.com.felipe.termometro.contribuicao.application.service.ContribuicaoService;
import br.com.felipe.termometro.contribuicao.domain.NomeDaContribuicao;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContribuicaoRestController implements ContribuicaoAPI {

    private final ContribuicaoService contribuicaoService;

    @Override
    public List<MetaContribuicaoResponse> consulta(String competencia) {
        return contribuicaoService.consulta(Competencia.parse(competencia)).stream()
                .map(MetaContribuicaoResponse::de)
                .toList();
    }

    @Override
    public MetaContribuicaoResponse autoriza(String nome, String competencia) {
        NomeDaContribuicao nomeDaContribuicao = converteNome(nome);
        contribuicaoService.autoriza(nomeDaContribuicao, Competencia.parse(competencia));
        return consulta(competencia).stream()
                .filter(item -> item.nome().equals(nomeDaContribuicao.name()))
                .findFirst()
                .orElseThrow(() -> APIException.build(HttpStatus.INTERNAL_SERVER_ERROR, "Meta sumiu depois de autorizada."));
    }

    private NomeDaContribuicao converteNome(String nome) {
        try {
            return NomeDaContribuicao.valueOf(nome.toUpperCase());
        } catch (IllegalArgumentException causa) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Meta desconhecida: " + nome, causa);
        }
    }
}
