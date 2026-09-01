package br.com.felipe.termometro.contamanual.application.api.response;
import br.com.felipe.termometro.contamanual.domain.ContaManual;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.UUID;
public record ContaManualResponse(UUID id,String identificador,String nome,String tipo,Dinheiro saldo){ public ContaManualResponse(ContaManual c){this(c.id(),c.identificador(),c.nome(),c.tipo().name(),c.saldo());} }
