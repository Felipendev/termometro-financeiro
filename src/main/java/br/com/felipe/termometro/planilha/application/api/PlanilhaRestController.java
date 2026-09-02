package br.com.felipe.termometro.planilha.application.api;

import br.com.felipe.termometro.planilha.application.api.request.ConfirmarDecisaoRequest;
import br.com.felipe.termometro.planilha.application.api.request.DiarioRequest;
import br.com.felipe.termometro.planilha.application.api.request.DiarioSerieRequest;
import br.com.felipe.termometro.planilha.application.api.request.ObservacaoRequest;
import br.com.felipe.termometro.planilha.application.api.request.LancamentoDaPlanilhaRequest;
import br.com.felipe.termometro.planilha.application.api.request.SaldoInicialRequest;
import br.com.felipe.termometro.planilha.application.api.request.SimularDecisaoRequest;
import br.com.felipe.termometro.planilha.application.api.response.ConfirmarDecisaoResponse;
import br.com.felipe.termometro.planilha.application.api.response.PlanilhaMesResponse;
import br.com.felipe.termometro.planilha.application.api.response.SaldoInicialResponse;
import br.com.felipe.termometro.planilha.application.api.response.SimulacaoDecisaoResponse;
import br.com.felipe.termometro.planilha.application.service.PlanilhaService;
import br.com.felipe.termometro.planilha.application.service.SimuladorDeDecisaoService;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlanilhaRestController implements PlanilhaAPI {

    private final PlanilhaService planilhaService;
    private final SimuladorDeDecisaoService simuladorDeDecisaoService;

    @Override
    public PlanilhaMesResponse consulta(String competencia) {
        return PlanilhaMesResponse.de(planilhaService.consulta(Competencia.parse(competencia)));
    }

    @Override
    public SaldoInicialResponse defineSaldoInicial(SaldoInicialRequest request) {
        var saldo = planilhaService.defineSaldoInicial(request.dataReferencia(), Dinheiro.de(request.valor()));
        return SaldoInicialResponse.de(saldo);
    }

    @Override
    public void sobrescreveDiario(LocalDate data, DiarioRequest request) {
        planilhaService.sobrescreveDiario(data, Dinheiro.de(request.valor()));
    }

    @Override
    public void sobrescreveDiarioEmSerie(DiarioSerieRequest request) {
        planilhaService.sobrescreveDiarioEmSerie(request.de(), request.ate(), Dinheiro.de(request.valor()));
    }

    @Override
    public void defineObservacao(LocalDate data, ObservacaoRequest request) {
        planilhaService.defineObservacao(data, request.texto());
    }

    @Override
    public void adicionaLancamento(LocalDate data, LancamentoDaPlanilhaRequest request) {
        planilhaService.adicionaLancamento(request.paraDominio(UUID.randomUUID(), data));
    }

    @Override
    public void editaLancamento(LocalDate data, UUID id, LancamentoDaPlanilhaRequest request) {
        planilhaService.editaLancamento(request.paraDominio(id, data), request.escopo());
    }

    @Override
    public void removeLancamento(UUID id) {
        planilhaService.removeLancamento(id);
    }

    @Override
    public SimulacaoDecisaoResponse simulaDecisao(SimularDecisaoRequest request) {
        var resultado = simuladorDeDecisaoService.simula(
                request.paraComando(), Competencia.parse(request.de()), Competencia.parse(request.ate()));
        return SimulacaoDecisaoResponse.de(resultado);
    }

    @Override
    public ConfirmarDecisaoResponse confirmaDecisao(ConfirmarDecisaoRequest request) {
        return new ConfirmarDecisaoResponse(simuladorDeDecisaoService.confirma(request.paraComando()));
    }
}
