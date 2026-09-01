package br.com.felipe.termometro.naogasto.application.api;

import br.com.felipe.termometro.naogasto.application.api.response.ResultadoDaConciliacaoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * RN-03 — motor automático de "não é gasto": pagamento de fatura de cartão, transferência entre
 * contas próprias e estorno/chargeback. Roda sob pedido (mesmo formato de
 * {@code POST /v1/triagem/{competencia}}), olhando a competência informada e os meses anteriores
 * necessários pra cada casador — ver Javadoc de {@code NaoGastoApplicationService} pros números.
 */
@RestController
@RequestMapping("/v1/nao-gasto")
public interface NaoGastoAPI {

    @PostMapping("/{competencia}")
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaConciliacaoResponse concilia(@PathVariable String competencia);
}
