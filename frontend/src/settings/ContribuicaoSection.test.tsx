// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ContribuicaoSection } from "./ContribuicaoSection";

const getMetasContribuicao = vi.fn();

vi.mock("./api", () => ({
  getMetasContribuicao: (...args: unknown[]) => getMetasContribuicao(...args),
  postAutorizarProximoPasso: vi.fn(),
}));

describe("ContribuicaoSection", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("mostra a informação necessária sem formatar valor ausente", async () => {
    getMetasContribuicao.mockResolvedValue([{
      nome: "DIZIMO",
      percentualAtual: "0.000000",
      percentualAlvo: "0.100000",
      informacaoNecessaria: "Para calcular o próximo passo, cadastre a renda de 2026-10.",
    }]);

    render(<ContribuicaoSection competencia="2026-09" />);

    expect(await screen.findByText(/cadastre a renda de 2026-10/i)).toBeTruthy();
    expect(document.body.textContent).not.toContain("NaN");
  });
});
