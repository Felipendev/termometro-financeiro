package br.com.felipe.termometro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS pro front-end (fatia 13, RN-11) — sem isso o dev server do Vite (porta diferente da API)
 * não consegue chamar {@code /v1/**}. Origem configurável via {@code app.cors.allowed-origins}
 * (aceita lista separada por vírgula; default o dev server padrão do Vite). Sistema é
 * single-tenant de uso pessoal — não há necessidade de uma lista dinâmica vinda de banco, só a
 * config, mesmo espírito de {@code pluggy.item-ids}.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v1/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
