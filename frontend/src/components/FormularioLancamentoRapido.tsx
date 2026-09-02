import { useRef, useState } from "react";
import { postLiquidarLancamentoPlanejado, putLancamentoPlanejado } from "../api";
import { formatarEntradaDeDinheiro, valorDaEntradaDeDinheiro } from "../format";
import type { CartaoManualResponse, ContaManualResponse, EscopoEdicaoRecorrencia, LancamentoPlanejadoRequest, LancamentoPlanejadoResponse, MarcacaoPlanejamento, OrigemReceita } from "../types";
import { CreditCard, HeartHandshake, Plus, Repeat2, Save, X } from "lucide-react";
import { IconeCategoria } from "./IconeCategoria";
import { CATEGORIAS_LANCAMENTO } from "../categorias";
import { CampoRecorrencia } from "./CampoRecorrencia";
import { EscolhaEscopoRecorrencia } from "./EscolhaEscopoRecorrencia";

export type TipoRapido = "DESPESA" | "RECEITA" | "TRANSFERENCIA";

const TITULOS: Record<TipoRapido, string> = {
  DESPESA: "Nova despesa",
  RECEITA: "Nova receita",
  TRANSFERENCIA: "Transferir entre contas",
};

const ORIGENS_RECEITA: { valor: OrigemReceita; rotulo: string }[] = [
  { valor: "SALARIO", rotulo: "Salário" },
  { valor: "INVESTIMENTO", rotulo: "Investimento" },
  { valor: "EMPRESTIMO", rotulo: "Empréstimo" },
];

export function FormularioLancamentoRapido({
  tipo,
  contas,
  cartoes,
  aoFechar,
  aoConcluir,
  inicial,
}: {
  tipo: TipoRapido;
  contas: ContaManualResponse[];
  cartoes: CartaoManualResponse[];
  aoFechar: () => void;
  aoConcluir: () => Promise<void>;
  inicial?: LancamentoPlanejadoResponse;
}) {
  const [descricao, setDescricao] = useState(inicial?.descricao ?? "");
  const [valor, setValor] = useState(inicial ? formatarEntradaDeDinheiro(String(Math.round(Number(inicial.valor) * 100))) : formatarEntradaDeDinheiro("0"));
  const [vencimento, setVencimento] = useState(inicial?.vencimento ?? new Date().toISOString().slice(0, 10));
  const [origem, setOrigem] = useState(inicial?.contaOrigemId ?? "");
  const [destino, setDestino] = useState(inicial?.contaDestinoId ?? "");
  const [cartao, setCartao] = useState(inicial?.cartaoManualId ?? "");
  const [categoria, setCategoria] = useState(inicial?.categoria?.nome ?? "");
  const [origemReceita, setOrigemReceita] = useState<OrigemReceita | "">(inicial?.origemReceita ?? "");
  const [marcacao, setMarcacao] = useState<MarcacaoPlanejamento>(inicial?.marcacaoPlanejamento ?? "NENHUMA");
  const [recorrente, setRecorrente] = useState(Boolean(inicial?.serieId));
  const [diaRecorrencia, setDiaRecorrencia] = useState(
    inicial?.diaRecorrencia ?? Number((inicial?.vencimento ?? new Date().toISOString().slice(0, 10)).slice(-2)),
  );
  const [perguntandoEscopo, setPerguntandoEscopo] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const valorRef = useRef<HTMLInputElement>(null);
  const pendente = useRef<{ dados: LancamentoPlanejadoRequest; criarOutra: boolean } | null>(null);
  function alteraValor(bruto: string) { const proximo = formatarEntradaDeDinheiro(bruto); setValor(proximo); requestAnimationFrame(() => valorRef.current?.setSelectionRange(proximo.length, proximo.length)); }

  async function salvar(evento: React.SyntheticEvent, criarOutra = false) {
    evento.preventDefault();
    const numero = valorDaEntradaDeDinheiro(valor);
    if (!descricao.trim() || numero === null) {
      setErro("Informe uma descrição e um valor maior que zero.");
      return;
    }
    if (tipo === "TRANSFERENCIA" && (!origem || !destino || origem === destino)) {
      setErro("Escolha contas de origem e destino diferentes.");
      return;
    }
    if (tipo === "DESPESA" && !categoria) {
      setErro("Escolha uma categoria para atualizar seus gráficos corretamente.");
      return;
    }
    if (tipo === "RECEITA" && !origemReceita) {
      setErro("Escolha a origem da receita.");
      return;
    }
    const categoriaEscolhida = CATEGORIAS_LANCAMENTO.find((item) => item.nome === categoria);
    const dados: LancamentoPlanejadoRequest = {
      descricao: descricao.trim(), tipo, valor: numero, vencimento,
      contaOrigemId: tipo === "TRANSFERENCIA" || (tipo === "DESPESA" && !cartao) ? origem || null : null,
      contaDestinoId: tipo === "TRANSFERENCIA" || tipo === "RECEITA" ? destino || null : null,
      categoria: categoriaEscolhida?.nome ?? null,
      grupoCategoria: categoriaEscolhida?.grupo ?? null,
      naturezaCategoria: categoriaEscolhida ? (marcacao === "CUSTO_FIXO" ? "FIXO" : "VARIAVEL") : null,
      cartaoManualId: tipo === "DESPESA" ? cartao || null : null,
      marcacaoPlanejamento: tipo === "TRANSFERENCIA" ? "NENHUMA" : marcacao,
      origemReceita: tipo === "RECEITA" ? origemReceita || null : null,
      diaRecorrencia: tipo !== "TRANSFERENCIA" && recorrente ? diaRecorrencia : null,
    };
    if (inicial?.serieId) {
      pendente.current = { dados, criarOutra };
      setPerguntandoEscopo(true);
      return;
    }
    await efetivaSalvar(dados, criarOutra, "ESTA");
  }

  async function efetivaSalvar(dados: LancamentoPlanejadoRequest, criarOutra: boolean, escopo: EscopoEdicaoRecorrencia) {
    setPerguntandoEscopo(false);
    setEnviando(true);
    setErro(null);
    try {
      const lancamento = await putLancamentoPlanejado(inicial?.id ?? crypto.randomUUID(), { ...dados, escopoEdicao: escopo });
      if (!inicial) await postLiquidarLancamentoPlanejado(lancamento.id);
      await aoConcluir();
      if (criarOutra) {
        setDescricao("");
        setValor(formatarEntradaDeDinheiro("0"));
        setCategoria("");
        setOrigemReceita("");
        setMarcacao("NENHUMA");
        setRecorrente(false);
      } else {
        aoFechar();
      }
    } catch (causa: unknown) {
      setErro(causa instanceof Error ? causa.message : "Não foi possível registrar o lançamento.");
    } finally {
      setEnviando(false);
    }
  }

  return <div className="modal" role="presentation" onMouseDown={aoFechar}>
    <section className="modal__conteudo" role="dialog" aria-modal="true" aria-labelledby="lancamento-rapido-titulo" onMouseDown={(evento) => evento.stopPropagation()}>
      <div className="modal__cabecalho"><h2 id="lancamento-rapido-titulo">{inicial ? "Editar lançamento" : TITULOS[tipo]}</h2><button className="modal__fechar" type="button" onClick={aoFechar} aria-label="Fechar"><X size={20} /></button></div>
      <p className="modal__ajuda">Ao salvar, o lançamento entra no seu histórico e atualiza a análise.</p>
      <form className="form" onSubmit={salvar}>
        <div className="form__campo"><label htmlFor="rapido-descricao">Descrição</label><input id="rapido-descricao" autoFocus value={descricao} onChange={(evento) => setDescricao(evento.target.value)} placeholder={tipo === "RECEITA" ? "Ex.: Salário" : "Ex.: Mercado"} /></div>
        <div className="modal__linha"><div className="form__campo"><label htmlFor="rapido-valor">Valor</label><input ref={valorRef} id="rapido-valor" inputMode="numeric" value={valor} onFocus={(evento) => evento.currentTarget.setSelectionRange(evento.currentTarget.value.length, evento.currentTarget.value.length)} onClick={(evento) => evento.currentTarget.setSelectionRange(evento.currentTarget.value.length, evento.currentTarget.value.length)} onChange={(evento) => alteraValor(evento.target.value)} placeholder="R$ 0,00" /></div><div className="form__campo"><label htmlFor="rapido-data">Data</label><input id="rapido-data" type="date" value={vencimento} onChange={(evento) => setVencimento(evento.target.value)} /></div></div>
        {tipo === "TRANSFERENCIA" && <><div className="form__campo"><label htmlFor="rapido-origem">Sai de</label><select id="rapido-origem" value={origem} onChange={(evento) => setOrigem(evento.target.value)}><option value="">Selecione uma conta</option>{contas.map((conta) => <option value={conta.id} key={conta.id}>{conta.nome}</option>)}</select></div><div className="form__campo"><label htmlFor="rapido-destino">Entra em</label><select id="rapido-destino" value={destino} onChange={(evento) => setDestino(evento.target.value)}><option value="">Selecione uma conta</option>{contas.map((conta) => <option value={conta.id} key={conta.id}>{conta.nome}</option>)}</select></div>{contas.length < 2 && <p className="form__aviso">Cadastre pelo menos duas contas para transferir entre elas.</p>}</>}
        {tipo === "DESPESA" && <div className="form__campo"><label htmlFor="rapido-categoria">Categoria</label><div className="seletor-com-icone"><IconeCategoria nome={categoria} /><select id="rapido-categoria" value={categoria} onChange={(evento) => setCategoria(evento.target.value)} required><option value="">Escolha uma categoria</option>{CATEGORIAS_LANCAMENTO.map((item) => <option value={item.nome} key={item.nome}>{item.nome}</option>)}</select></div></div>}
        {tipo === "RECEITA" && <div className="form__campo"><label htmlFor="rapido-origem-receita">Origem da receita</label><select id="rapido-origem-receita" value={origemReceita} onChange={(evento) => setOrigemReceita(evento.target.value as OrigemReceita | "")} required><option value="">Escolha a origem</option>{ORIGENS_RECEITA.map((item) => <option value={item.valor} key={item.valor}>{item.rotulo}</option>)}</select></div>}
        {tipo === "DESPESA" && <><div className="form__campo"><label htmlFor="rapido-cartao">Cartão (opcional)</label><div className="seletor-com-icone"><span className="icone-categoria" aria-hidden="true"><CreditCard size={18} /></span><select id="rapido-cartao" value={cartao} onChange={(evento) => { setCartao(evento.target.value); if (evento.target.value) setOrigem(""); }}><option value="">Pagar no débito / pela conta</option>{cartoes.map((item) => <option value={item.id} key={item.id}>{item.nome}</option>)}</select></div></div><div className="form__campo"><label htmlFor="rapido-conta-despesa">Conta de débito (opcional)</label><select id="rapido-conta-despesa" value={origem} disabled={Boolean(cartao)} onChange={(evento) => { setOrigem(evento.target.value); if (evento.target.value) setCartao(""); }}><option value="">Não alterar saldo de conta</option>{contas.map((conta) => <option value={conta.id} key={conta.id}>{conta.nome}</option>)}</select></div></>}
        {tipo === "DESPESA" && <fieldset className="marcacao-opcoes"><legend>O que essa despesa representa?</legend><label className={marcacao === "CUSTO_FIXO" ? "marcacao-opcao marcacao-opcao--ativa" : "marcacao-opcao"}><input type="checkbox" checked={marcacao === "CUSTO_FIXO"} onChange={(evento) => setMarcacao(evento.target.checked ? "CUSTO_FIXO" : "NENHUMA")} /><Repeat2 size={18} /><span><strong>Custo fixo</strong><small>Compromisso recorrente do mês</small></span></label><label className={marcacao === "PISO_HUMANO" ? "marcacao-opcao marcacao-opcao--ativa" : "marcacao-opcao"}><input type="checkbox" checked={marcacao === "PISO_HUMANO"} onChange={(evento) => setMarcacao(evento.target.checked ? "PISO_HUMANO" : "NENHUMA")} /><HeartHandshake size={18} /><span><strong>Piso humano</strong><small>Mínimo necessário para viver bem</small></span></label></fieldset>}
        {tipo === "RECEITA" && <fieldset className="marcacao-opcoes"><legend>Recorrência</legend><label className={marcacao === "RECEITA_RECORRENTE" ? "marcacao-opcao marcacao-opcao--ativa" : "marcacao-opcao"}><input type="checkbox" checked={marcacao === "RECEITA_RECORRENTE"} onChange={(evento) => setMarcacao(evento.target.checked ? "RECEITA_RECORRENTE" : "NENHUMA")} /><Repeat2 size={18} /><span><strong>Receita recorrente</strong><small>Ex.: salário que entra todos os meses</small></span></label></fieldset>}
        {tipo === "RECEITA" && <div className="form__campo"><label htmlFor="rapido-conta-receita">Entra na conta (opcional)</label><select id="rapido-conta-receita" value={destino} onChange={(evento) => setDestino(evento.target.value)}><option value="">Não alterar saldo de conta</option>{contas.map((conta) => <option value={conta.id} key={conta.id}>{conta.nome}</option>)}</select></div>}
        {tipo !== "TRANSFERENCIA" && (
          <CampoRecorrencia
            recorrente={recorrente}
            dia={diaRecorrencia}
            jaEhSerie={Boolean(inicial?.serieId)}
            onChangeRecorrente={setRecorrente}
            onChangeDia={setDiaRecorrencia}
          />
        )}
        {erro && <p className="form__erro" role="alert">{erro}</p>}
        {perguntandoEscopo && pendente.current ? (
          <EscolhaEscopoRecorrencia
            onEscolher={(escopo) => efetivaSalvar(pendente.current!.dados, pendente.current!.criarOutra, escopo)}
            onCancelar={() => setPerguntandoEscopo(false)}
          />
        ) : (
          <div className="form__acoes"><button type="button" className="botao--texto" onClick={aoFechar}>Cancelar</button><button type="submit" disabled={enviando}><Save size={16} />{enviando ? "Salvando…" : "Salvar"}</button><button type="button" className="botao--secundario" disabled={enviando} onClick={(evento) => salvar(evento, true)}><Plus size={16} />Salvar e criar outra</button></div>
        )}
      </form>
    </section>
  </div>;
}
