import { Fragment, useCallback, useDeferredValue, useEffect, useMemo, useState } from "react";
import { AlertCircle, Ban, Check, CheckCircle2, HeartHandshake, Pencil, Plus, Repeat2, RotateCcw, Search, Tags, Trash2, X } from "lucide-react";
import { buscaLancamentos, deleteLancamentoPlanejado, postClassificarTransacao, postComandoLancamento } from "../api";
import { CATEGORIAS_LANCAMENTO } from "../categorias";
import { IconeCategoria } from "../components/IconeCategoria";
import { formatarDinheiro } from "../format";
import type { CartaoManualResponse, ConsultaLancamentosResponse, ContaManualResponse, LancamentoPlanejadoResponse } from "../types";

const ROTULOS_ORIGEM_RECEITA = {
  SALARIO: "Salário",
  INVESTIMENTO: "Investimento",
  EMPRESTIMO: "Empréstimo",
} as const;

export function Lancamentos({ competencia, contas, cartoes, aoNovo, aoEditar, aoAlterar = async () => undefined, versao = 0 }: {
  competencia: string;
  contas: ContaManualResponse[];
  cartoes: CartaoManualResponse[];
  aoNovo: (tipo: "DESPESA" | "RECEITA") => void;
  aoEditar: (item: LancamentoPlanejadoResponse) => void;
  aoAlterar?: () => Promise<void>;
  versao?: number;
}) {
  const [dados, setDados] = useState<ConsultaLancamentosResponse | null>(null);
  const [tipo, setTipo] = useState("");
  const [status, setStatus] = useState("");
  const [contaId, setContaId] = useState("");
  const [cartaoId, setCartaoId] = useState("");
  const [categoria, setCategoria] = useState("");
  const [texto, setTexto] = useState("");
  const [pagina, setPagina] = useState(0);
  const [erro, setErro] = useState<string | null>(null);
  const [revisando, setRevisando] = useState<string | null>(null);
  const [categoriaRevisao, setCategoriaRevisao] = useState("");
  const textoAdiado = useDeferredValue(texto);
  const contasPorId = useMemo(() => new Map(contas.map((conta) => [conta.id, conta])), [contas]);
  const cartoesPorId = useMemo(() => new Map(cartoes.map((cartao) => [cartao.id, cartao])), [cartoes]);
  const hojeIso = new Date().toLocaleDateString("en-CA");

  const carregar = useCallback((paginaAlvo = 0, acumular = false) => {
    return buscaLancamentos(competencia, {
      tipo, status, contaId, cartaoId, categoria, q: textoAdiado, pagina: paginaAlvo, tamanho: 30,
    })
      .then((novos) => {
        setErro(null);
        setDados((atuais) => acumular && atuais
          ? { ...novos, itens: [...atuais.itens, ...novos.itens] }
          : novos);
        setPagina(paginaAlvo);
      })
      .catch((causa: Error) => setErro(causa.message));
  }, [cartaoId, categoria, competencia, contaId, status, textoAdiado, tipo]);

  useEffect(() => {
    void versao;
    void carregar(0, false);
  }, [carregar, versao]);

  async function comando(id: string, acao: "liquidar" | "reabrir" | "cancelar") {
    await postComandoLancamento(id, acao);
    await aoAlterar();
    await carregar(0, false);
  }

  async function excluir(id: string, descricao: string) {
    if (!window.confirm(`Excluir definitivamente “${descricao}”?`)) return;
    await deleteLancamentoPlanejado(id);
    await aoAlterar();
    await carregar(0, false);
  }

  async function classificar(item: LancamentoPlanejadoResponse) {
    const escolhida = CATEGORIAS_LANCAMENTO.find((categoria) => categoria.nome === categoriaRevisao);
    if (!escolhida) return;
    await postClassificarTransacao(item.id, {
      categoria: escolhida.nome,
      grupo: escolhida.grupo,
      natureza: "VARIAVEL",
      aplicarAoGrupo: true,
    });
    setRevisando(null);
    await aoAlterar();
    await carregar(0, false);
  }

  function origemDo(item: LancamentoPlanejadoResponse) {
    if (item.contaOuCartao) return item.contaOuCartao;
    if (item.cartaoManualId) return cartoesPorId.get(item.cartaoManualId)?.nome ?? "Cartão";
    const conta = item.contaOrigemId ? contasPorId.get(item.contaOrigemId) : undefined;
    return conta?.nome ?? "Sem conta vinculada";
  }

  function dataLegivel(data: string) {
    const [ano, mes, dia] = data.split("-");
    return `${dia}/${mes}/${ano}`;
  }

  function statusLegivel(item: LancamentoPlanejadoResponse) {
    if (item.status === "PENDENTE" && item.vencimento < hojeIso) return "Atrasado";
    if (item.status === "LIQUIDADO") return item.tipo === "RECEITA" ? "Recebido" : "Pago";
    if (item.status === "CANCELADO") return "Cancelado";
    return "Pendente";
  }

  return <section className="lancamentos">
    <div className="lancamentos__cabecalho">
      <div><p className="eyebrow">Histórico mensal</p><h2>Lançamentos</h2></div>
      <div className="lancamentos__novos">
        <button type="button" className="lancamentos__novo lancamentos__novo--despesa" onClick={() => aoNovo("DESPESA")}><Plus size={17} /> Despesa</button>
        <button type="button" className="lancamentos__novo lancamentos__novo--receita" onClick={() => aoNovo("RECEITA")}><Plus size={17} /> Receita</button>
      </div>
    </div>
    <div className="lancamentos__filtros">
      <select aria-label="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value)}><option value="">Todos os tipos</option><option value="DESPESA">Despesas</option><option value="RECEITA">Receitas</option><option value="TRANSFERENCIA">Transferências</option></select>
      <select aria-label="Status" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">Todos os status</option><option value="PENDENTE">Pendentes</option><option value="ATRASADO">Atrasados</option><option value="LIQUIDADO">Pagos ou recebidos</option><option value="CANCELADO">Cancelados</option></select>
      <select aria-label="Conta" value={contaId} onChange={(e) => setContaId(e.target.value)}><option value="">Todas as contas</option>{contas.map((conta) => <option key={conta.id} value={conta.id}>{conta.nome}</option>)}</select>
      <select aria-label="Cartão" value={cartaoId} onChange={(e) => setCartaoId(e.target.value)}><option value="">Todos os cartões</option>{cartoes.map((cartao) => <option key={cartao.id} value={cartao.id}>{cartao.nome}</option>)}</select>
      <select aria-label="Categoria" value={categoria} onChange={(e) => setCategoria(e.target.value)}><option value="">Todas as categorias</option>{CATEGORIAS_LANCAMENTO.map((item) => <option key={item.nome} value={item.nome}>{item.nome}</option>)}</select>
      <label className="busca-lancamento"><Search size={16} /><input aria-label="Buscar descrição" placeholder="Buscar descrição" value={texto} onChange={(e) => setTexto(e.target.value)} /></label>
    </div>
    {erro && <p className="form__erro">{erro}</p>}
    {dados && <>
      {dados.quantidadeAtrasados > 0 && <div className="lancamentos__alerta" role="status"><AlertCircle size={18} /><span><b>{dados.quantidadeAtrasados} {dados.quantidadeAtrasados === 1 ? "pendência vencida" : "pendências vencidas"}</b> neste período.</span></div>}
      <div className="lancamentos__totais"><span>Receitas <b>{formatarDinheiro(dados.totalReceitas)}</b></span><span>Despesas <b className="valor--despesa">-{formatarDinheiro(dados.totalDespesas)}</b></span></div>
      {dados.itens.length === 0 ? <div className="cartao vazio">Nenhum lançamento neste período. Crie um lançamento ou importe um extrato.</div> :
        <ul className="lancamentos__lista">{dados.itens.map((item, indice) => <Fragment key={item.id}>
          {(indice === 0 || dados.itens[indice - 1].vencimento !== item.vencimento) && <li className="lancamentos__data">{dataLegivel(item.vencimento)}</li>}
          <li className={item.status === "PENDENTE" && item.vencimento < hojeIso ? "lancamento--atrasado" : undefined}>
          <div className="lancamento__identidade"><IconeCategoria nome={item.categoria?.nome} /><div><span className="lancamento__titulo"><strong>{item.descricao}</strong>{item.editavel && item.status === "PENDENTE" && <><button className="botao--edicao-inline" aria-label={`Editar ${item.descricao}`} title="Editar" onClick={() => aoEditar(item)}><Pencil size={14} /></button><button className="botao--edicao-inline botao--edicao-inline-perigo" aria-label={`Excluir ${item.descricao}`} title="Excluir definitivamente" onClick={() => excluir(item.id, item.descricao)}><Trash2 size={14} /></button></>}</span>{item.marcacaoPlanejamento && item.marcacaoPlanejamento !== "NENHUMA" && <span className="marcacao-badge">{item.marcacaoPlanejamento === "CUSTO_FIXO" ? <Repeat2 size={12} /> : <HeartHandshake size={12} />}{item.marcacaoPlanejamento === "CUSTO_FIXO" ? "Custo fixo" : "Piso humano"}</span>}<small>{item.tipo === "RECEITA" ? (item.origemReceita ? ROTULOS_ORIGEM_RECEITA[item.origemReceita] : "Origem não informada") : item.categoria?.nome ?? "Sem categoria"} · {item.vencimento} · {item.status}</small></div></div>
          <b className={item.tipo === "DESPESA" ? "valor--despesa" : item.tipo === "RECEITA" ? "valor--receita" : ""}>{item.tipo === "DESPESA" ? "-" : item.tipo === "RECEITA" ? "+" : ""}{formatarDinheiro(item.valor)}</b>
          <span className="lancamento__origem">{origemDo(item)}<small>{statusLegivel(item)}</small>{item.origem !== "MANUAL" && <small>Importado via {item.origem}</small>}</span>
          <div className="lancamentos__acoes">
            {item.editavel && item.status === "PENDENTE" && <button className="botao--icone" aria-label={`Liquidar ${item.descricao}`} title="Liquidar" onClick={() => comando(item.id, "liquidar")}><CheckCircle2 size={17} /></button>}
            {item.editavel && item.status === "PENDENTE" && <button className="botao--icone" aria-label={`Cancelar ${item.descricao}`} title="Cancelar lançamento" onClick={() => comando(item.id, "cancelar")}><Ban size={17} /></button>}
            {item.editavel && item.status === "LIQUIDADO" && <button className="botao--icone" aria-label={`Reabrir ${item.descricao}`} title="Reabrir" onClick={() => comando(item.id, "reabrir")}><RotateCcw size={17} /></button>}
            {!item.editavel && revisando !== item.id && <button className="botao--icone" aria-label={`Revisar categoria de ${item.descricao}`} title="Revisar categoria" onClick={() => { setRevisando(item.id); setCategoriaRevisao(item.categoria?.nome ?? ""); }}><Tags size={17} /></button>}
            {!item.editavel && revisando === item.id && <span className="lancamento__revisao"><select aria-label={`Nova categoria de ${item.descricao}`} value={categoriaRevisao} onChange={(evento) => setCategoriaRevisao(evento.target.value)}><option value="">Escolha</option>{CATEGORIAS_LANCAMENTO.map((categoria) => <option value={categoria.nome} key={categoria.nome}>{categoria.nome}</option>)}</select><button className="botao--icone" aria-label={`Salvar categoria de ${item.descricao}`} onClick={() => classificar(item)}><Check size={16} /></button><button className="botao--icone" aria-label={`Fechar revisão de ${item.descricao}`} onClick={() => setRevisando(null)}><X size={16} /></button></span>}
          </div>
        </li></Fragment>)}</ul>}
      <p className="lancamentos__paginacao">Exibindo {Math.min((pagina + 1) * dados.tamanho, dados.totalDeItens)} de {dados.totalDeItens} lançamentos · página {pagina + 1} de {Math.max(1, Math.ceil(dados.totalDeItens / dados.tamanho))}</p>
      {dados.temMais && <button type="button" className="lancamentos__mais" onClick={() => carregar(pagina + 1, true)}>Carregar mais lançamentos</button>}
      <footer className="lancamentos__resumo"><span>Saldo realizado <b className={Number(dados.saldoRealizado) < 0 ? "valor--despesa" : ""}>{formatarDinheiro(dados.saldoRealizado)}</b></span><span>Saldo previsto <b className={Number(dados.saldoPrevisto) < 0 ? "valor--despesa" : ""}>{formatarDinheiro(dados.saldoPrevisto)}</b></span></footer>
    </>}
  </section>;
}
