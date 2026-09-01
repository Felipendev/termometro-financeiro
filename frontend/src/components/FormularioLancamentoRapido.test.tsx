// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { FormularioLancamentoRapido } from "./FormularioLancamentoRapido";

const putLancamentoPlanejado = vi.fn();
const postLiquidarLancamentoPlanejado = vi.fn();

vi.mock("../api", () => ({
  putLancamentoPlanejado: (...args: unknown[]) => putLancamentoPlanejado(...args),
  postLiquidarLancamentoPlanejado: (...args: unknown[]) => postLiquidarLancamentoPlanejado(...args),
}));

describe("FormularioLancamentoRapido", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("abre despesa com dinheiro, categoria, forma de pagamento e marcações", () => {
    const html = renderToStaticMarkup(
      <FormularioLancamentoRapido tipo="DESPESA" contas={[]} cartoes={[]} aoFechar={() => undefined} aoConcluir={async () => undefined} />,
    );

    expect(html).toContain("R$ 0,00");
    expect(html).toContain("Escolha uma categoria");
    expect(html).toContain("Pagar no débito / pela conta");
    expect(html).toContain("Custo fixo");
    expect(html).toContain("Piso humano");
    expect(html).toContain("Salvar e criar outra");
  });

  it("salva e cria outra mantendo o modal aberto e limpando os campos principais", async () => {
    const user = userEvent.setup();
    const aoFechar = vi.fn();
    const aoConcluir = vi.fn().mockResolvedValue(undefined);
    putLancamentoPlanejado.mockResolvedValue({ id: "novo-1" });
    postLiquidarLancamentoPlanejado.mockResolvedValue(undefined);
    render(<FormularioLancamentoRapido tipo="DESPESA" contas={[]} cartoes={[]}
      aoFechar={aoFechar} aoConcluir={aoConcluir} />);

    await user.type(screen.getByLabelText("Descrição"), "Mercado");
    fireEvent.change(screen.getByLabelText("Valor"), { target: { value: "1250" } });
    await user.selectOptions(screen.getByLabelText("Categoria"), "Mercado");
    await user.click(screen.getByRole("button", { name: "Salvar e criar outra" }));

    await waitFor(() => expect(putLancamentoPlanejado).toHaveBeenCalledOnce());
    expect(postLiquidarLancamentoPlanejado).toHaveBeenCalledWith("novo-1");
    expect(aoConcluir).toHaveBeenCalledOnce();
    expect(aoFechar).not.toHaveBeenCalled();
    expect((screen.getByLabelText("Descrição") as HTMLInputElement).value).toBe("");
    expect((screen.getByLabelText("Valor") as HTMLInputElement).value).toContain("0,00");
    expect((screen.getByLabelText("Categoria") as HTMLSelectElement).value).toBe("");
  });

  it("salva receita com origem própria e sem categoria de despesa", async () => {
    const user = userEvent.setup();
    const aoConcluir = vi.fn().mockResolvedValue(undefined);
    putLancamentoPlanejado.mockResolvedValue({ id: "receita-1" });
    postLiquidarLancamentoPlanejado.mockResolvedValue(undefined);
    render(<FormularioLancamentoRapido tipo="RECEITA" contas={[]} cartoes={[]}
      aoFechar={() => undefined} aoConcluir={aoConcluir} />);

    expect(screen.queryByLabelText("Categoria")).toBeNull();
    expect(screen.getByLabelText("Origem da receita").textContent).toContain("Salário");
    await user.type(screen.getByLabelText("Descrição"), "Salário");
    fireEvent.change(screen.getByLabelText("Valor"), { target: { value: "500000" } });
    await user.selectOptions(screen.getByLabelText("Origem da receita"), "SALARIO");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(putLancamentoPlanejado).toHaveBeenCalledOnce());
    expect(putLancamentoPlanejado).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({
      tipo: "RECEITA",
      categoria: null,
      grupoCategoria: null,
      naturezaCategoria: null,
      origemReceita: "SALARIO",
    }));
    expect(postLiquidarLancamentoPlanejado).toHaveBeenCalledWith("receita-1");
    expect(aoConcluir).toHaveBeenCalledOnce();
  });
});
