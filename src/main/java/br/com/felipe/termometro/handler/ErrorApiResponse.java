package br.com.felipe.termometro.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Corpo padrão de erro da API.
 *
 * @param message     o que o cliente precisa ler
 * @param description a causa técnica, quando existir; omitida do JSON quando nula
 */
public record ErrorApiResponse(
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) @Nullable String description) {

    public static ErrorApiResponse de(String message) {
        return new ErrorApiResponse(message, null);
    }
}
