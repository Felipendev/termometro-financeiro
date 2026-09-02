import { describe, expect, it } from "vitest";
import type { LancamentoPlanejadoResponse } from "./types";
import { resumirFluxoDoMes, resumirMovimentosPorMeio } from "./resumoMeios";

function item(parcial: Partial<LancamentoPlanejadoResponse>): LancamentoPlanejadoResponse {
  return {
    id: crypto.randomUUID(), descricao: "item", tipo: "DESPESA", valor: "0.00",
    vencimento: "2026-08-20", status: "LIQUIDADO", contaOrigemId: null,
    contaDestinoId: null, categoria: null, cartaoManualId: null, transacaoId: null,
    marcacaoPlanejamento: "NENHUMA", contaOuCartao: null, editavel: true, origem: "MANUAL",
    origemReceita: null, serieId: null, diaRecorrencia: null,
    ...parcial,
  };
}

describe("resumirMovimentosPorMeio", () => {
  it("agrupa a competência por conta e cartão sem contar transferências", () => {
    const resumo = resumirMovimentosPorMeio([
      item({ descricao: "Mercado", valor: "120.00", contaOrigemId: "conta-1" }),
      item({ descricao: "Salário", tipo: "RECEITA", valor: "5000.00", contaDestinoId: "conta-1" }),
      item({ descricao: "Aluguel", valor: "2200.00", cartaoManualId: "cartao-1" }),
      item({ descricao: "Streaming", valor: "39.80", editavel: false, origem: "CSV", contaOuCartao: "Nubank" }),
      item({ descricao: "Pix", tipo: "TRANSFERENCIA", valor: "100.00", contaOrigemId: "conta-1", contaDestinoId: "conta-2" }),
    ], [{ id: "conta-1", nome: "Conta Itaú" }, { id: "conta-2", nome: "Reserva" }],
    [{ id: "cartao-1", nome: "PicPay" }], ["Nubank"]);

    expect(resumo.contas).toEqual([
      { id: "conta-1", nome: "Conta Itaú", despesas: "120.00", receitas: "5000.00", saldo: "4880.00" },
    ]);
    expect(resumo.cartoes).toEqual([
      { id: "cartao-1", nome: "PicPay", despesas: "2200.00", receitas: "0.00", saldo: "-2200.00" },
      { id: "importado:Nubank", nome: "Nubank", despesas: "39.80", receitas: "0.00", saldo: "-39.80" },
    ]);
  });

  it("resume entradas e saídas pelos movimentos sem transferências ou cancelados", () => {
    expect(resumirFluxoDoMes([
      item({ tipo: "RECEITA", valor: "5000.00" }),
      item({ tipo: "DESPESA", valor: "120.40" }),
      item({ tipo: "TRANSFERENCIA", valor: "900.00" }),
      item({ tipo: "DESPESA", valor: "300.00", status: "CANCELADO" }),
    ])).toEqual({ receitas: "5000.00", despesas: "120.40", saldo: "4879.60" });
  });
});
