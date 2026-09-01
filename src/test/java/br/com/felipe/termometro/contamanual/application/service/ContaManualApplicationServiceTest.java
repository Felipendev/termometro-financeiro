package br.com.felipe.termometro.contamanual.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.com.felipe.termometro.contamanual.application.api.request.ContaManualRequest;
import br.com.felipe.termometro.contamanual.application.repository.ContaManualRepository;
import br.com.felipe.termometro.contamanual.domain.ContaManual;
import br.com.felipe.termometro.contamanual.domain.TipoContaManual;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ContaManualApplicationServiceTest {
    @Test
    void salvaContaComSaldoInformado() {
        ContaManualRepository repository = Mockito.mock(ContaManualRepository.class);
        UUID id = UUID.randomUUID();
        ContaManual conta = new ContaManual(id, "principal", "Conta principal", TipoContaManual.CORRENTE, Dinheiro.de("100"), true);
        Mockito.when(repository.salva(Mockito.any())).thenReturn(conta);
        ContaManualApplicationService service = new ContaManualApplicationService(repository);

        ContaManual resultado = service.salva(id, new ContaManualRequest("principal", "Conta principal", "CORRENTE", new BigDecimal("100")));

        assertThat(resultado).isEqualTo(conta);
        verify(repository).salva(conta);
    }
}
