package br.com.felipe.termometro.contamanual.application.api.request;

import br.com.felipe.termometro.contamanual.domain.ContaManual;
import br.com.felipe.termometro.contamanual.domain.TipoContaManual;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ContaManualRequest(@NotBlank String identificador, @NotBlank String nome,
                                 @NotBlank String tipo, @NotNull BigDecimal saldo) {
    public ContaManual paraDominio(UUID id) {
        return new ContaManual(id, identificador, nome, TipoContaManual.valueOf(tipo), Dinheiro.de(saldo), true);
    }
}
