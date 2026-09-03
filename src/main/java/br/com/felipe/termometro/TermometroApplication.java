package br.com.felipe.termometro;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// UserDetailsServiceAutoConfiguration excluída: login é via auth/JwtAuthenticationFilter (JWT
// validado à mão, sem AuthenticationManager/UserDetailsService) — sem a exclusão, o Boot cria um
// usuário em memória com senha aleatória que nunca é usado, só suja o log de start.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableAsync
public class TermometroApplication {

    public static void main(String[] args) {
        carregaEnvLocal();
        SpringApplication.run(TermometroApplication.class, args);
    }

    /**
     * Carrega um {@code .env} na raiz do projeto (git-ignorado — ver {@code .gitignore}) antes de
     * subir o contexto Spring, pra dev local não depender de configuração de ambiente da IDE.
     *
     * <p>Motivo de existir: {@code PLUGGY_CLIENT_ID}/{@code PLUGGY_CLIENT_SECRET} setados
     * corretamente no SO ainda assim não chegavam certos ao processo — algo na cadeia de
     * configuração da IDE (run configuration com environment variables próprias, ou plugin de
     * env file apontando pra um arquivo desatualizado) sobrepunha o valor certo por um errado, e
     * não tinha como diagnosticar isso de fora. Um {@code .env} lido pelo próprio código remove
     * essa cadeia inteira — o valor usado é sempre exatamente o que está no arquivo.
     *
     * <p><b>Nunca sobrescreve</b> uma variável de ambiente ou system property que já exista:
     * {@code .env} é só um fallback de conveniência para dev local, quem manda de verdade é a
     * variável de ambiente real (CI, produção, ou até um {@code export} manual no terminal).
     */
    private static void carregaEnvLocal() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entrada -> {
            if (System.getProperty(entrada.getKey()) == null && System.getenv(entrada.getKey()) == null) {
                System.setProperty(entrada.getKey(), entrada.getValue());
            }
        });
    }
}
