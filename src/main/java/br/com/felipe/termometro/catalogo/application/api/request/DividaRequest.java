package br.com.felipe.termometro.catalogo.application.api.request;

import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cria ou atualiza uma dívida de parcela fixa (RN-08). Upsert — o id vem do path, gerado no
 * cliente para uma dívida nova. Quitação normal não usa isto: uma dívida "acaba" sozinha quando
 * {@code competenciaUltimaParcela} passa ({@link Divida#ativaEm}) — este endpoint é só cadastro.
 */
public record DividaRequest(
        @NotBlank(message = "informe o nome da dívida") String nome,

        @NotNull(message = "informe o valor da parcela")
        @Positive(message = "o valor da parcela deve ser positivo")
        BigDecimal valorParcela,

        @NotBlank(message = "informe a competência da última parcela (AAAA-MM)")
        String competenciaUltimaParcela,

        String observacao) {

    public Divida paraDominio(UUID id) {
        return new Divida(id, nome, Dinheiro.de(valorParcela),
                Competencia.parse(competenciaUltimaParcela), observacao);
    }
}
