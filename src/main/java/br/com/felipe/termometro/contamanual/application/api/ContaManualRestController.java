package br.com.felipe.termometro.contamanual.application.api;
import br.com.felipe.termometro.contamanual.application.api.request.ContaManualRequest;
import br.com.felipe.termometro.contamanual.application.api.response.ContaManualResponse;
import br.com.felipe.termometro.contamanual.application.service.ContaManualApplicationService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequiredArgsConstructor public class ContaManualRestController implements ContaManualAPI {
 private final ContaManualApplicationService service;
 public List<ContaManualResponse> lista(){return service.listaAtivas().stream().map(ContaManualResponse::new).toList();}
 public ContaManualResponse salva(UUID id,ContaManualRequest request){return new ContaManualResponse(service.salva(id,request));}
 public void remove(UUID id){service.remove(id);}
}
