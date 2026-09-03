package br.com.felipe.termometro.auth.application.service;

import br.com.felipe.termometro.auth.application.api.request.LoginRequest;
import br.com.felipe.termometro.auth.application.api.response.LoginResponse;
import br.com.felipe.termometro.handler.APIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Login de usuário único (app pessoal, sem cadastro) — credenciais vêm de env, não de banco. Ver
 * contexto completo em {@link JwtService}.
 */
@Service
public class AuthApplicationService {

    private final String usuarioConfigurado;
    private final String hashSenhaConfigurada;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthApplicationService(
            @Value("${app.security.username}") String usuarioConfigurado,
            @Value("${app.security.password-hash}") String hashSenhaConfigurada,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioConfigurado = usuarioConfigurado;
        this.hashSenhaConfigurada = hashSenhaConfigurada;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        boolean usuarioCorreto = usuarioConfigurado.equals(request.usuario());
        boolean senhaCorreta = passwordEncoder.matches(request.senha(), hashSenhaConfigurada);
        if (!usuarioCorreto || !senhaCorreta) {
            throw APIException.build(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
        }
        JwtService.LoginToken token = jwtService.emite(usuarioConfigurado);
        return new LoginResponse(token.token(), token.expiraEm());
    }
}
