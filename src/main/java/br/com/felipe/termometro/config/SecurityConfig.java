package br.com.felipe.termometro.config;

import br.com.felipe.termometro.auth.infra.JwtAuthenticationFilter;
import br.com.felipe.termometro.handler.ErrorApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Login único do app (RN de hospedagem pública — ver {@link
 * br.com.felipe.termometro.auth.application.service.AuthApplicationService}). {@code
 * /v1/auth/login} é aberto; o resto de {@code /termometro/api/**} exige o JWT (ver {@link
 * JwtAuthenticationFilter}); a raiz e os assets do front-end buildado continuam públicos — senão a
 * própria tela de login não carregaria.
 *
 * <p>Sem sessão de servidor (stateless) e sem cookie ⇒ sem CSRF a proteger. CORS reaproveita a
 * config já registrada em {@link CorsConfig} (Spring Security detecta automaticamente as
 * {@code @CrossOrigin}/{@code addCorsMappings} do MVC quando nenhum {@code CorsConfigurationSource}
 * próprio é declarado).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPointNaoAutenticado()))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/termometro/api/v1/auth/**")
                                        .permitAll()
                                        .requestMatchers("/termometro/api/**")
                                        .authenticated()
                                        .anyRequest()
                                        .permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Substitui o 403 HTML padrão por {@link ErrorApiResponse}, o mesmo formato de erro do resto da API. */
    private org.springframework.security.web.AuthenticationEntryPoint entryPointNaoAutenticado() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(ErrorApiResponse.de("Não autenticado")));
        };
    }
}
