package br.com.felipe.termometro.cartao.fatura.application.service;

import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.fatura.application.api.request.PagamentoFaturaRequest;
import br.com.felipe.termometro.cartao.fatura.application.api.request.ValorFaturaDeclaradaRequest;
import br.com.felipe.termometro.cartao.fatura.application.api.response.FaturaCartaoResponse;
import br.com.felipe.termometro.cartao.fatura.application.api.response.FaturaCartaoResponse.PagamentoResponse;
import br.com.felipe.termometro.cartao.fatura.application.repository.PagamentoFaturaRepository;
import br.com.felipe.termometro.cartao.fatura.application.repository.FaturaDeclaradaRepository;
import br.com.felipe.termometro.cartao.fatura.domain.PagamentoFatura;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.ingestao.application.service.CartaoService;
import br.com.felipe.termometro.lancamentoplanejado.application.service.LancamentoPlanejadoApplicationService;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaturaCartaoApplicationService {
    private final CartaoService cartaoImportadoService;
    private final CartaoRepository cartoesManuais;
    private final PagamentoFaturaRepository pagamentos;
    private final FaturaDeclaradaRepository faturasDeclaradas;
    private final LancamentoPlanejadoApplicationService lancamentos;

    public List<FaturaCartaoResponse> consulta(Competencia competencia) {
        Map<String, FaturaBase> porNome = new LinkedHashMap<>();
        Map<String, Dinheiro> valoresDeclarados = faturasDeclaradas.buscaPorCompetencia(competencia);
        cartaoImportadoService.consultaCartoes(competencia).cartoes().forEach(cartao ->
                porNome.put(normaliza(cartao.nome()), new FaturaBase(
                        "IMPORTADO:" + cartao.identificador(), cartao.nome(), cartao.limite(),
                        cartao.gastoNoMes(), "IMPORTACAO")));
        cartoesManuais.buscaAtivos().forEach(cartao -> porNome.compute(normaliza(cartao.nome()),
                (chave, importada) -> importada != null && importada.valorTotal().ehPositivo()
                        ? importada
                        : new FaturaBase("MANUAL:" + cartao.id(), cartao.nome(), cartao.limite(),
                                valoresDeclarados.getOrDefault("MANUAL:" + cartao.id(),
                                        competencia.equals(Competencia.atual(java.time.Clock.system(Competencia.FUSO)))
                                                ? cartao.valorFatura() : Dinheiro.ZERO),
                                "DECLARADA")));

        Map<String, List<PagamentoFatura>> pagamentosPorReferencia = new LinkedHashMap<>();
        pagamentos.buscaPorCompetencia(competencia).forEach(pagamento ->
                pagamentosPorReferencia.computeIfAbsent(pagamento.referenciaCartao(), chave -> new ArrayList<>())
                        .add(pagamento));
        return porNome.values().stream()
                .map(base -> resposta(base, pagamentosPorReferencia.getOrDefault(base.referencia(), List.of())))
                .filter(fatura -> "DECLARADA".equals(fatura.origem())
                        || fatura.valorTotal().ehPositivo() || fatura.valorPago().ehPositivo())
                .toList();
    }

    @Transactional
    public FaturaCartaoResponse paga(Competencia competencia, PagamentoFaturaRequest request) {
        FaturaCartaoResponse fatura = consulta(competencia).stream()
                .filter(item -> item.referencia().equals(request.referencia()))
                .findFirst()
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND, "Fatura não encontrada nesta competência."));
        Dinheiro valor = Dinheiro.de(request.valor());
        if (!competencia.contem(request.dataPagamento())) {
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A data de pagamento deve pertencer ao mês da fatura.");
        }
        if (valor.maiorQue(fatura.saldoAberto())) {
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O pagamento não pode ultrapassar o saldo aberto da fatura.");
        }

        // Fatura IMPORTADA: cada compra já foi lançada/importada individualmente, então o
        // pagamento é NAO_E_GASTO (RN-03) — contar de novo aqui seria contar duas vezes.
        // Fatura DECLARADA: o usuário só digita o total, nenhuma compra existe como lançamento
        // à parte — o pagamento É a única representação do gasto, então precisa contar (FIXO,
        // pra não distorcer a verba diária com um valor agregado de uma tacada só).
        boolean importada = "IMPORTACAO".equals(fatura.origem());
        UUID lancamentoId = UUID.randomUUID();
        LancamentoPlanejado saida = new LancamentoPlanejado(lancamentoId,
                "Pagamento de fatura - " + fatura.nome(), TipoLancamentoPlanejado.DESPESA,
                valor, request.dataPagamento(), StatusLancamentoPlanejado.PENDENTE,
                request.contaOrigemId(), null,
                new CategoriaDoLancamento("Pagamento de fatura", "TRANSFERENCIA",
                        importada ? "NAO_E_GASTO" : "FIXO"),
                null, null, MarcacaoPlanejamento.NENHUMA);
        lancamentos.salva(saida);
        lancamentos.liquidar(lancamentoId);

        pagamentos.salva(new PagamentoFatura(UUID.randomUUID(), fatura.referencia(), fatura.nome(),
                competencia, valor, request.dataPagamento(), lancamentoId));
        return consulta(competencia).stream()
                .filter(item -> item.referencia().equals(request.referencia()))
                .findFirst().orElseThrow();
    }

    @Transactional
    public FaturaCartaoResponse declara(Competencia competencia, ValorFaturaDeclaradaRequest request) {
        if (!request.referencia().startsWith("MANUAL:")) {
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Faturas importadas são calculadas pelos lançamentos e não podem ser sobrescritas.");
        }
        FaturaCartaoResponse fatura = consulta(competencia).stream()
                .filter(item -> item.referencia().equals(request.referencia()))
                .findFirst()
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND, "Cartão manual não encontrado."));
        Dinheiro novoValor = Dinheiro.de(request.valor());
        if (novoValor.menorQue(fatura.valorPago())) {
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O valor da fatura não pode ficar abaixo do que já foi pago.");
        }
        faturasDeclaradas.salva(fatura.referencia(), fatura.nome(), competencia, novoValor);
        return consulta(competencia).stream()
                .filter(item -> item.referencia().equals(request.referencia()))
                .findFirst().orElseThrow();
    }

    private static FaturaCartaoResponse resposta(FaturaBase base, List<PagamentoFatura> pagamentos) {
        Dinheiro pago = Dinheiro.somaDe(pagamentos.stream().map(PagamentoFatura::valor).toList());
        Dinheiro aberto = base.valorTotal().subtrair(pago).maximo(Dinheiro.ZERO);
        String status = base.valorTotal().ehZero() && pago.ehZero() ? "SEM_MOVIMENTO"
                : pago.ehZero() ? "ABERTA" : aberto.ehZero() ? "PAGA" : "PARCIAL";
        return new FaturaCartaoResponse(base.referencia(), base.nome(), base.limite(), base.valorTotal(),
                pago, aberto, status, base.origem(), pagamentos.stream()
                        .map(item -> new PagamentoResponse(item.id().toString(), item.valor(),
                                item.dataPagamento(), item.lancamentoPlanejadoId().toString()))
                        .toList());
    }

    private static String normaliza(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private record FaturaBase(String referencia, String nome, Dinheiro limite,
            Dinheiro valorTotal, String origem) { }
}
