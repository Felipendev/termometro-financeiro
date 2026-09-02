package br.com.felipe.termometro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Prefixa toda rota de {@code @RestController} com {@code /termometro/api} no nível do
 * framework, em vez de {@code server.servlet.context-path} — que prefixaria também a serva de
 * estático (o front-end buildado), obrigando a acessar a aplicação em
 * {@code /termometro/api/} em vez da raiz do domínio. Assim a API continua em
 * {@code /termometro/api/v1/**} (nenhuma chamada do front-end muda) e o front-end fica servido
 * na raiz — essencial pra hospedar os dois num serviço só.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/termometro/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
