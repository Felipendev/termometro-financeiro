package br.com.felipe.termometro.classificacao.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Faixa do dia em que a compra aconteceu (RN-12 e RN-13).
 *
 * <p>É o contexto que mais ajuda quando a descrição não diz nada: {@code PAG*IFD 8823} sozinho é
 * opaco, mas "sábado, 21h, R$ 68" quase sempre basta para o usuário lembrar o que foi.
 *
 * <p><b>Sem hora confiável, não existe período.</b> Devolve {@link Optional#empty()} em vez de
 * assumir meia-noite — inventar madrugada para uma fatura de PDF que não traz horário produziria
 * um padrão temporal que não existe (RN-13).
 */
public enum PeriodoDoDia {

    MADRUGADA(0, 5),
    MANHA(6, 11),
    TARDE(12, 17),
    NOITE(18, 23);

    private final int horaInicial;
    private final int horaFinal;

    PeriodoDoDia(int horaInicial, int horaFinal) {
        this.horaInicial = horaInicial;
        this.horaFinal = horaFinal;
    }

    public static Optional<PeriodoDoDia> de(@Nullable LocalDateTime dataHora, boolean horaConfiavel) {
        if (dataHora == null || !horaConfiavel) {
            return Optional.empty();
        }
        return de(dataHora.toLocalTime());
    }

    static Optional<PeriodoDoDia> de(LocalTime hora) {
        int h = hora.getHour();
        for (PeriodoDoDia periodo : values()) {
            if (h >= periodo.horaInicial && h <= periodo.horaFinal) {
                return Optional.of(periodo);
            }
        }
        return Optional.empty();
    }

    public String rotulo() {
        return switch (this) {
            case MADRUGADA -> "madrugada";
            case MANHA -> "manhã";
            case TARDE -> "tarde";
            case NOITE -> "noite";
        };
    }
}
