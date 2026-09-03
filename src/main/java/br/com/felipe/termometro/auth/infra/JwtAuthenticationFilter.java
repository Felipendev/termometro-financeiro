package br.com.felipe.termometro.auth.infra;

import br.com.felipe.termometro.auth.application.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê {@code Authorization: Bearer <token>}, valida via {@link JwtService} e popula o {@code
 * SecurityContext}. Sem token, ou token inválido/expirado: segue a cadeia sem autenticar — quem
 * barra rota protegida é o {@code SecurityFilterChain} (ver {@link
 * br.com.felipe.termometro.config.SecurityConfig}), não este filtro.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            String token = header.substring(PREFIXO_BEARER.length());
            jwtService
                    .validaEExtraiUsuario(token)
                    .ifPresent(
                            usuario -> {
                                var autenticacao =
                                        new UsernamePasswordAuthenticationToken(usuario, null, List.of());
                                SecurityContextHolder.getContext().setAuthentication(autenticacao);
                            });
        }
        filterChain.doFilter(request, response);
    }
}
