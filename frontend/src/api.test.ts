import { afterEach, describe, expect, it, vi } from "vitest";
import { apiFetch, postClassificarTransacao, verificaCompatibilidade } from "./api";

describe("postClassificarTransacao", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("envia a revisão como JSON", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}", {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    vi.stubGlobal("fetch", fetchMock);

    await postClassificarTransacao("transacao-1", {
      categoria: "Casa", grupo: "MORADIA", natureza: "VARIAVEL", aplicarAoGrupo: true,
    });

    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining("/v1/transacoes/transacao-1/classificar"),
      expect.objectContaining({
        method: "POST",
        headers: { "Content-Type": "application/json" },
      }));
  });
});

describe("apiFetch", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("preserva a mensagem específica do formato de erro do backend", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({
      message: "Nenhuma renda declarada para 2026-10.",
      description: null,
    }), {
      status: 404,
      headers: { "Content-Type": "application/json" },
    })));

    await expect(apiFetch("/v1/exemplo")).rejects.toMatchObject({
      name: "ApiError",
      status: 404,
      message: "Nenhuma renda declarada para 2026-10.",
    });
  });
});

describe("verificaCompatibilidade", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("bloqueia a interface com explicação quando o servidor antigo não possui o contrato", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 404 })));

    await expect(verificaCompatibilidade()).rejects.toMatchObject({
      status: 409,
      message: expect.stringContaining("servidor ainda é de uma versão antiga"),
    });
  });

  it("aceita apenas a mesma versão de contrato", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({
      contratoApi: "2026-09-01-planilha-editavel-v1",
    }), { status: 200, headers: { "Content-Type": "application/json" } })));

    await expect(verificaCompatibilidade()).resolves.toBeUndefined();
  });
});
