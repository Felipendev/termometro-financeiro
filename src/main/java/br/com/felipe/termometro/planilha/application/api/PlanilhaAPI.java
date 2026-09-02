package br.com.felipe.termometro.planilha.application.api;
import br.com.felipe.termometro.planilha.application.api.request.*;import br.com.felipe.termometro.planilha.application.api.response.*;import jakarta.validation.Valid;import java.time.LocalDate;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RequestMapping("/v1/planilha") public interface PlanilhaAPI {
    @GetMapping @ResponseStatus(HttpStatus.OK) PlanilhaMesResponse consulta(@RequestParam String competencia);
    @PutMapping("/saldo-inicial") @ResponseStatus(HttpStatus.OK) SaldoInicialResponse defineSaldoInicial(@RequestBody @Valid SaldoInicialRequest request);
    @PutMapping("/{data}/diario") @ResponseStatus(HttpStatus.NO_CONTENT) void sobrescreveDiario(@PathVariable LocalDate data, @RequestBody @Valid DiarioRequest request);
    @PutMapping("/diario-serie") @ResponseStatus(HttpStatus.NO_CONTENT) void sobrescreveDiarioEmSerie(@RequestBody @Valid DiarioSerieRequest request);
    @PutMapping("/{data}/observacao") @ResponseStatus(HttpStatus.NO_CONTENT) void defineObservacao(@PathVariable LocalDate data, @RequestBody @Valid ObservacaoRequest request);
    @PostMapping("/{data}/lancamentos") @ResponseStatus(HttpStatus.NO_CONTENT) void adicionaLancamento(@PathVariable LocalDate data, @RequestBody @Valid LancamentoDaPlanilhaRequest request);
    @PutMapping("/{data}/lancamentos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void editaLancamento(@PathVariable LocalDate data, @PathVariable java.util.UUID id, @RequestBody @Valid LancamentoDaPlanilhaRequest request);
    @DeleteMapping("/lancamentos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void removeLancamento(@PathVariable java.util.UUID id);
    @PostMapping("/simular-decisao") @ResponseStatus(HttpStatus.OK) SimulacaoDecisaoResponse simulaDecisao(@RequestBody @Valid SimularDecisaoRequest request);
    @PostMapping("/simular-decisao/confirmar") @ResponseStatus(HttpStatus.OK) ConfirmarDecisaoResponse confirmaDecisao(@RequestBody @Valid ConfirmarDecisaoRequest request);
}
