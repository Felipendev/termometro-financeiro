package br.com.felipe.termometro.handler;

import java.io.Serial;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Exceção de negócio que já carrega o status HTTP e o corpo da resposta.
 *
 * <p>Regra de fronteira do projeto: <b>violação de regra de negócio vira {@code APIException}</b>
 * (o cliente errou e precisa saber o quê); <b>erro de programação vira
 * {@code IllegalArgumentException} ou {@code IllegalStateException}</b> (nós erramos, e isso é bug,
 * não resposta 4xx). O domínio — {@code Dinheiro}, {@code CalculadoraDeVerbaDiaria} — não conhece
 * esta classe e continua testável sem Spring.
 */
public class APIException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient HttpStatus statusException;
    private final transient ErrorApiResponse bodyException;

    private APIException(HttpStatus statusException, String message, @Nullable Exception causa) {
        super(message, causa);
        this.statusException = statusException;
        this.bodyException = new ErrorApiResponse(message, descricaoDe(causa));
    }

    public static APIException build(HttpStatus statusException, String message) {
        return new APIException(statusException, message, null);
    }

    public static APIException build(HttpStatus statusException, String message, Exception causa) {
        return new APIException(statusException, message, causa);
    }

    public HttpStatus getStatusException() {
        return statusException;
    }

    public ErrorApiResponse getBodyException() {
        return bodyException;
    }

    public ResponseEntity<ErrorApiResponse> buildErrorResponseEntity() {
        return ResponseEntity.status(statusException).body(bodyException);
    }

    private static @Nullable String descricaoDe(@Nullable Exception causa) {
        return Optional.ofNullable(causa)
                .map(e -> e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                .orElse(null);
    }
}
