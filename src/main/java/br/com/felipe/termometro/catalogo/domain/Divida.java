package br.com.felipe.termometro.catalogo.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma dívida com parcela fixa e prazo conhecido (RN-08: {@code ServicoDivida}). Versão reduzida
 * do {@code divida} completo da especificação — sem saldo devedor nem taxa de juros, porque isso
 * só importa para a projeção de quitação (RN-09), ainda não implementada.
 */
public record Divida(UUID id, String nome, Dinheiro valorParcela,
                      Competencia competenciaUltimaParcela, String observacao) {

    public Divida {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(valorParcela, "valor da parcela não pode ser nulo");
        Objects.requireNonNull(competenciaUltimaParcela, "competência da última parcela não pode ser nula");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        if (!valorParcela.ehPositivo()) {
            throw new IllegalArgumentException("parcela precisa ser positiva: " + valorParcela);
        }
    }

    /** Se a dívida ainda tem parcela caindo na competência informada. */
    public boolean ativaEm(Competencia competencia) {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        return competencia.compareTo(competenciaUltimaParcela) <= 0;
    }
}
