package br.com.felipe.termometro.vampiros.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.time.LocalDate;
import java.util.Objects;

/**
 * RN-07 — uma cobrança recorrente detectada. {@code custoAnual} é o que a UI mostra para dar
 * peso à decisão ("cancelar libera R$ X/mês, R$ Y/ano") — {@code valorMedio × 12} para
 * {@link Periodicidade#MENSAL}, o próprio {@code valorMedio} para {@link Periodicidade#ANUAL}.
 *
 * @param reajusteDetectado {@code true} quando o valor deu um degrau (todas as cobranças antigas
 *                          abaixo de todas as novas) mas a recorrência continuou regular —
 *                          {@code valorMedio} já reflete só o patamar atual
 * @param cobrancaSilenciosa {@code true} quando {@code valorMedio < R$ 50} — a faixa que passa
 *                           despercebida no extrato (RN-07). O segundo sinalizador da spec
 *                           ("sem decisão registrada há mais de 6 meses") depende de rastrear
 *                           decisão do usuário por recorrência, que esta fatia ainda não persiste
 */
public record Recorrencia(
        String nomeNormalizado,
        Periodicidade periodicidade,
        Dinheiro valorMedio,
        Dinheiro custoAnual,
        Percentual confianca,
        LocalDate primeiraOcorrencia,
        LocalDate ultimaOcorrencia,
        int ocorrencias,
        boolean reajusteDetectado,
        boolean cobrancaSilenciosa) {

    public Recorrencia {
        Objects.requireNonNull(nomeNormalizado, "nome normalizado não pode ser nulo");
        Objects.requireNonNull(periodicidade, "periodicidade não pode ser nula");
        Objects.requireNonNull(valorMedio, "valor médio não pode ser nulo");
        Objects.requireNonNull(custoAnual, "custo anual não pode ser nulo");
        Objects.requireNonNull(confianca, "confiança não pode ser nula");
        Objects.requireNonNull(primeiraOcorrencia, "primeira ocorrência não pode ser nula");
        Objects.requireNonNull(ultimaOcorrencia, "última ocorrência não pode ser nula");
        if (nomeNormalizado.isBlank()) {
            throw new IllegalArgumentException("nome normalizado não pode ser vazio");
        }
        if (!valorMedio.ehPositivo()) {
            throw new IllegalArgumentException("valor médio deve ser positivo: " + valorMedio);
        }
        if (ocorrencias < 3) {
            throw new IllegalArgumentException(
                    "recorrência precisa de ao menos 3 ocorrências, recebido: " + ocorrencias);
        }
        if (primeiraOcorrencia.isAfter(ultimaOcorrencia)) {
            throw new IllegalArgumentException("primeira ocorrência não pode ser depois da última");
        }
    }
}
