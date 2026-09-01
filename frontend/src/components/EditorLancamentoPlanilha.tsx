import { useState, type FormEvent } from "react";
import { postLancamentoNaPlanilha, putLancamentoNaPlanilha } from "../api";
import { CATEGORIAS_LANCAMENTO } from "../categorias";
import { normalizarDecimal } from "../format";
import type { LancamentoDaPlanilhaRequest, LancamentoDaPlanilhaResponse, OrigemReceita } from "../types";

const ORIGENS: { valor: OrigemReceita; rotulo: string }[] = [
  { valor: "SALARIO", rotulo: "Salário" },
  { valor: "INVESTIMENTO", rotulo: "Investimento" },
  { valor: "EMPRESTIMO", rotulo: "Empréstimo" },
];

export function EditorLancamentoPlanilha({
  data,
  item,
  aoSalvar,
  aoCancelar,
}: {
  data: string;
  item?: LancamentoDaPlanilhaResponse;
  aoSalvar: () => void;
  aoCancelar: () => void;
}) {
  const [tipo, setTipo] = useState<"ENTRADA" | "SAIDA">(item?.tipo ?? "SAIDA");
  const [descricao, setDescricao] = useState(item?.descricao ?? "");
  const [valor, setValor] = useState(item?.valor.replace(".", ",") ?? "");
  const [categoria, setCategoria] = useState(item?.categoria ?? "");
  const [origemReceita, setOrigemReceita] = useState<OrigemReceita | "">(item?.origemReceita ?? "");
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function salvar(evento: FormEvent) {
    evento.preventDefault();
    const decimal = normalizarDecimal(valor);
    const categoriaEscolhida = CATEGORIAS_LANCAMENTO.find((opcao) => opcao.nome === categoria);
    if (!decimal || Number(decimal) <= 0) {
      setErro("Informe um valor maior que zero.");
      return;
    }
    if (tipo === "SAIDA" && !categoriaEscolhida) {
      setErro("Escolha a categoria da saída.");
      return;
    }
    if (tipo === "ENTRADA" && !origemReceita) {
      setErro("Escolha se a entrada é salário, investimento ou empréstimo.");
      return;
    }
    const request: LancamentoDaPlanilhaRequest = {
      descricao: descricao.trim(),
      tipo,
      valor: Number(decimal),
      categoria: tipo === "SAIDA" ? categoriaEscolhida?.nome : null,
      grupoCategoria: tipo === "SAIDA" ? categoriaEscolhida?.grupo : null,
      naturezaCategoria: tipo === "SAIDA" ? (item?.naturezaCategoria ?? "VARIAVEL") : null,
      origemReceita: tipo === "ENTRADA" ? origemReceita || null : null,
    };
    setSalvando(true);
    setErro(null);
    try {
      if (item?.id) await putLancamentoNaPlanilha(data, item.id, request);
      else await postLancamentoNaPlanilha(data, request);
      aoSalvar();
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : "Não foi possível salvar o lançamento.");
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="form planilha__editor" onSubmit={salvar}>
      <div className="form__campo">
        <label htmlFor="planilha-tipo">Movimento</label>
        <select id="planilha-tipo" value={tipo} disabled={Boolean(item)} onChange={(evento) => setTipo(evento.target.value as "ENTRADA" | "SAIDA")}>
          <option value="SAIDA">Saída</option>
          <option value="ENTRADA">Entrada</option>
        </select>
      </div>
      <div className="form__campo">
        <label htmlFor="planilha-descricao">Descrição</label>
        <input id="planilha-descricao" value={descricao} onChange={(evento) => setDescricao(evento.target.value)} required autoFocus />
      </div>
      <div className="form__campo">
        <label htmlFor="planilha-valor">Valor</label>
        <input id="planilha-valor" inputMode="decimal" placeholder="0,00" value={valor} onChange={(evento) => setValor(evento.target.value)} required />
      </div>
      {tipo === "SAIDA" ? (
        <div className="form__campo">
          <label htmlFor="planilha-categoria">Categoria da saída</label>
          <select id="planilha-categoria" value={categoria} onChange={(evento) => setCategoria(evento.target.value)} required>
            <option value="">Escolha uma categoria</option>
            {CATEGORIAS_LANCAMENTO.map((opcao) => <option key={opcao.nome} value={opcao.nome}>{opcao.nome}</option>)}
          </select>
        </div>
      ) : (
        <div className="form__campo">
          <label htmlFor="planilha-origem">Origem da entrada</label>
          <select id="planilha-origem" value={origemReceita} onChange={(evento) => setOrigemReceita(evento.target.value as OrigemReceita | "")} required>
            <option value="">Escolha a origem</option>
            {ORIGENS.map((opcao) => <option key={opcao.valor} value={opcao.valor}>{opcao.rotulo}</option>)}
          </select>
        </div>
      )}
      {erro && <p className="form__erro" role="alert">{erro}</p>}
      <div className="form__acoes">
        <button type="submit" disabled={salvando}>{salvando ? "Salvando..." : item ? "Salvar alteração" : "Adicionar ao dia"}</button>
        <button type="button" className="botao--secundario" onClick={aoCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
