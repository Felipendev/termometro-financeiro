// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { EditorLancamentoPlanilha } from "./EditorLancamentoPlanilha";

const postLancamentoNaPlanilha = vi.fn();
const putLancamentoNaPlanilha = vi.fn();
vi.mock("../api", () => ({
  postLancamentoNaPlanilha: (...args: unknown[]) => postLancamentoNaPlanilha(...args),
  putLancamentoNaPlanilha: (...args: unknown[]) => putLancamentoNaPlanilha(...args),
}));

describe("EditorLancamentoPlanilha", () => {
  afterEach(() => { cleanup(); vi.clearAllMocks(); });

  it("adiciona entrada com origem e sem categoria", async () => {
    postLancamentoNaPlanilha.mockResolvedValue(undefined);
    const aoSalvar = vi.fn();
    const user = userEvent.setup();
    render(<EditorLancamentoPlanilha data="2026-09-05" aoSalvar={aoSalvar} aoCancelar={() => undefined} />);

    await user.selectOptions(screen.getByLabelText("Movimento"), "ENTRADA");
    await user.type(screen.getByLabelText("Descrição"), "Salário");
    await user.type(screen.getByLabelText("Valor"), "5000,00");
    await user.selectOptions(screen.getByLabelText("Origem da entrada"), "SALARIO");
    await user.click(screen.getByRole("button", { name: "Adicionar ao dia" }));

    expect(postLancamentoNaPlanilha).toHaveBeenCalledWith("2026-09-05", expect.objectContaining({
      descricao: "Salário", tipo: "ENTRADA", valor: 5000, origemReceita: "SALARIO",
      categoria: null, grupoCategoria: null, naturezaCategoria: null,
    }));
    expect(aoSalvar).toHaveBeenCalledOnce();
  });
});
