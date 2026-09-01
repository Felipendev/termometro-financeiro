import { somarDinheiro } from "./format";
import type { DinheiroStr, LancamentoPlanejadoResponse } from "./types";

type Referencia = { id: string; nome: string };
export type ResumoDeMeio = Referencia & {
  despesas: DinheiroStr;
  receitas: DinheiroStr;
  saldo: DinheiroStr;
};

export function resumirFluxoDoMes(movimentos: LancamentoPlanejadoResponse[]): {
  receitas: DinheiroStr;
  despesas: DinheiroStr;
  saldo: DinheiroStr;
} {
  const ativos = movimentos.filter((item) => item.tipo !== "TRANSFERENCIA" && item.status !== "CANCELADO");
  const receitas = somarDinheiro(ativos.filter((item) => item.tipo === "RECEITA").map((item) => item.valor));
  const despesas = somarDinheiro(ativos.filter((item) => item.tipo === "DESPESA").map((item) => item.valor));
  return { receitas, despesas, saldo: somarDinheiro([receitas, `-${despesas}`]) };
}

function acumular(mapa: Map<string, ResumoDeMeio>, id: string, nome: string,
                  tipo: "DESPESA" | "RECEITA", valor: DinheiroStr) {
  const atual = mapa.get(id) ?? { id, nome, despesas: "0.00", receitas: "0.00", saldo: "0.00" };
  const despesas = tipo === "DESPESA" ? somarDinheiro([atual.despesas, valor]) : atual.despesas;
  const receitas = tipo === "RECEITA" ? somarDinheiro([atual.receitas, valor]) : atual.receitas;
  mapa.set(id, { ...atual, despesas, receitas, saldo: somarDinheiro([receitas, `-${despesas}`]) });
}

export function resumirMovimentosPorMeio(
  movimentos: LancamentoPlanejadoResponse[],
  contas: Referencia[],
  cartoes: Referencia[],
  nomesDeCartoesImportados: string[],
): { contas: ResumoDeMeio[]; cartoes: ResumoDeMeio[] } {
  const contasPorId = new Map(contas.map((conta) => [conta.id, conta.nome]));
  const contaIdPorNome = new Map(contas.map((conta) => [conta.nome.toLocaleLowerCase("pt-BR"), conta.id]));
  const cartoesPorId = new Map(cartoes.map((cartao) => [cartao.id, cartao.nome]));
  const cartaoIdPorNome = new Map(cartoes.map((cartao) => [cartao.nome.toLocaleLowerCase("pt-BR"), cartao.id]));
  const nomesImportados = new Set(nomesDeCartoesImportados.map((nome) => nome.toLocaleLowerCase("pt-BR")));
  const resumoContas = new Map<string, ResumoDeMeio>();
  const resumoCartoes = new Map<string, ResumoDeMeio>();

  movimentos
    .filter((item) => item.tipo !== "TRANSFERENCIA" && item.status !== "CANCELADO")
    .forEach((item) => {
      const tipo = item.tipo as "DESPESA" | "RECEITA";
      if (item.cartaoManualId) {
        acumular(resumoCartoes, item.cartaoManualId,
          cartoesPorId.get(item.cartaoManualId) ?? "Cartão", tipo, item.valor);
        return;
      }
      const contaId = tipo === "RECEITA" ? item.contaDestinoId : item.contaOrigemId;
      if (contaId) {
        acumular(resumoContas, contaId, contasPorId.get(contaId) ?? "Conta", tipo, item.valor);
        return;
      }
      if (!item.contaOuCartao) return;
      const nomeNormalizado = item.contaOuCartao.toLocaleLowerCase("pt-BR");
      if (cartoesPorId.has(item.contaOuCartao)) {
        acumular(resumoCartoes, item.contaOuCartao,
          cartoesPorId.get(item.contaOuCartao) ?? "Cartão", tipo, item.valor);
        return;
      }
      const cartaoId = cartaoIdPorNome.get(nomeNormalizado);
      if (cartaoId || nomesImportados.has(nomeNormalizado)) {
        acumular(resumoCartoes, cartaoId ?? `importado:${item.contaOuCartao}`,
          item.contaOuCartao, tipo, item.valor);
        return;
      }
      const id = contaIdPorNome.get(nomeNormalizado) ?? `importado:${item.contaOuCartao}`;
      acumular(resumoContas, id, item.contaOuCartao, tipo, item.valor);
    });

  return { contas: [...resumoContas.values()], cartoes: [...resumoCartoes.values()] };
}
