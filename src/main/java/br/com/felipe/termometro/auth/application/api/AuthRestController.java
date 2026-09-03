package br.com.felipe.termometro.auth.application.api;
import br.com.felipe.termometro.auth.application.api.request.LoginRequest;
import br.com.felipe.termometro.auth.application.api.response.LoginResponse;
import br.com.felipe.termometro.auth.application.service.AuthApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequiredArgsConstructor public class AuthRestController implements AuthAPI {
 private final AuthApplicationService service;
 public LoginResponse login(LoginRequest request){return service.login(request);}
}
