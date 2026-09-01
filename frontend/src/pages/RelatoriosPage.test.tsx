// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { ConsultaLancamentosResponse, DashboardResponse } from "../types";
import { Relatorios } from "./Relatorios";

const buscaLancamentos = vi.fn();
vi.mock("../api", () => ({
  buscaLancamentos: (...args: unknown[]) => buscaLancamentos(...args),
  buscaRollupAnual: () => Promise.resolve([]),
}));
vi.mock("../settings/api", () => ({
  getCartoesManuais: () => Promise.resolve([]),
  getDividasRotativas: () => Promise.resolve([]),
  putCartaoManual: () => Promise.reject(new Error("não usado neste teste")),
  deleteCartaoManual: () => Promise.reject(new Error("não usado neste teste")),
  putDividaRotativa: () => Promise.reject(new Error("não usado neste teste")),
}));

const dashboard = {
  competencia: "2026-08",
  viabilidade: { rendaLiquida: "5000.00", custoFixoTotal: "2200.00", pisoVariavelTotal: "800.00" },
  euDoPresente: {
    resumoTriagem: [],
    cartoes: { cartoes: [{ identificador: "nubank", nome: "Nubank", gastoNoMes: "39.80" }], totalGastoEmCartoes: "39.80" },
    cartoesManuais: [{ id: "cartao-1", nome: "PicPay", limite: null, valorFatura: "4257.74", observacao: null }],
  },
} as unknown as DashboardResponse;

const resposta = {
  itens: [
    { id: "receita", descricao: "Salário", tipo: "RECEITA", valor: "5000.00", vencimento: "2026-08-05", status: "LIQUIDADO", contaOrigemId: null, contaDestinoId: "conta-1", categoria: null, cartaoManualId: null, transacaoId: null, marcacaoPlanejamento: "NENHUMA", contaOuCartao: "Conta Itaú", editavel: true, origem: "MANUAL", origemReceita: "SALARIO" },
    { id: "despesa", descricao: "Mercado", tipo: "DESPESA", valor: "120.00", vencimento: "2026-08-06", status: "LIQUIDADO", contaOrigemId: "conta-1", contaDestinoId: null, categoria: null, cartaoManualId: null, transacaoId: null, marcacaoPlanejamento: "NENHUMA", contaOuCartao: "Conta Itaú", editavel: true, origem: "MANUAL", origemReceita: null },
    { id: "cartao", descricao: "Streaming", tipo: "DESPESA", valor: "39.80", vencimento: "2026-08-07", status: "LIQUIDADO", contaOrigemId: null, contaDestinoId: null, categoria: null, cartaoManualId: null, transacaoId: "cartao", marcacaoPlanejamento: "NENHUMA", contaOuCartao: "Nubank", editavel: false, origem: "CSV", origemReceita: null },
  ],
  totalDeItens: 3, totalDespesas: "159.80", totalReceitas: "5000.00", saldoRealizado: "4840.20", saldoPrevisto: "4840.20", quantidadeAtrasados: 0, pagina: 0, tamanho: 100, temMais: false,
} as ConsultaLancamentosResponse;

describe("Relatorios", () => {
  afterEach(() => { cleanup(); vi.clearAllMocks(); });

  it("mostra contas e cartões pelos movimentos da competência, sem fatura atual", async () => {
    buscaLancamentos.mockResolvedValue(resposta);
    const user = userEvent.setup();
    render(<Relatorios dashboard={dashboard} pendencias={[]} contas={[{ id: "conta-1", identificador: "itau", nome: "Conta Itaú", tipo: "CORRENTE", saldo: "99999.00" }]} />);

    await user.click(screen.getByRole("tab", { name: "Contas" }));
    expect(await screen.findByText(/Entradas.*5\.000,00.*saídas.*120,00/)).toBeTruthy();
    expect(screen.queryByText("R$ 99.999,00")).toBeNull();

    await user.click(screen.getByRole("tab", { name: "Cartões" }));
    expect(await screen.findByText("Compras no período")).toBeTruthy();
    expect(screen.getByText(/39,80/)).toBeTruthy();
    expect(screen.queryByText(/4\.257,74/)).toBeNull();

    await user.click(screen.getByRole("tab", { name: "Entradas e saídas" }));
    expect(screen.getByText("Receitas do mês").parentElement?.textContent).toContain("5.000,00");
    expect(screen.getByText("Despesas do mês").parentElement?.textContent).toContain("159,80");
    expect(screen.getByText("Saldo do mês").parentElement?.textContent).toContain("4.840,20");
    expect(screen.queryByText("Renda líquida informada")).toBeNull();
  });
});
