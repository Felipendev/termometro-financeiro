package br.com.felipe.termometro.sistema.application.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompatibilidadeRestControllerTest {

    @Test
    void publicaContratoQueAInterfaceDeveConfirmarAntesDeCarregar() {
        var resposta = new CompatibilidadeRestController().compatibilidade();

        assertThat(resposta.contratoApi())
                .isEqualTo(CompatibilidadeRestController.CONTRATO_API)
                .isNotBlank();
    }
}
