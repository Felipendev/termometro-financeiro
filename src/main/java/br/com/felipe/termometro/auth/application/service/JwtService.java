package br.com.felipe.termometro.auth.application.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite e valida o JWT da sessão única do app (RN de login, ver {@link
 * br.com.felipe.termometro.config.SecurityConfig}). Sem tabela de usuário — o "subject" do token é
 * sempre o {@code app.security.username} configurado; validar o token equivale a confirmar que
 * quem pediu login conhecia a senha configurada.
 *
 * @param segredo chave HMAC, {@code app.security.jwt-secret} (env {@code JWT_SECRET}) — base64 de
 *     ao menos 256 bits, ex.: {@code openssl rand -base64 32}
 * @param expiracaoDias {@code app.security.jwt-expiracao-dias} (env {@code JWT_EXPIRACAO_DIAS})
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoDias;

    public JwtService(
            @Value("${app.security.jwt-secret}") String segredo,
            @Value("${app.security.jwt-expiracao-dias}") long expiracaoDias) {
        this.chave = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(segredo));
        this.expiracaoDias = expiracaoDias;
    }

    public LoginToken emite(String usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(expiracaoDias, ChronoUnit.DAYS);
        String token =
                Jwts.builder()
                        .subject(usuario)
                        .issuedAt(java.util.Date.from(agora))
                        .expiration(java.util.Date.from(expiraEm))
                        .signWith(chave)
                        .compact();
        return new LoginToken(token, expiraEm);
    }

    /** Devolve o usuário do token se válido e não expirado; vazio caso contrário (nunca lança). */
    public Optional<String> validaEExtraiUsuario(String token) {
        try {
            String usuario = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload().getSubject();
            return Optional.ofNullable(usuario);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record LoginToken(String token, Instant expiraEm) {}
}
