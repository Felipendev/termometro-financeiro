package br.com.felipe.termometro.handler;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(APIException.class)
    public ResponseEntity<ErrorApiResponse> handleApiException(APIException ex) {
        return ex.buildErrorResponseEntity();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorApiResponse> handleParametroObrigatorio(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorApiResponse.de("Parâmetro obrigatório ausente: " + ex.getParameterName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApiResponse> handleCorpoIncompativel(HttpMessageNotReadableException ex) {
        log.warn("Corpo JSON inválido ou incompatível com o contrato atual: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ErrorApiResponse.de(
                "Os dados enviados não correspondem à versão atual do servidor. "
                        + "Atualize ou reinicie a interface e tente novamente."));
    }

    /**
     * Erro de programação que vazou até a borda. Vira 500 e some do corpo da resposta — mensagem de
     * exceção interna não vai para o cliente, vai para o log.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorApiResponse> handleErroDeProgramacao(RuntimeException ex) {
        log.error("Invariante do domínio violada na borda: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorApiResponse.de("Erro interno. Avise o administrador do sistema."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApiResponse> handleGenericException(Exception ex) {
        log.error("Exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorApiResponse("Avise o administrador do sistema.", "INTERNAL SERVER ERROR"));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(erro ->
                erros.put(((FieldError) erro).getField(), erro.getDefaultMessage()));
        return erros;
    }
}
