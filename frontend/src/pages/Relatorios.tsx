import { ArrowDownCircle, ArrowUpCircle, BarChart3, HeartHandshake, Landmark, Repeat2 } from "lucide-react";
import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { buscaRollupAnual, buscaTodosLancamentos } from "../api";
import { IconeCategoria } from "../components/IconeCategoria";
import { Banner } from "../components/Banner";
import { ColunaPassado } from "../components/ColunaPassado";
import { ColunaPresente } from "../components/ColunaPresente";
import { ColunaFuturo } from "../components/ColunaFuturo";
import { PainelOperacional } from "../components/PainelOperacional";
import { CartoesSection } from "../settings/CartoesSection";
import { DividasRotativasSection } from "../settings/DividasRotativasSection";
import { ImportarInteligente } from "../components/ImportarInteligente";
import { FaturasCartaoSection } from "../components/FaturasCartaoSection";
import { formatarDespesa, formatarDinheiro, formatarPercentual, somarDinheiro } from "../format";
import type { ContaManualResponse, DashboardResponse, LancamentoPlanejadoResponse, MesDoRollupResponse } from "../types";
import { combinaCategorias } from "../resumoCategorias";
import { resumirFluxoDoMes, resumirMovimentosPorMeio } from "../resumoMeios";

const CORES = ["#7b8ff5", "#ff927b", "#53ad86", "#e0b04f", "#a876d8", "#57a7d8"];
type AbaRelatorio = "categorias" | "fluxo" | "contas" | "cartoes" | "planejamento" | "anual";
export type { AbaRelatorio };

const ABAS: { id: AbaRelatorio; rotulo: string }[] = [
  { id: "categorias", rotulo: "Categorias" },
  { id: "fluxo", rotulo: "Entradas e saídas" },
  { id: "contas", rotulo: "Contas" },
  { id: "cartoes", rotulo: "Cartões" },
  { id: "planejamento", rotulo: "Planejamento" },
  { id: "anual", rotulo: "Anual" },
];

function fatiasDoGrafico(valores: string[], base: number) {
  return valores.slice(0, 6).reduce(
    (estado, valor, indice) => {
      const fim = estado.fim + (Number(valor) / base) * 100;
      return { fim, fatias: [...estado.fatias, `${CORES[indice]} ${estado.fim.toFixed(2)}% ${fim.toFixed(2)}%`] };
    },
    { fim: 0, fatias: [] as string[] },
  ).fatias;
}

export function Relatorios({ dashboard, pendencias, contas, abaInicial, marcacaoInicial, cartaoInicial, aoAlterar }: {
  dashboard: DashboardResponse;
  pendencias: LancamentoPlanejadoResponse[];
  contas: ContaManualResponse[];
  abaInicial?: AbaRelatorio;
  marcacaoInicial?: "CUSTO_FIXO" | "PISO_HUMANO" | null;
  cartaoInicial?: string | null;
  aoAlterar?: () => void;
}) {
  const [aba, setAba] = useState<AbaRelatorio>(abaInicial ?? "categorias");
  const [filtroCategoria, setFiltroCategoria] = useState("");
  const [categoriaAberta, setCategoriaAberta] = useState<string | null>(null);
  const [filtroSustentacao, setFiltroSustentacao] = useState<"CUSTO_FIXO" | "PISO_HUMANO" | null>(marcacaoInicial ?? null);
  const [movimentos, setMovimentos] = useState<LancamentoPlanejadoResponse[]>([]);
  const [erroMeios, setErroMeios] = useState<string | null>(null);
  const [rollupAnual, setRollupAnual] = useState<MesDoRollupResponse[] | null>(null);
  const anoDaCompetencia = Number(dashboard.competencia.split("-")[0]);
  const todasCategorias = combinaCategorias(dashboard.euDoPresente.resumoTriagem, pendencias, dashboard.competencia);
  const categorias = filtroCategoria ? todasCategorias.filter((item) => item.nome === filtroCategoria) : todasCategorias;
  const total = somarDinheiro(categorias.map((item) => item.total));
  const base = Math.max(Number(total), 1);
  const fatias = fatiasDoGrafico(categorias.map((item) => item.total), base);
  const estilo = { "--fatias": fatias.length ? fatias.join(", ") : "#e7ebe6 0 100%" } as CSSProperties;
  const cartoesImportados = dashboard.euDoPresente.cartoes.cartoes;
  const cartoesManuais = dashboard.euDoPresente.cartoesManuais;
  const resumoMeios = useMemo(() => resumirMovimentosPorMeio(
    movimentos,
    contas.map((conta) => ({ id: conta.id, nome: conta.nome })),
    cartoesManuais.map((cartao) => ({ id: cartao.id, nome: cartao.nome })),
    cartoesImportados.map((cartao) => cartao.nome),
  ), [cartoesImportados, cartoesManuais, contas, movimentos]);
  const fluxoDoMes = useMemo(() => resumirFluxoDoMes(movimentos), [movimentos]);
  const itensSustentacao = movimentos.filter((item) => item.marcacaoPlanejamento === filtroSustentacao && item.status !== "CANCELADO");
  const itensDaCategoria = movimentos.filter((item) => item.tipo === "DESPESA" && item.status !== "CANCELADO"
    && item.categoria?.nome.localeCompare(categoriaAberta ?? "", "pt-BR", { sensitivity: "base" }) === 0);

  useEffect(() => {
    let ativo = true;
    async function carregarMovimentos() {
      try {
        const itens = await buscaTodosLancamentos(dashboard.competencia);
        if (ativo) {
          setMovimentos(itens);
          setErroMeios(null);
        }
      } catch (causa) {
        if (ativo) setErroMeios(causa instanceof Error ? causa.message : "Não foi possível carregar os movimentos do mês.");
      }
    }
    void carregarMovimentos();
    return () => { ativo = false; };
  }, [dashboard.competencia]);

  useEffect(() => {
    if (aba !== "anual" || rollupAnual) return;
    buscaRollupAnual(anoDaCompetencia).then(setRollupAnual).catch(() => setRollupAnual([]));
  }, [aba, anoDaCompetencia, rollupAnual]);

  return (
    <section className="relatorios" aria-labelledby="relatorios-titulo">
      <div className="relatorios__abas" role="tablist" aria-label="Tipo de relatório">
        {ABAS.map((item) => <button key={item.id} type="button" className={aba === item.id ? "relatorios__aba relatorios__aba--ativa" : "relatorios__aba"} role="tab" aria-selected={aba === item.id} aria-controls={`painel-${item.id}`} onClick={() => setAba(item.id)}>{item.rotulo}</button>)}
      </div>

      <div className="relatorios__barra">
        <div><p className="eyebrow">{ABAS.find((item) => item.id === aba)?.rotulo}</p><h2 id="relatorios-titulo">{aba === "categorias" ? "Para onde foi o seu dinheiro" : aba === "fluxo" ? "O que entrou e saiu no mês" : aba === "contas" ? "Movimentação por conta no mês" : aba === "cartoes" ? "Compras por cartão no mês" : aba === "anual" ? "O ano inteiro, mês a mês" : "Passado, presente e próximos passos"}</h2></div>
        {aba === "categorias" && <select className="relatorios__filtro" aria-label="Filtrar categoria" value={filtroCategoria} onChange={(evento) => setFiltroCategoria(evento.target.value)}><option value="">Todas as categorias</option>{todasCategorias.map((item) => <option key={item.nome} value={item.nome}>{item.nome}</option>)}</select>}
      </div>

      {aba === "categorias" && <div id="painel-categorias" role="tabpanel">
        <div className="relatorios__premissas" aria-label="Premissas do mês">
          <button type="button" className={filtroSustentacao === "CUSTO_FIXO" ? "selecionado" : ""} onClick={() => setFiltroSustentacao((atual) => atual === "CUSTO_FIXO" ? null : "CUSTO_FIXO")}><Repeat2 size={18} /><span>Custo fixo</span><strong>{formatarDespesa(dashboard.viabilidade.custoFixoTotal)}</strong></button>
          <button type="button" className={filtroSustentacao === "PISO_HUMANO" ? "selecionado" : ""} onClick={() => setFiltroSustentacao((atual) => atual === "PISO_HUMANO" ? null : "PISO_HUMANO")}><HeartHandshake size={18} /><span>Piso humano</span><strong>{formatarDespesa(dashboard.viabilidade.pisoVariavelTotal)}</strong></button>
          <p>Estes totais vêm das despesas marcadas no lançamento; o cadastro antigo é usado apenas enquanto o mês ainda não possuir marcações.</p>
        </div>
        {filtroSustentacao && <section className="relatorios__sustentacao-detalhe" aria-live="polite"><div><h3>{filtroSustentacao === "CUSTO_FIXO" ? "Despesas recorrentes do mês" : "Despesas do piso humano"}</h3><button type="button" className="link-painel" onClick={() => setFiltroSustentacao(null)}>Fechar</button></div>{itensSustentacao.length === 0 ? <p className="vazio">Nenhum lançamento real marcado neste mês; o total exibido ainda vem do catálogo antigo.</p> : <ul>{itensSustentacao.map((item) => <li key={item.id}><span><strong>{item.descricao}</strong><small>{item.categoria?.nome ?? "Sem categoria"} · {item.vencimento}</small></span><b>{formatarDespesa(item.valor)}</b></li>)}</ul>}</section>}
        {categorias.length === 0 ? <div className="estado-vazio estado-vazio--alto"><BarChart3 size={28} /><strong>Nenhuma despesa classificada neste mês</strong><p>Crie ou importe lançamentos e escolha suas categorias para montar o relatório.</p></div> : <div className="relatorio-categorias">
          <div className="relatorio-categorias__lista"><p className="relatorio-categorias__rotulo">Despesas</p>{categorias.map(({ nome, total: valor }, indice) => <button type="button" className={`relatorio-categoria ${categoriaAberta === nome ? "relatorio-categoria--ativa" : ""}`} aria-expanded={categoriaAberta === nome} onClick={() => setCategoriaAberta((atual) => atual === nome ? null : nome)} key={nome}><IconeCategoria nome={nome} cor={CORES[indice % CORES.length]} /><span>{nome}</span><div><strong>{formatarDespesa(valor)}</strong><small>{((Number(valor) / base) * 100).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%</small></div></button>)}<div className="relatorio-categorias__total"><span>Total</span><strong>{formatarDespesa(total)}</strong></div></div>
          <div className="relatorio-categorias__visual"><p>Gastos classificados no mês</p><div className="grafico-rosca grafico-rosca--grande" style={estilo}><span>{categorias.length}</span><small>categorias</small></div></div>
        </div>}
        {categoriaAberta && <section className="relatorio-categoria-detalhe"><div><div><p className="eyebrow">Incluído em {categoriaAberta}</p><h3>Valores específicos do mês</h3></div><button type="button" className="link-painel" onClick={() => setCategoriaAberta(null)}>Fechar</button></div>{itensDaCategoria.length === 0 ? <p className="vazio">Não há itens detalháveis; atualize a análise para sincronizar as categorias importadas.</p> : <ul>{itensDaCategoria.map((item) => <li key={item.id}><span><strong>{item.descricao}</strong><small>{item.vencimento} · {item.origem}</small></span><b>{formatarDespesa(item.valor)}</b></li>)}</ul>}</section>}
      </div>}

      {aba === "fluxo" && <div id="painel-fluxo" role="tabpanel" className="relatorio-painel">
        {erroMeios && <p className="form__erro" role="alert">{erroMeios}</p>}
        <div className="relatorio-indicador relatorio-indicador--receita"><ArrowUpCircle size={22} /><span>Receitas do mês</span><strong>{formatarDinheiro(fluxoDoMes.receitas)}</strong></div>
        <div className="relatorio-indicador relatorio-indicador--despesa"><ArrowDownCircle size={22} /><span>Despesas do mês</span><strong>{formatarDespesa(fluxoDoMes.despesas)}</strong></div>
        <div className="relatorio-indicador"><BarChart3 size={22} /><span>Saldo do mês</span><strong className={Number(fluxoDoMes.saldo) < 0 ? "valor--despesa" : "valor--receita"}>{formatarDinheiro(fluxoDoMes.saldo)}</strong></div>
        <p className="relatorio-painel__nota">Transferências entre contas e lançamentos cancelados ficam fora das entradas e saídas.</p>
      </div>}

      {aba === "contas" && <div id="painel-contas" role="tabpanel" className="relatorio-painel relatorio-lista">
        {erroMeios && <p className="form__erro" role="alert">{erroMeios}</p>}
        {resumoMeios.contas.length === 0 ? <div className="estado-vazio"><Landmark size={26} /><strong>Nenhuma movimentação de conta no mês</strong></div> : resumoMeios.contas.map((conta) => <div className="relatorio-linha" key={conta.id}><span className="relatorio-linha__icone"><Landmark size={18} /></span><div><strong>{conta.nome}</strong><small>Entradas {formatarDinheiro(conta.receitas)} · saídas {formatarDespesa(conta.despesas)}</small></div><b className={Number(conta.saldo) < 0 ? "valor--despesa" : "valor--receita"}>{formatarDinheiro(conta.saldo)}</b></div>)}
      </div>}

      {aba === "cartoes" && <div id="painel-cartoes" role="tabpanel">
        {erroMeios && <p className="form__erro" role="alert">{erroMeios}</p>}
        <FaturasCartaoSection competencia={dashboard.competencia} contas={contas} movimentos={movimentos} nomeInicial={cartaoInicial} aoPagar={aoAlterar} />
        <div className="config__grid config__grid--duas-colunas relatorio-cartoes-cadastro">
          <CartoesSection />
          <ImportarInteligente />
          <DividasRotativasSection />
        </div>
      </div>}

      {aba === "planejamento" && <div id="painel-planejamento" role="tabpanel" className="relatorio-planejamento">
        <Banner viabilidade={dashboard.viabilidade} />
        <PainelOperacional contas={contas} pendencias={pendencias} />
        <main className="colunas colunas--planejamento">
          <ColunaPassado dados={dashboard.euDoPassado} />
          <ColunaPresente dados={dashboard.euDoPresente} />
          <ColunaFuturo dados={dashboard.euDoFuturo} />
        </main>
      </div>}

      {aba === "anual" && <div id="painel-anual" role="tabpanel" className="relatorio-painel relatorio-lista">
        {!rollupAnual ? <p className="vazio">Carregando...</p> : (
          <table className="tabela-anual">
            <thead><tr><th>Mês</th><th>Entrada</th><th>Saída</th><th>% economia</th></tr></thead>
            <tbody>
              {rollupAnual.map((mes) => (
                <tr key={mes.competencia}>
                  <td>{mes.competencia}</td>
                  <td>{formatarDinheiro(mes.entrada)}</td>
                  <td>{formatarDespesa(mes.saida)}</td>
                  <td className={Number(mes.taxaEconomia) < 0 ? "valor--despesa" : "valor--receita"}>{formatarPercentual(mes.taxaEconomia)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>}
    </section>
  );
}
