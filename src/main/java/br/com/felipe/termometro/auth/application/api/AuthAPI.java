package br.com.felipe.termometro.auth.application.api;
import br.com.felipe.termometro.auth.application.api.request.LoginRequest;
import br.com.felipe.termometro.auth.application.api.response.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RequestMapping("/v1/auth") public interface AuthAPI {
 @PostMapping("/login") LoginResponse login(@RequestBody @Valid LoginRequest request);
}
