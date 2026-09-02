import { useRef, useState, type FormEvent } from "react";
import { postLancamentoNaPlanilha, putLancamentoNaPlanilha } from "../api";
import { CATEGORIAS_LANCAMENTO } from "../categorias";
import { formatarEntradaDeDinheiro, valorDaEntradaDeDinheiro } from "../format";
import type { EscopoEdicaoRecorrencia, LancamentoDaPlanilhaRequest, LancamentoDaPlanilhaResponse, MarcacaoPlanejamento, OrigemReceita } from "../types";
import { CampoRecorrencia } from "./CampoRecorrencia";
import { EscolhaEscopoRecorrencia } from "./EscolhaEscopoRecorrencia";

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
  const [valor, setValor] = useState(item ? formatarEntradaDeDinheiro(String(Math.round(Number(item.valor) * 100))) : formatarEntradaDeDinheiro("0"));
  const valorRef = useRef<HTMLInputElement>(null);
  function alteraValor(bruto: string) { const proximo = formatarEntradaDeDinheiro(bruto); setValor(proximo); requestAnimationFrame(() => valorRef.current?.setSelectionRange(proximo.length, proximo.length)); }
  const [categoria, setCategoria] = useState(item?.categoria ?? "");
  const [origemReceita, setOrigemReceita] = useState<OrigemReceita | "">(item?.origemReceita ?? "");
  const [marcacao, setMarcacao] = useState<MarcacaoPlanejamento>(item?.marcacaoPlanejamento ?? "NENHUMA");
  const jaEhSerie = Boolean(item?.diaRecorrencia);
  const [recorrente, setRecorrente] = useState(jaEhSerie);
  const [diaRecorrencia, setDiaRecorrencia] = useState(item?.diaRecorrencia ?? Number(data.slice(-2)));
  const [perguntandoEscopo, setPerguntandoEscopo] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const pendente = useRef<LancamentoDaPlanilhaRequest | null>(null);

  function salvar(evento: FormEvent) {
    evento.preventDefault();
    const numero = valorDaEntradaDeDinheiro(valor);
    const categoriaEscolhida = CATEGORIAS_LANCAMENTO.find((opcao) => opcao.nome === categoria);
    if (numero === null) {
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
      valor: numero,
      categoria: tipo === "SAIDA" ? categoriaEscolhida?.nome : null,
      grupoCategoria: tipo === "SAIDA" ? categoriaEscolhida?.grupo : null,
      naturezaCategoria: tipo === "SAIDA" ? (item?.naturezaCategoria ?? "VARIAVEL") : null,
      origemReceita: tipo === "ENTRADA" ? origemReceita || null : null,
      marcacaoPlanejamento: marcacao,
      diaRecorrencia: recorrente ? diaRecorrencia : null,
    };
    if (jaEhSerie) {
      pendente.current = request;
      setPerguntandoEscopo(true);
      return;
    }
    efetivaSalvar(request, "ESTA");
  }

  async function efetivaSalvar(request: LancamentoDaPlanilhaRequest, escopo: EscopoEdicaoRecorrencia) {
    setPerguntandoEscopo(false);
    setSalvando(true);
    setErro(null);
    try {
      const corpo = { ...request, escopoEdicao: escopo };
      if (item?.id) await putLancamentoNaPlanilha(data, item.id, corpo);
      else await postLancamentoNaPlanilha(data, corpo);
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
        <select id="planilha-tipo" value={tipo} disabled={Boolean(item)} onChange={(evento) => { const novoTipo = evento.target.value as "ENTRADA" | "SAIDA"; setTipo(novoTipo); setMarcacao("NENHUMA"); }}>
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
        <input ref={valorRef} id="planilha-valor" inputMode="numeric" value={valor} onFocus={(evento) => evento.currentTarget.setSelectionRange(evento.currentTarget.value.length, evento.currentTarget.value.length)} onClick={(evento) => evento.currentTarget.setSelectionRange(evento.currentTarget.value.length, evento.currentTarget.value.length)} onChange={(evento) => alteraValor(evento.target.value)} placeholder="R$ 0,00" required />
      </div>
      {tipo === "SAIDA" ? (
        <><div className="form__campo">
          <label htmlFor="planilha-categoria">Categoria da saída</label>
          <select id="planilha-categoria" value={categoria} onChange={(evento) => setCategoria(evento.target.value)} required>
            <option value="">Escolha uma categoria</option>
            {CATEGORIAS_LANCAMENTO.map((opcao) => <option key={opcao.nome} value={opcao.nome}>{opcao.nome}</option>)}
          </select>
        </div><div className="form__campo"><label htmlFor="planilha-recorrencia">Planejamento</label><select id="planilha-recorrencia" value={marcacao} onChange={(evento) => setMarcacao(evento.target.value as MarcacaoPlanejamento)}><option value="NENHUMA">Despesa comum</option><option value="CUSTO_FIXO">Recorrente / custo fixo</option><option value="PISO_HUMANO">Piso humano</option></select></div></>
      ) : (
        <><div className="form__campo">
          <label htmlFor="planilha-origem">Origem da entrada</label>
          <select id="planilha-origem" value={origemReceita} onChange={(evento) => setOrigemReceita(evento.target.value as OrigemReceita | "")} required>
            <option value="">Escolha a origem</option>
            {ORIGENS.map((opcao) => <option key={opcao.valor} value={opcao.valor}>{opcao.rotulo}</option>)}
          </select>
        </div><div className="form__campo"><label htmlFor="planilha-recorrencia-receita">Recorrência</label><select id="planilha-recorrencia-receita" value={marcacao} onChange={(evento) => setMarcacao(evento.target.value as MarcacaoPlanejamento)}><option value="NENHUMA">Entrada pontual</option><option value="RECEITA_RECORRENTE">Receita recorrente</option></select></div></>
      )}
      <CampoRecorrencia
        recorrente={recorrente}
        dia={diaRecorrencia}
        jaEhSerie={jaEhSerie}
        onChangeRecorrente={setRecorrente}
        onChangeDia={setDiaRecorrencia}
      />
      {erro && <p className="form__erro" role="alert">{erro}</p>}
      {perguntandoEscopo && pendente.current ? (
        <EscolhaEscopoRecorrencia
          onEscolher={(escopo) => efetivaSalvar(pendente.current!, escopo)}
          onCancelar={() => setPerguntandoEscopo(false)}
        />
      ) : (
        <div className="form__acoes">
          <button type="submit" disabled={salvando}>{salvando ? "Salvando..." : item ? "Salvar alteração" : "Adicionar ao dia"}</button>
          <button type="button" className="botao--secundario" onClick={aoCancelar}>Cancelar</button>
        </div>
      )}
    </form>
  );
}
