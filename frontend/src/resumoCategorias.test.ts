import { describe, expect, it } from "vitest";
import { combinaCategorias } from "./resumoCategorias";
import type { LancamentoPlanejadoResponse, ResumoDeCategoriaResponse } from "./types";

describe("categorias da visão geral", () => {
  it("soma pendências da competência sem esconder despesas ainda não liquidadas", () => {
    const resumos = [
      { categoria: "CASA", totalAzul: "100.00", totalAmarelo: "0.00", totalVermelho: "0.00", totalVerde: "0.00", totalNaoTriada: "0.00" },
      { categoria: "TRANSFERENCIA_PESSOAL", totalAzul: "50.00", totalAmarelo: "0.00", totalVermelho: "0.00", totalVerde: "0.00", totalNaoTriada: "0.00" },
    ] as ResumoDeCategoriaResponse[];
    const pendencias = [
      { id: "1", tipo: "DESPESA", valor: "2200.00", vencimento: "2026-08-25", categoria: { nome: "Casa" } },
      { id: "2", tipo: "DESPESA", valor: "800.00", vencimento: "2026-09-01", categoria: { nome: "Casa" } },
    ] as LancamentoPlanejadoResponse[];

    expect(combinaCategorias(resumos, pendencias, "2026-08")).toEqual([
      { nome: "Casa", total: "2300.00" },
    ]);
  });
});
