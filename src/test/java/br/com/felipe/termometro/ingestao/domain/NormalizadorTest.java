package br.com.felipe.termometro.ingestao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Normalizador")
class NormalizadorTest {

    @ParameterizedTest(name = "{0} + cidade {1} -> {2}")
    @DisplayName("desgruda a cidade que o Itaú cola no fim do estabelecimento")
    @CsvSource({
            "'SUPERMERCADO ARRUDAJOAO', 'JOAO PESSOA', 'SUPERMERCADO ARRUDA'",
            "'LaisDeAraujoJOAO PESSOA', 'JOAO PESSOA', 'LAISDEARAUJO'",
            "'DeliciasDoBrejoJOAO PES', 'JOAO PESSOA', 'DELICIASDOBREJO'",
            "'UBER * PENDINGSAO PAULO', 'SAO PAULO', 'UBER * PENDING'",
            "'GRUPO FRUTOS DE GOIASJO', 'JOAO PESSOA', 'GRUPO FRUTOS DE GOIAS'",
    })
    void desgrudaCidade(String descricao, String cidade, String esperado) {
        assertThat(Normalizador.chaveDeEstabelecimento(descricao, cidade)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("o mesmo mercado com e sem cidade colada gera a mesma chave")
    void mesmaChaveParaOMesmoEstabelecimento() {
        String comCidade = Normalizador.chaveDeEstabelecimento("SUPERMERCADO ARRUDAJOAO", "JOAO PESSOA");
        String semCidade = Normalizador.chaveDeEstabelecimento("Supermercado Arruda", null);
        assertThat(comCidade)
                .as("sem isso o detector de recorrência (RN-07) nunca acumula ocorrências")
                .isEqualTo(semCidade);
    }

    @Test
    @DisplayName("não mutila nomes curtos que terminam parecidos com cidade")
    void naoMutilaNomeCurto() {
        assertThat(Normalizador.chaveDeEstabelecimento("KFC", "JOAO PESSOA")).isEqualTo("KFC");
        assertThat(Normalizador.chaveDeEstabelecimento("Bessa Pao", null)).isEqualTo("BESSA PAO");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("remove o sufixo de parcela nos três dialetos")
    @CsvSource({
            "'Amazon - Parcela 9/12', 'AMAZON'",
            "'AMAZON BR PARC10/10', 'AMAZON BR'",
            "'NOHA SHOES - J 01/04', 'NOHA SHOES J'",
            "'Pichau Informatica - NuPay - Parcela 9/12', 'PICHAU INFORMATICA NUPAY'",
    })
    void removeParcela(String descricao, String esperado) {
        assertThat(Normalizador.chaveDeEstabelecimento(descricao)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("remove acento e código longo, mantém o asterisco do adquirente")
    void limpezaGeral() {
        assertThat(Normalizador.chaveDeEstabelecimento("Panificadora São Gonça"))
                .isEqualTo("PANIFICADORA SAO GONCA");
        assertThat(Normalizador.chaveDeEstabelecimento("IFD*66.929.790 RYAN DOS"))
                .as("o CNPJ do parceiro iFood identifica o estabelecimento e fica")
                .isEqualTo("IFD*66.929.790 RYAN DOS");
    }

    @Test
    @DisplayName("unifica o prefixo de Pix entre bancos")
    void unificaPix() {
        assertThat(Normalizador.chaveDeEstabelecimento("Pix no Crédito - Arnaldo Batista"))
                .isEqualTo(Normalizador.chaveDeEstabelecimento("PIX Arnaldo Batista"));
    }

    @Test
    @DisplayName("a chave de deduplicação preserva mais do original que a de estabelecimento")
    void chavesTemPropositosDiferentes() {
        String original = "Amazon Marketplace - Parcela 8/10";
        assertThat(Normalizador.chaveDeDeduplicacao(original)).isEqualTo("AMAZON MARKETPLACE");
        assertThat(Normalizador.chaveDeDeduplicacao("Café")).isEqualTo("CAFE");
    }
}
