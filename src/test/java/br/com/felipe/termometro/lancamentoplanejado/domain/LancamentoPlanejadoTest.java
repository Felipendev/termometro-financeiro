package br.com.felipe.termometro.lancamentoplanejado.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LancamentoPlanejadoTest {
 @Test void classificaVencimentoESoLiquidaUmaVez(){
  var item=new LancamentoPlanejado(UUID.randomUUID(),"Aluguel",TipoLancamentoPlanejado.DESPESA,Dinheiro.de("100"),LocalDate.of(2026,8,24),StatusLancamentoPlanejado.PENDENTE);
  assertThat(item.statusEm(LocalDate.of(2026,8,25))).isEqualTo(StatusVisualVencimento.ATRASADA);
  assertThat(item.liquidar()).isEqualTo(item.comStatus(StatusLancamentoPlanejado.LIQUIDADO));
  assertThatThrownBy(() -> item.comStatus(StatusLancamentoPlanejado.LIQUIDADO).liquidar()).isInstanceOf(IllegalStateException.class);
 }

 @Test void preservaCategoriaEFormaDePagamentoNoLancamentoManual(){
  UUID cartaoId=UUID.randomUUID();
  var categoria=new CategoriaDoLancamento("MERCADO","ALIMENTACAO","VARIAVEL");
  var item=new LancamentoPlanejado(UUID.randomUUID(),"Feira",TipoLancamentoPlanejado.DESPESA,Dinheiro.de("85.40"),LocalDate.of(2026,8,25),StatusLancamentoPlanejado.PENDENTE,null,null,categoria,cartaoId,null);

  assertThat(item.categoria()).isEqualTo(categoria);
  assertThat(item.cartaoManualId()).isEqualTo(cartaoId);
 assertThat(item.liquidar().categoria()).isEqualTo(categoria);
 }

 @Test void preservaMarcacaoQueSustentaOMesAoMudarStatus(){
  var item=new LancamentoPlanejado(UUID.randomUUID(),"Aluguel",TipoLancamentoPlanejado.DESPESA,
    Dinheiro.de("2200"),LocalDate.of(2026,8,25),StatusLancamentoPlanejado.PENDENTE,
    null,null,new CategoriaDoLancamento("Casa","MORADIA","FIXO"),null,null,
    MarcacaoPlanejamento.CUSTO_FIXO);

  assertThat(item.liquidar().marcacaoPlanejamento()).isEqualTo(MarcacaoPlanejamento.CUSTO_FIXO);
 }
}
