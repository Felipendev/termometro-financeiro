// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { DashboardInicioResponse } from "./types";
import App from "./App";

const buscaDashboardInicio = vi.fn();

vi.mock("./api", () => ({
  ApiError: class ApiError extends Error {},
  verificaCompatibilidade: () => Promise.resolve(),
  buscaDashboardInicio: (...args: unknown[]) => buscaDashboardInicio(...args),
  postNaoGasto: vi.fn(),
  postTriagem: vi.fn(),
}));

vi.mock("./pages/Planilha", () => ({ Planilha: () => <div>Planilha simulada</div> }));
vi.mock("./pages/Relatorios", () => ({ Relatorios: () => <div>Relatórios simulados</div> }));
vi.mock("./pages/Configuracoes", () => ({ Planejamento: () => <div>Configurações simuladas</div> }));
vi.mock("./components/PainelVisaoGeral", () => ({ PainelVisaoGeral: () => <div>Painel simulado</div> }));
vi.mock("./components/Skeleton", () => ({ Skeleton: () => <div>Carregando</div> }));
vi.mock("./pages/Lancamentos", () => ({
  Lancamentos: ({ aoNovo }: { aoNovo: (tipo: "DESPESA" | "RECEITA") => void }) => <div>
    <button type="button" onClick={() => aoNovo("DESPESA")}>Despesa simulada</button>
    <button type="button" onClick={() => aoNovo("RECEITA")}>Receita simulada</button>
  </div>,
}));
vi.mock("./components/FormularioLancamentoRapido", () => ({
  FormularioLancamentoRapido: ({ tipo }: { tipo: string }) => <div role="dialog">Tipo selecionado: {tipo}</div>,
}));

const dashboard = {
  analise: { euDoPresente: { cartoesManuais: [] } },
  contasManuais: [],
  pendencias: [],
} as unknown as DashboardInicioResponse;

describe("App", () => {
  beforeEach(() => buscaDashboardInicio.mockResolvedValue(dashboard));
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("mantém o tipo escolhido ao abrir um novo lançamento", async () => {
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText("Painel simulado");

    await user.click(screen.getByRole("button", { name: "Lançamentos" }));
    await user.click(screen.getByRole("button", { name: "Receita simulada" }));

    expect(screen.getByRole("dialog").textContent).toContain("Tipo selecionado: RECEITA");
  });
});
