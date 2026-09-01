package br.com.felipe.termometro.config;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serialização dos value objects do shared kernel.
 *
 * <p><b>Dinheiro e Percentual trafegam como string.</b> Number em JSON vira {@code double}
 * no JavaScript e volta com centavo errado — {@code 1234.56} pode virar
 * {@code 1234.5599999999999}. Para um sistema cujo produto é a data de quitação de uma
 * dívida, isso não é detalhe de formatação.
 */
@Configuration
public class JsonConfig {

    @Bean
    public SimpleModule moduloDoDominio() {
        SimpleModule modulo = new SimpleModule("termometro-shared-kernel");

        modulo.addSerializer(Dinheiro.class, new JsonSerializer<>() {
            @Override
            public void serialize(Dinheiro valor, JsonGenerator gen, SerializerProvider p)
                    throws IOException {
                gen.writeString(valor.paraJson());
            }
        });
        modulo.addDeserializer(Dinheiro.class, new JsonDeserializer<>() {
            @Override
            public Dinheiro deserialize(JsonParser p, DeserializationContext ctx)
                    throws IOException {
                return Dinheiro.de(p.getValueAsString());
            }
        });

        modulo.addSerializer(Percentual.class, new JsonSerializer<>() {
            @Override
            public void serialize(Percentual valor, JsonGenerator gen, SerializerProvider p)
                    throws IOException {
                gen.writeString(valor.paraJson());
            }
        });
        modulo.addDeserializer(Percentual.class, new JsonDeserializer<>() {
            @Override
            public Percentual deserialize(JsonParser p, DeserializationContext ctx)
                    throws IOException {
                return Percentual.deFracao(p.getValueAsString());
            }
        });

        modulo.addSerializer(Competencia.class, new JsonSerializer<>() {
            @Override
            public void serialize(Competencia valor, JsonGenerator gen, SerializerProvider p)
                    throws IOException {
                gen.writeString(valor.toString());
            }
        });
        modulo.addDeserializer(Competencia.class, new JsonDeserializer<>() {
            @Override
            public Competencia deserialize(JsonParser p, DeserializationContext ctx)
                    throws IOException {
                return Competencia.parse(p.getValueAsString());
            }
        });

        return modulo;
    }
}
