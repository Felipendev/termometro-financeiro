// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ConsultaLancamentosResponse } from "../types";
import { Lancamentos } from "./Lancamentos";

const buscaLancamentos = vi.fn();
const postComandoLancamento = vi.fn();
const deleteLancamentoPlanejado = vi.fn();
const postClassificarTransacao = vi.fn();

vi.mock("../api", () => ({
  buscaLancamentos: (...args: unknown[]) => buscaLancamentos(...args),
  postComandoLancamento: (...args: unknown[]) => postComandoLancamento(...args),
  deleteLancamentoPlanejado: (...args: unknown[]) => deleteLancamentoPlanejado(...args),
  postClassificarTransacao: (...args: unknown[]) => postClassificarTransacao(...args),
}));

const resposta: ConsultaLancamentosResponse = {
  itens: [
    {
      id: "manual-1", descricao: "Mercado", tipo: "DESPESA", valor: "120.00",
      vencimento: "2026-08-25", status: "PENDENTE", contaOrigemId: "conta-1",
      contaDestinoId: null, categoria: { nome: "Mercado", grupo: "DIA_A_DIA", natureza: "VARIAVEL" },
      cartaoManualId: null, transacaoId: null, marcacaoPlanejamento: "NENHUMA",
      contaOuCartao: "Conta Itaú", editavel: true, origem: "MANUAL", origemReceita: null, serieId: null, diaRecorrencia: null,
    },
    {
      id: "importado-1", descricao: "Streaming", tipo: "DESPESA", valor: "39.80",
      vencimento: "2026-08-24", status: "LIQUIDADO", contaOrigemId: null,
      contaDestinoId: null, categoria: { nome: "Assinaturas e serviços", grupo: "DIA_A_DIA", natureza: "VARIAVEL" },
      cartaoManualId: null, transacaoId: "importado-1", marcacaoPlanejamento: "NENHUMA",
      contaOuCartao: "Nubank", editavel: false, origem: "CSV", origemReceita: null, serieId: null, diaRecorrencia: null,
    },
    {
      id: "manual-2", descricao: "Aluguel", tipo: "DESPESA", valor: "2200.00",
      vencimento: "2026-08-20", status: "LIQUIDADO", contaOrigemId: "conta-1",
      contaDestinoId: null, categoria: { nome: "Casa", grupo: "MORADIA", natureza: "FIXO" },
      cartaoManualId: null, transacaoId: "movimento-2", marcacaoPlanejamento: "CUSTO_FIXO",
      contaOuCartao: "Conta Itaú", editavel: true, origem: "MANUAL", origemReceita: null, serieId: null, diaRecorrencia: null,
    },
  ],
  totalDeItens: 32,
  totalDespesas: "159.80",
  totalReceitas: "0.00",
  saldoRealizado: "-39.80",
  saldoPrevisto: "-159.80",
  quantidadeAtrasados: 1,
  pagina: 0,
  tamanho: 30,
  temMais: true,
};

function propriedades(aoAlterar = vi.fn().mockResolvedValue(undefined)) {
  return {
    competencia: "2026-08",
    contas: [{ id: "conta-1", identificador: "itau", nome: "Conta Itaú", tipo: "CORRENTE", saldo: "0.00" }],
    cartoes: [],
    aoNovo: vi.fn(),
    aoEditar: vi.fn(),
    aoAlterar,
  };
}

describe("Lancamentos", () => {
  beforeEach(() => {
    buscaLancamentos.mockResolvedValue(resposta);
    postComandoLancamento.mockResolvedValue(undefined);
    postClassificarTransacao.mockResolvedValue(undefined);
    deleteLancamentoPlanejado.mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("mantém filtros ao trocar a competência e informa paginação", async () => {
    const user = userEvent.setup();
    const props = propriedades();
    const { rerender } = render(<Lancamentos {...props} />);
    await screen.findByText("Mercado");

    await user.selectOptions(screen.getByLabelText("Tipo"), "DESPESA");
    rerender(<Lancamentos {...props} competencia="2026-09" />);

    expect((screen.getByLabelText("Tipo") as HTMLSelectElement).value).toBe("DESPESA");
    expect(await screen.findByText("Exibindo 30 de 32 lançamentos · página 1 de 2")).toBeTruthy();
  });

  it("abre o formulário já no tipo escolhido", async () => {
    const user = userEvent.setup();
    const props = propriedades();
    render(<Lancamentos {...props} />);
    await screen.findByText("Mercado");

    await user.click(screen.getByRole("button", { name: "Despesa" }));
    await user.click(screen.getByRole("button", { name: "Receita" }));

    expect(props.aoNovo).toHaveBeenNthCalledWith(1, "DESPESA");
    expect(props.aoNovo).toHaveBeenNthCalledWith(2, "RECEITA");
  });

  it("explica a origem do importado e oferece revisão de categoria", async () => {
    const user = userEvent.setup();
    const aoAlterar = vi.fn().mockResolvedValue(undefined);
    render(<Lancamentos {...propriedades(aoAlterar)} />);

    expect(await screen.findByText("Importado via CSV")).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Revisar categoria de Streaming" }));
    await user.selectOptions(screen.getByLabelText("Nova categoria de Streaming"), "Casa");
    await user.click(screen.getByRole("button", { name: "Salvar categoria de Streaming" }));

    await waitFor(() => expect(postClassificarTransacao).toHaveBeenCalledWith("importado-1", {
      categoria: "Casa", grupo: "MORADIA", natureza: "VARIAVEL", aplicarAoGrupo: true,
    }));
    expect(aoAlterar).toHaveBeenCalledOnce();
  });

  it("permite cancelar pendente e notifica as demais visões", async () => {
    const user = userEvent.setup();
    const aoAlterar = vi.fn().mockResolvedValue(undefined);
    render(<Lancamentos {...propriedades(aoAlterar)} />);
    await screen.findByText("Mercado");

    await user.click(screen.getByRole("button", { name: "Cancelar Mercado" }));

    await waitFor(() => expect(postComandoLancamento).toHaveBeenCalledWith("manual-1", "cancelar"));
    expect(aoAlterar).toHaveBeenCalledOnce();
  });

  it("liquida, reabre e exclui definitivamente lançamentos manuais", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<Lancamentos {...propriedades()} />);
    await screen.findByText("Mercado");

    await user.click(screen.getByRole("button", { name: "Liquidar Mercado" }));
    await user.click(screen.getByRole("button", { name: "Reabrir Aluguel" }));
    await user.click(screen.getByRole("button", { name: "Excluir Mercado" }));

    await waitFor(() => {
      expect(postComandoLancamento).toHaveBeenCalledWith("manual-1", "liquidar");
      expect(postComandoLancamento).toHaveBeenCalledWith("manual-2", "reabrir");
      expect(deleteLancamentoPlanejado).toHaveBeenCalledWith("manual-1");
    });
  });

  it("mostra sinais, cores e destaque para pendência vencida", async () => {
    render(<Lancamentos {...propriedades()} />);

    const despesa = await screen.findByText(/-R\$\s*120,00/);
    expect(despesa.classList.contains("valor--despesa")).toBe(true);
    const titulo = screen.getAllByText("Mercado").find((elemento) => elemento.tagName === "STRONG");
    expect(titulo?.closest("li")?.classList.contains("lancamento--atrasado")).toBe(true);
  });

  it("orienta criação ou importação quando não há lançamentos", async () => {
    buscaLancamentos.mockResolvedValueOnce({ ...resposta, itens: [], totalDeItens: 0, temMais: false });
    render(<Lancamentos {...propriedades()} />);

    expect(await screen.findByText(/Nenhum lançamento neste período.*Crie um lançamento ou importe/)).toBeTruthy();
  });
});
