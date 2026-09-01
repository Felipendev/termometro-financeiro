package br.com.felipe.termometro.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes que tocam o banco.
 *
 * <p>Postgres de verdade via Testcontainers, com o Flyway rodando as mesmas migrations da
 * produção. H2 aceitaria SQL que o Postgres recusa — e é justamente nas {@code check constraints}
 * e no {@code numeric(14,2)} que este projeto se apoia.
 *
 * <p>O container é {@code static}: um por execução da suíte, não um por classe.
 *
 * <p><b>{@code disabledWithoutDocker}:</b> sem Docker no ambiente, estes testes são <i>ignorados</i>
 * em vez de quebrarem o build. Quebrar o build de quem não tem Docker ligado esconde o resultado
 * dos 153 testes de domínio atrás de um erro de infraestrutura.
 *
 * <p>Mas <b>ignorado não é aprovado</b>: enquanto estes testes não rodarem, a camada de persistência
 * está sem verificação nenhuma. Em CI, rode com {@code -Dintegracao.obrigatoria=true} — aí a
 * ausência de Docker volta a ser falha, que é o comportamento certo lá.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("integracao")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BancoDeTesteIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("termometro")
                    .withUsername("termometro")
                    .withPassword("termometro");

    @BeforeAll
    static void exigeDockerQuandoObrigatorio() {
        if (Boolean.getBoolean("integracao.obrigatoria") && !DockerClientFactory.instance().isDockerAvailable()) {
            throw new IllegalStateException(
                    "integracao.obrigatoria=true mas o Docker não está disponível: "
                            + "os testes de persistência não podem ser ignorados aqui.");
        }
    }

    @DynamicPropertySource
    static void propriedadesDeTeste(DynamicPropertyRegistry registro) {
        // Não usar @ServiceConnection: o Spring Boot 3.3 não reconhece de forma confiável a
        // linha 2.x do Testcontainers. As propriedades explícitas preservam o banco efêmero e
        // impedem que um DB_URL local desvie a suíte para o banco de desenvolvimento.
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
