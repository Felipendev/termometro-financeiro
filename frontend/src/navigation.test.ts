import { describe, expect, it } from "vitest";
import { NAVEGACAO_PRINCIPAL } from "./navigation";

describe("navegação principal", () => {
  it("prioriza o fluxo financeiro e remove as antigas telas de configuração", () => {
    expect(NAVEGACAO_PRINCIPAL.map((item) => item.rotulo)).toEqual([
      "Visão geral",
      "Lançamentos",
      "Planilha",
      "Relatórios",
    ]);
    expect(NAVEGACAO_PRINCIPAL.map((item) => item.rotulo)).not.toContain("Planejamento");
    expect(NAVEGACAO_PRINCIPAL.map((item) => item.rotulo)).not.toContain("Contas e cartões");
    expect(NAVEGACAO_PRINCIPAL.map((item) => item.rotulo)).not.toContain("Cartões");
  });
});
