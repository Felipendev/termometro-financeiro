package br.com.felipe.termometro.vampiros.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RN-07 — validado contra os dois cenários Gherkin da spec. Ver o Javadoc de
 * {@link DetectorDeRecorrencias} para a justificativa das fórmulas de confiança que a spec não
 * fecha numericamente.
 */
class DetectorDeRecorrenciasTest {

    private static Ocorrencia em(String data, String valor) {
        return new Ocorrencia(LocalDate.parse(data), Dinheiro.de(valor));
    }

    @Nested
    @DisplayName("Detecção de assinatura mensal (Gherkin)")
    class DeteccaoMensal {

        @Test
        @DisplayName("5 cobranças entre R$39,90 e R$44,90, ~30 dias de intervalo → confiança ≥ 0,8, custo anual ≈ R$500")
        void detectaAssinaturaMensal() {
            List<Ocorrencia> ocorrencias = List.of(
                    em("2026-01-05", "39.90"),
                    em("2026-02-04", "40.90"),
                    em("2026-03-06", "42.40"),
                    em("2026-04-05", "43.90"),
                    em("2026-05-05", "44.90"));

            Optional<Recorrencia> resultado = DetectorDeRecorrencias.detectar("STREAMING Y", ocorrencias);

            assertThat(resultado).isPresent();
            Recorrencia recorrencia = resultado.get();
            assertThat(recorrencia.periodicidade()).isEqualTo(Periodicidade.MENSAL);
            assertThat(recorrencia.confianca().fracao()).isGreaterThanOrEqualTo(new java.math.BigDecimal("0.8"));
            assertThat(recorrencia.custoAnual()).isEqualTo(Dinheiro.de("508.80"));
            assertThat(recorrencia.reajusteDetectado()).isFalse();
            assertThat(recorrencia.ocorrencias()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Reajuste não quebra a recorrência (Gherkin)")
    class Reajuste {

        @Test
        @DisplayName("6 cobranças mensais, as 3 últimas 25% maiores → recorrência mantida, reajuste detectado")
        void reajusteMantemARecorrencia() {
            List<Ocorrencia> ocorrencias = List.of(
                    em("2026-01-05", "40.00"),
                    em("2026-02-04", "40.00"),
                    em("2026-03-06", "40.00"),
                    em("2026-04-05", "50.00"),
                    em("2026-05-05", "50.00"),
                    em("2026-06-04", "50.00"));

            Optional<Recorrencia> resultado = DetectorDeRecorrencias.detectar("STREAMING Y", ocorrencias);

            assertThat(resultado).isPresent();
            Recorrencia recorrencia = resultado.get();
            assertThat(recorrencia.reajusteDetectado()).isTrue();
            // valor_medio passa a considerar só o patamar atual (R$ 50), não a média dos 6
            assertThat(recorrencia.valorMedio()).isEqualTo(Dinheiro.de("50.00"));
            assertThat(recorrencia.periodicidade()).isEqualTo(Periodicidade.MENSAL);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("menos de 3 ocorrências não é recorrência")
        void poucasOcorrenciasNaoEhRecorrencia() {
            List<Ocorrencia> ocorrencias = List.of(em("2026-01-05", "40.00"), em("2026-02-04", "40.00"));
            assertThat(DetectorDeRecorrencias.detectar("X", ocorrencias)).isEmpty();
        }

        @Test
        @DisplayName("intervalo semanal (~7 dias) não bate nenhuma periodicidade reconhecida")
        void intervaloForaDasFaixasNaoEhRecorrencia() {
            List<Ocorrencia> ocorrencias = List.of(
                    em("2026-01-05", "40.00"), em("2026-01-12", "40.00"),
                    em("2026-01-19", "40.00"), em("2026-01-26", "40.00"));
            assertThat(DetectorDeRecorrencias.detectar("X", ocorrencias)).isEmpty();
        }

        @Test
        @DisplayName("variação de valor acima de 20% sem degrau monotônico não é recorrência")
        void variacaoSemDegrauNaoEhRecorrencia() {
            // alterna alto/baixo — nunca "todo antigo abaixo de todo novo"
            List<Ocorrencia> ocorrencias = List.of(
                    em("2026-01-05", "40.00"), em("2026-02-04", "70.00"),
                    em("2026-03-06", "40.00"), em("2026-04-05", "70.00"));
            assertThat(DetectorDeRecorrencias.detectar("X", ocorrencias)).isEmpty();
        }

        @Test
        @DisplayName("periodicidade anual: custo anual é o próprio valor médio, não ×12")
        void periodicidadeAnualNaoMultiplicaPor12() {
            List<Ocorrencia> ocorrencias = List.of(
                    em("2024-03-01", "120.00"), em("2025-03-02", "120.00"), em("2026-03-01", "120.00"));
            Optional<Recorrencia> resultado = DetectorDeRecorrencias.detectar("ANUIDADE", ocorrencias);
            assertThat(resultado).isPresent();
            assertThat(resultado.get().periodicidade()).isEqualTo(Periodicidade.ANUAL);
            assertThat(resultado.get().custoAnual()).isEqualTo(Dinheiro.de("120.00"));
        }

        @Test
        @DisplayName("valor médio abaixo de R$50 é sinalizado como cobrança silenciosa")
        void cobrancaSilenciosaAbaixoDe50() {
            List<Ocorrencia> ocorrencias = List.of(
                    em("2026-01-05", "19.90"), em("2026-02-04", "19.90"), em("2026-03-06", "19.90"));
            Optional<Recorrencia> resultado = DetectorDeRecorrencias.detectar("ASSINATURA PEQUENA", ocorrencias);
            assertThat(resultado).isPresent();
            assertThat(resultado.get().cobrancaSilenciosa()).isTrue();
        }

        @Test
        @DisplayName("valor médio acima de R$50 não é sinalizado como cobrança silenciosa")
        void naoSinalizaAcimaDe50() {
            List<Ocorrencia> ocorrencias = List.of(
                    em("2026-01-05", "89.90"), em("2026-02-04", "89.90"), em("2026-03-06", "89.90"));
            Optional<Recorrencia> resultado = DetectorDeRecorrencias.detectar("ASSINATURA GRANDE", ocorrencias);
            assertThat(resultado).isPresent();
            assertThat(resultado.get().cobrancaSilenciosa()).isFalse();
        }
    }
}
