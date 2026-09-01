package br.com.felipe.termometro.config;

import br.com.felipe.termometro.orcamento.domain.CalculadoraDeVerbaDiaria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O motor da verba é domínio puro e não carrega anotação de Spring — quem o publica como bean é a
 * configuração. É isso que mantém {@code CalculadoraDeVerbaDiaria} testável sem contexto.
 */
@Configuration
public class OrcamentoConfig {

    @Bean
    public CalculadoraDeVerbaDiaria calculadoraDeVerbaDiaria() {
        return CalculadoraDeVerbaDiaria.padrao();
    }
}
