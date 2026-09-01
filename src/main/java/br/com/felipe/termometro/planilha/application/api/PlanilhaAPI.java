package br.com.felipe.termometro.planilha.application.api;
import br.com.felipe.termometro.planilha.application.api.request.*;import br.com.felipe.termometro.planilha.application.api.response.*;import jakarta.validation.Valid;import java.time.LocalDate;import org.springframework.web.bind.annotation.*;
@RequestMapping("/v1/planilha") public interface PlanilhaAPI {
    @GetMapping PlanilhaMesResponse consulta(@RequestParam String competencia);
    @PutMapping("/saldo-inicial") SaldoInicialResponse defineSaldoInicial(@RequestBody @Valid SaldoInicialRequest request);
    @PutMapping("/{data}/diario") void sobrescreveDiario(@PathVariable LocalDate data, @RequestBody @Valid DiarioRequest request);
    @PutMapping("/diario-serie") void sobrescreveDiarioEmSerie(@RequestBody @Valid DiarioSerieRequest request);
    @PutMapping("/{data}/observacao") void defineObservacao(@PathVariable LocalDate data, @RequestBody @Valid ObservacaoRequest request);
    @PostMapping("/{data}/lancamentos") void adicionaLancamento(@PathVariable LocalDate data, @RequestBody @Valid LancamentoDaPlanilhaRequest request);
    @PutMapping("/{data}/lancamentos/{id}") void editaLancamento(@PathVariable LocalDate data, @PathVariable java.util.UUID id, @RequestBody @Valid LancamentoDaPlanilhaRequest request);
    @DeleteMapping("/lancamentos/{id}") void removeLancamento(@PathVariable java.util.UUID id);
    @PostMapping("/simular-decisao") SimulacaoDecisaoResponse simulaDecisao(@RequestBody @Valid SimularDecisaoRequest request);
    @PostMapping("/simular-decisao/confirmar") ConfirmarDecisaoResponse confirmaDecisao(@RequestBody @Valid ConfirmarDecisaoRequest request);
}
