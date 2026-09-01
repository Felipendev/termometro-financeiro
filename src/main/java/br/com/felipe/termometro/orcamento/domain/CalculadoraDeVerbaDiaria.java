package br.com.felipe.termometro.orcamento.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * O motor do Termômetro (RN-19 e RN-20).
 *
 * <p>A conta central é uma só:
 * <pre>
 *   verba_de_hoje = (dia_a_dia_disponível − gasto_até_ontem) / dias_restantes
 * </pre>
 * e a consequência é o que faz a regra funcionar: <b>ela se recalcula todo dia</b>. Gastou mais
 * ontem, a de hoje encolhe; segurou ontem, a de hoje cresce. É um orçamento que responde, não um
 * teto que se quebra e vira culpa.
 *
 * <p>Usa <b>gasto até ontem</b>, não até hoje: o que já se gastou hoje sai da verba de hoje na
 * hora de decidir, mas não deve encolher a própria verba que ainda está sendo consumida.
 *
 * <p>Domínio puro: recebe {@link Clock}, não consulta relógio de sistema, não conhece banco.
 */
public final class CalculadoraDeVerbaDiaria {

    /** Depois disso a lista de ações vira parede de texto em vez de sugestão. */
    private static final int MAXIMO_DE_ACOES = 3;
    /** Dias 1 e 2 calculam ritmo mas não alertam (edge case 33). */
    private static final int DIA_MINIMO_PARA_CONFIAR_NO_RITMO = 3;
    private static final int DIAS_DE_ANTECEDENCIA_DO_EVENTO = 3;
    private static final int ESCALA_DA_PROPORCAO = 6;

    private final List<TicketMedio> tickets;

    public CalculadoraDeVerbaDiaria(List<TicketMedio> tickets) {
        this.tickets = List.copyOf(Objects.requireNonNull(tickets, "tickets não podem ser nulos"));
    }

    public static CalculadoraDeVerbaDiaria padrao() {
        return new CalculadoraDeVerbaDiaria(TicketMedio.medidosEmAgosto2026());
    }

    public VerbaDoDia calcular(VerbaMensal verba, Collection<GastoDoDia> gastos,
                               Collection<Evento> eventos, Clock relogio) {
        Objects.requireNonNull(verba, "verba não pode ser nula");
        Objects.requireNonNull(gastos, "gastos não podem ser nulos");
        Objects.requireNonNull(eventos, "eventos não podem ser nulos");
        Objects.requireNonNull(relogio, "relógio não pode ser nulo");

        Competencia competencia = verba.competencia();
        LocalDate hoje = LocalDate.now(relogio);
        if (!competencia.contem(hoje)) {
            throw new IllegalArgumentException(
                    "a verba é de " + competencia + " e hoje é " + hoje);
        }

        int diaDoMes = hoje.getDayOfMonth();
        int diasDoMes = competencia.quantidadeDeDias();
        int diasRestantes = diasDoMes - diaDoMes + 1;

        // RN-20: o que a provisão não cobre é descontado do dia a dia — e o usuário sabe disso antes.
        Dinheiro totalDeEventos = Dinheiro.somaDe(
                eventos.stream().filter(e -> competencia.contem(e.data())).map(Evento::valor).toList());
        Dinheiro excedenteDeEventos =
                totalDeEventos.subtrair(verba.provisao()).maximo(Dinheiro.ZERO);
        Dinheiro diaADiaDisponivel =
                verba.diaADia().subtrair(excedenteDeEventos).maximo(Dinheiro.ZERO);

        Dinheiro gastoAteOntem = somar(gastos, competencia, g -> g.data().isBefore(hoje));
        Dinheiro gastoAteHoje = somar(gastos, competencia, g -> !g.data().isAfter(hoje));

        Dinheiro verbaDeHoje = diaADiaDisponivel.subtrair(gastoAteOntem)
                .maximo(Dinheiro.ZERO)
                .dividirPor(BigDecimal.valueOf(diasRestantes));
        Dinheiro restanteDoMes = diaADiaDisponivel.subtrair(gastoAteHoje).maximo(Dinheiro.ZERO);

        Dinheiro verbaBase = diaADiaDisponivel.dividirPor(BigDecimal.valueOf(diasDoMes));
        FaixaSaude faixa = verbaBase.ehZero()
                ? FaixaSaude.PESSIMO
                : FaixaSaude.de(proporcao(verbaDeHoje, verbaBase));

        BigDecimal ritmo = calcularRitmo(gastoAteHoje, diaADiaDisponivel, diaDoMes, diasDoMes);
        boolean baixaConfianca = diaDoMes < DIA_MINIMO_PARA_CONFIAR_NO_RITMO;

        List<Evento> proximos = eventos.stream()
                .filter(e -> e.aindaVaiAcontecer(hoje))
                .filter(e -> !e.data().isAfter(hoje.plusDays(DIAS_DE_ANTECEDENCIA_DO_EVENTO)))
                .sorted(java.util.Comparator.comparing(Evento::data))
                .toList();

        List<AcaoPossivel> podeFazer = traduzirEmAcoes(verbaDeHoje);
        String mensagem = mensagemPara(faixa, verbaDeHoje, restanteDoMes, diasRestantes,
                podeFazer, proximos, excedenteDeEventos, baixaConfianca);

        return new VerbaDoDia(hoje, verbaDeHoje, verbaBase, gastoAteHoje, restanteDoMes,
                totalDeEventos, diasRestantes, faixa, ritmo, baixaConfianca,
                podeFazer, proximos, mensagem);
    }

    private static Dinheiro somar(Collection<GastoDoDia> gastos, Competencia competencia,
                                  java.util.function.Predicate<GastoDoDia> filtro) {
        return Dinheiro.somaDe(gastos.stream()
                .filter(g -> competencia.contem(g.data()))
                .filter(filtro)
                .map(GastoDoDia::valor)
                .toList());
    }

    private static BigDecimal proporcao(Dinheiro parte, Dinheiro base) {
        return BigDecimal.valueOf(parte.centavos())
                .divide(BigDecimal.valueOf(base.centavos()), ESCALA_DA_PROPORCAO, RoundingMode.HALF_EVEN);
    }

    /** Gastou 50% da verba em 17% do mês? Ritmo 3,0 — vai estourar, mesmo parecendo confortável. */
    private static BigDecimal calcularRitmo(Dinheiro gastoAteHoje, Dinheiro disponivel,
                                            int diaDoMes, int diasDoMes) {
        if (disponivel.ehZero()) {
            return BigDecimal.ZERO;
        }
        BigDecimal consumido = proporcao(gastoAteHoje, disponivel);
        BigDecimal fracaoDoMes = BigDecimal.valueOf(diaDoMes)
                .divide(BigDecimal.valueOf(diasDoMes), ESCALA_DA_PROPORCAO, RoundingMode.HALF_EVEN);
        return consumido.divide(fracaoDoMes, 2, RoundingMode.HALF_EVEN);
    }

    private List<AcaoPossivel> traduzirEmAcoes(Dinheiro verbaDeHoje) {
        List<AcaoPossivel> acoes = new ArrayList<>();
        for (TicketMedio ticket : tickets) {
            long quantas = verbaDeHoje.centavos() / ticket.valor().centavos();
            if (quantas >= 1) {
                acoes.add(AcaoPossivel.de(ticket, (int) Math.min(quantas, 99)));
            }
            if (acoes.size() == MAXIMO_DE_ACOES) {
                break;
            }
        }
        return List.copyOf(acoes);
    }

    private static String mensagemPara(FaixaSaude faixa, Dinheiro verbaDeHoje, Dinheiro restante,
                                       int diasRestantes, List<AcaoPossivel> acoes,
                                       List<Evento> proximos, Dinheiro excedenteDeEventos,
                                       boolean baixaConfianca) {
        StringBuilder texto = new StringBuilder();
        if (verbaDeHoje.ehZero()) {
            texto.append("A verba do mês acabou. Restam ").append(diasRestantes)
                 .append(diasRestantes == 1 ? " dia" : " dias").append(" — só o essencial.");
        } else if (faixa == FaixaSaude.PESSIMO || faixa == FaixaSaude.RUIM) {
            texto.append("Sobram ").append(restante).append(" para ").append(diasRestantes)
                 .append(diasRestantes == 1 ? " dia" : " dias").append(" — ")
                 .append(verbaDeHoje).append(" hoje. ").append(faixa.leitura()).append(".");
        } else {
            texto.append("Você tem ").append(verbaDeHoje).append(" hoje");
            if (!acoes.isEmpty()) {
                texto.append(": dá para ").append(acoes.get(0).frase());
                if (acoes.size() > 1) {
                    texto.append(" ou ").append(acoes.get(1).frase());
                }
            }
            texto.append(".");
        }
        if (excedenteDeEventos.ehPositivo()) {
            texto.append(" Os eventos do mês passaram da provisão em ").append(excedenteDeEventos)
                 .append(", então a verba diária já veio ajustada.");
        }
        for (Evento evento : proximos) {
            texto.append(" ").append(evento.descricao()).append(" em ").append(evento.data())
                 .append(": ").append(evento.valor()).append(" já reservados.");
        }
        if (baixaConfianca) {
            texto.append(" (Começo de mês — dois dias não predizem trinta.)");
        }
        return texto.toString();
    }
}
