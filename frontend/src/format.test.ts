import { describe, expect, it } from "vitest";
import { formatarEntradaDeDinheiro, valorDaEntradaDeDinheiro } from "./format";

describe("entrada monetária", () => {
  it.each([
    ["1", "R$ 0,01"],
    ["12", "R$ 0,12"],
    ["123", "R$ 1,23"],
    ["123456", "R$ 1.234,56"],
    ["R$ 0,012", "R$ 0,12"],
  ])("desloca os centavos ao digitar %s", (entrada, esperado) => {
    expect(formatarEntradaDeDinheiro(entrada).replace(/\u00a0/g, " ")).toBe(esperado);
  });

  it("converte o valor mascarado para a API", () => {
    expect(valorDaEntradaDeDinheiro("R$ 2.200,00")).toBe(2200);
  });
});
