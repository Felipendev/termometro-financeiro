package br.com.felipe.termometro.projecao.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.MotorDeProjecao;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.projecao.domain.SaldoInicialDeDivida;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-09: busca as premissas no catálogo e monta as funções mês a mês que
 * {@link MotorDeProjecao} pede, depois delega a simulação inteira para lá.
 *
 * <p><b>Renda constante:</b> Felipe é PJ com renda fixa (R$ 10.000/mês, sem variação — a
 * queda histórica de R$ 14.000 é passado, não projeção). A renda declarada na competência de
 * início do plano é tratada como constante em todo o horizonte, em vez de exigir uma linha de
 * renda por competência futura que não existe e não deveria existir para uma renda que não
 * varia.
 *
 * <p><b>Duas fontes de dívida, dois papéis diferentes:</b> dívidas de parcela fixa
 * ({@link Divida}, ex.: empréstimo Nubank) entram em {@code saida_fixa(m)} — elas quitam
 * sozinhas, independente de estratégia, então não fazem parte da simulação dinâmica. Só o
 * saldo rotativo (o que o cartão realmente cobra juros mês a mês) entra na lista de dívidas do
 * motor, porque é o único que a estratégia de amortização tem algo a decidir sobre.
 *
 * <p><b>Saída variável = piso</b> (RN-09: "default = piso, se não houver meta") — ainda não há
 * módulo de metas por categoria, então o piso humano total é o que entra todo mês.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjecaoApplicationService implements ProjecaoService {

    /** {@code cenario.reserva_alvo_meses default 6} na spec (seção 4, schema de cenário). */
    private static final int RESERVA_ALVO_MESES = 6;

    private final CatalogoRepository catalogoRepository;
    private final CompromissoFuturoRepository compromissoFuturoRepository;
    private final TotaisMarcadosDoMes totaisMarcados;

    @Override
    public Projecao projeta(Competencia competenciaInicio, Estrategia estrategia, int horizonteMeses) {
        log.info("[inicia] ProjecaoApplicationService - projeta [competenciaInicio={}, estrategia={}]",
                competenciaInicio, estrategia);

        Renda renda = catalogoRepository.buscaRenda(competenciaInicio)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Nenhuma renda declarada para " + competenciaInicio + "."));
        Dinheiro rendaLiquidaConstante = renda.valorLiquido();

        Dinheiro custoFixoTotal = totaisMarcados.marcadoOuLegado(competenciaInicio,
                MarcacaoPlanejamento.CUSTO_FIXO, Dinheiro.somaDe(
                        catalogoRepository.buscaCustoFixoAtivo().stream().map(CustoFixoItem::valor).toList()));
        Dinheiro pisoVariavelTotal = totaisMarcados.marcadoOuLegado(competenciaInicio,
                MarcacaoPlanejamento.PISO_HUMANO, Dinheiro.somaDe(
                        catalogoRepository.buscaPisoHumano().stream().map(PisoHumano::valorPiso).toList()));
        List<Divida> dividasDeParcelaFixa = catalogoRepository.buscaDividasAtivas(competenciaInicio);
        List<SaldoInicialDeDivida> saldosRotativos = catalogoRepository.buscaDividasRotativasAtivas().stream()
                .map(d -> new SaldoInicialDeDivida(d.nome(), d.saldoDevedor(), d.taxaJurosMensal()))
                .toList();

        // RN-04: cobre o horizonte pedido E o teto fixo de MotorDeProjecao.HORIZONTE_BUSCA_BINARIA_MESES
        // (24) — a busca binária de renda extra mínima simula com esse teto mesmo quando
        // horizonteMeses for menor, e um mês fora do intervalo buscado aqui cairia no ZERO do
        // getOrDefault abaixo, subestimando a saída fixa. 24 é constante hoje; se mudar lá, muda
        // aqui também — não há como este módulo importar a constante do domínio de projecao sem
        // acoplar a assinatura pública do motor.
        Competencia fimDaBusca = competenciaInicio.mais(Math.max(horizonteMeses, 24) - 1);
        Map<Competencia, Dinheiro> compromissosFuturosPorMes = compromissoFuturoRepository
                .buscaPorPeriodo(competenciaInicio, fimDaBusca).stream()
                .collect(Collectors.groupingBy(CompromissoFuturo::competencia,
                        Collectors.reducing(Dinheiro.ZERO, CompromissoFuturo::valor, Dinheiro::somar)));

        Function<Competencia, Dinheiro> rendaLiquida = m -> rendaLiquidaConstante;
        Function<Competencia, Dinheiro> rendaExtra = m -> Dinheiro.ZERO;
        Function<Competencia, Dinheiro> saidaFixa = m -> custoFixoTotal
                .somar(Dinheiro.somaDe(
                        dividasDeParcelaFixa.stream().filter(d -> d.ativaEm(m)).map(Divida::valorParcela).toList()))
                .somar(compromissosFuturosPorMes.getOrDefault(m, Dinheiro.ZERO));
        Function<Competencia, Dinheiro> saidaVariavel = m -> pisoVariavelTotal;

        Projecao projecao = MotorDeProjecao.projetar(competenciaInicio, horizonteMeses, rendaLiquida,
                rendaExtra, saidaFixa, saidaVariavel, saldosRotativos, estrategia, RESERVA_ALVO_MESES);

        log.info("[finaliza] ProjecaoApplicationService - projeta [status={}]", projecao.status());
        return projecao;
    }
}
