package br.com.felipe.termometro.notificacao.domain;

import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.FaixaSaude;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * RN-22 — decide, a partir do que os outros domínios já calcularam, <b>se</b> um alerta deve
 * disparar e <b>o quê</b> ele carrega. Puro: não conhece Telegram, não conhece banco, e não sabe
 * se já avisou antes — a deduplicação (não repetir o mesmo aviso todo dia) é responsabilidade de
 * quem chama, via {@code EstadoDeAlertaRepository}.
 */
public final class DecisorDeAlerta {

    private DecisorDeAlerta() {
    }

    /**
     * Só avisa de novo no mesmo dia se a faixa piorou desde o último aviso (ex.: RUIM → PÉSSIMO).
     * Repetir "ainda está RUIM" 4 vezes ao dia — uma por sync automático — é ruído, não alerta.
     */
    public static boolean verbaPiorou(FaixaSaude atual, @Nullable FaixaSaude ultimaAvisadaHoje) {
        Objects.requireNonNull(atual, "faixa atual não pode ser nula");
        boolean baixa = atual == FaixaSaude.RUIM || atual == FaixaSaude.PESSIMO;
        if (!baixa) {
            return false;
        }
        return ultimaAvisadaHoje == null || atual.ordinal() > ultimaAvisadaHoje.ordinal();
    }

    /** Só despesas (RN-01: saída negativa) acima do limite — entrada/estorno não é gasto. */
    public static List<TransacaoBruta> transacoesAcimaDoLimite(List<TransacaoBruta> transacoes, Dinheiro limite) {
        Objects.requireNonNull(transacoes, "transações não podem ser nulas");
        Objects.requireNonNull(limite, "limite não pode ser nulo");
        return transacoes.stream()
                .filter(TransacaoBruta::ehDespesa)
                .filter(t -> t.valor().absoluto().maiorQue(limite))
                .toList();
    }

    /**
     * A projeção sempre recomeça do zero em "hoje" (mesma ressalva já documentada na RN-21) —
     * então isto só é {@code true} quando o próprio mês corrente já é o mês do marco.
     */
    public static boolean marcoAtingidoAgora(@Nullable Competencia marco, Competencia hoje) {
        Objects.requireNonNull(hoje, "hoje não pode ser nulo");
        return marco != null && marco.equals(hoje);
    }

    public static String mensagemVerbaBaixa(VerbaDoDia verba) {
        Objects.requireNonNull(verba, "verba não pode ser nula");
        return "Verba apertada agora: " + verba.faixa().leitura() + ". " + verba.mensagem();
    }

    public static String mensagemTransacoesAltas(List<TransacaoBruta> altas, Dinheiro limite) {
        Objects.requireNonNull(altas, "transações altas não podem ser nulas");
        Objects.requireNonNull(limite, "limite não pode ser nulo");
        StringBuilder texto = new StringBuilder("Transações acima de ").append(limite)
                .append(" nesta sincronização:");
        for (TransacaoBruta t : altas) {
            texto.append("\n").append(t.data()).append(" — ").append(t.descricao()).append(": ")
                    .append(t.valor().absoluto());
        }
        return texto.toString();
    }

    public static String mensagemMarco(String rotulo, Competencia competencia) {
        Objects.requireNonNull(rotulo, "rótulo não pode ser nulo");
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        return "Marco atingido em " + competencia + ": " + rotulo + ".";
    }

    public static String mensagemEvento(Evento evento) {
        Objects.requireNonNull(evento, "evento não pode ser nulo");
        return "Evento chegando: " + evento.descricao() + " em " + evento.data() + " — "
                + evento.valor() + " já reservados.";
    }
}
