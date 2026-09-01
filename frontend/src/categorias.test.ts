import { describe, expect, it } from "vitest";
import { CATEGORIAS_LANCAMENTO } from "./categorias";

describe("CATEGORIAS_LANCAMENTO", () => {
  it("usa somente grupos aceitos pelo backend", () => {
    const gruposAceitos = new Set([
      "MORADIA", "ALIMENTACAO", "TRANSPORTE", "SAUDE", "LAZER",
      "ASSINATURAS", "VESTUARIO", "COMPRAS", "SERVICOS", "IMPOSTOS",
      "DIVIDA", "TRANSFERENCIA", "OUTROS",
    ]);

    expect(CATEGORIAS_LANCAMENTO.every((item) => gruposAceitos.has(item.grupo))).toBe(true);
  });
});
