// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { DashboardResponse } from "../types";
import { Relatorios } from "./Relatorios";

vi.mock("../api", () => ({
  buscaTodosLancamentos: () => Promise.resolve([]),
  buscaRollupAnual: () => Promise.resolve([
    { competencia: "2026-09", entrada: "5000.00", saida: "3200.00", taxaEconomia: "0.360000" },
  ]),
  buscaFaturasCartao: () => Promise.resolve([]),
}));
vi.mock("../components/Banner", () => ({ Banner: () => <div>Viabilidade consolidada</div> }));
vi.mock("../components/PainelOperacional", () => ({ PainelOperacional: () => <div>Contas a receber e Minhas contas</div> }));
vi.mock("../components/ColunaPassado", () => ({ ColunaPassado: () => <div>Dívidas ativas</div> }));
vi.mock("../components/ColunaPresente", () => ({ ColunaPresente: () => <div>Saldo de sobrevivência, Triagem e Vampiros</div> }));
vi.mock("../components/ColunaFuturo", () => ({ ColunaFuturo: () => <div>Plano de ajuste</div> }));
vi.mock("../settings/CartoesSection", () => ({ CartoesSection: () => null }));
vi.mock("../settings/DividasRotativasSection", () => ({ DividasRotativasSection: () => null }));
vi.mock("../components/ImportarInteligente", () => ({ ImportarInteligente: () => null }));

const dashboard = {
  competencia: "2026-09",
  viabilidade: { custoFixoTotal: "2200.00", pisoVariavelTotal: "800.00" },
  euDoPassado: {},
  euDoPresente: { resumoTriagem: [], cartoes: { cartoes: [], totalGastoEmCartoes: "0.00" }, cartoesManuais: [] },
  euDoFuturo: {},
} as unknown as DashboardResponse;

describe("Relatórios consolidados", () => {
  afterEach(() => { cleanup(); vi.clearAllMocks(); });

  it("reúne planejamento legado e rollup anual dentro de Relatórios", async () => {
    const user = userEvent.setup();
    render(<Relatorios dashboard={dashboard} pendencias={[]} contas={[]} />);

    await user.click(screen.getByRole("tab", { name: "Planejamento" }));
    expect(screen.getByText("Contas a receber e Minhas contas")).toBeTruthy();
    expect(screen.getByText("Dívidas ativas")).toBeTruthy();
    expect(screen.getByText("Saldo de sobrevivência, Triagem e Vampiros")).toBeTruthy();
    expect(screen.getByText("Plano de ajuste")).toBeTruthy();

    await user.click(screen.getByRole("tab", { name: "Anual" }));
    expect(await screen.findByText("2026-09")).toBeTruthy();
    expect(screen.getByText("36,0%")).toBeTruthy();
  });
});
