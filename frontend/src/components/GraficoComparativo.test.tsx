// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GraficoComparativo } from "./GraficoComparativo";

const buscaComparativoCategorias = vi.fn();
vi.mock("../api", () => ({
  buscaComparativoCategorias: (...args: unknown[]) => buscaComparativoCategorias(...args),
}));

describe("GraficoComparativo", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("exibe escala e valores de atual, bom, ideal e ruim nas bolinhas", async () => {
    buscaComparativoCategorias.mockResolvedValue([
      { grupo: "MORADIA", atual: "0.268490", bom: "0.250000", ideal: "0.220000", ruim: "0.312500", valorAtual: "1342.45", rendaReferencia: "5000.00", fonte: "LANCAMENTOS_DO_MES", itens: [] },
    ]);

    render(<GraficoComparativo competencia="2026-09" rendaLiquida="5000.00" />);

    expect(await screen.findByLabelText(/Atual de Moradia: 26,85% · R\$/)).toBeTruthy();
    expect(screen.getByLabelText(/Bom de Moradia: 25% · R\$/)).toBeTruthy();
    expect(screen.getByLabelText(/Ideal de Moradia: 22% · R\$/)).toBeTruthy();
    expect(screen.getByLabelText(/Ruim de Moradia: 31,25% · R\$/)).toHaveProperty("dataset.tooltip");
    expect(screen.getByLabelText(/Escala do gráfico: zero a R\$/)).toBeTruthy();
  });
});
