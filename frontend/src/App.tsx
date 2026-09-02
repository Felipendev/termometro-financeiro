import { useCallback, useEffect, useState } from "react";
import { BarChart3, ChevronLeft, ChevronRight, LayoutDashboard, ListChecks, Rows3, Settings, X } from "lucide-react";
import { ApiError, buscaDashboardInicio, postNaoGasto, postTriagem, verificaCompatibilidade } from "./api";
import type { DashboardInicioResponse, LancamentoPlanejadoResponse } from "./types";
import { competenciaAtual, formatarCompetencia } from "./format";
import { NAVEGACAO_PRINCIPAL, type AbaPrincipal } from "./navigation";
import { Skeleton } from "./components/Skeleton";
import { Planilha } from "./pages/Planilha";
import { PainelVisaoGeral } from "./components/PainelVisaoGeral";
import { FormularioLancamentoRapido, type TipoRapido } from "./components/FormularioLancamentoRapido";
import { Lancamentos } from "./pages/Lancamentos";
import { Relatorios, type AbaRelatorio } from "./pages/Relatorios";
import { Planejamento } from "./pages/Configuracoes";
import "./App.css";
import "./totais.css";

type Estado =
  | { tipo: "carregando" }
  | { tipo: "erro"; mensagem: string }
  | { tipo: "pronto"; dashboard: DashboardInicioResponse };

const ICONES_NAVEGACAO = {
  dashboard: LayoutDashboard,
  lancamentos: ListChecks,
  planilha: Rows3,
  relatorios: BarChart3,
} as const;

const TITULOS: Record<AbaPrincipal, { apoio: string; titulo: string }> = {
  dashboard: { apoio: "Visão geral", titulo: "Seu mês, sem surpresas" },
  lancamentos: { apoio: "Lançamentos", titulo: "Tudo que entrou e saiu" },
  planilha: { apoio: "Planilha", titulo: "Seus meses, lado a lado" },
  relatorios: { apoio: "Relatórios", titulo: "Entenda seus gastos" },
};

function deslocarCompetencia(competencia: string, deslocamento: number) {
  const [ano, mes] = competencia.split("-").map(Number);
  const data = new Date(Date.UTC(ano, mes - 1 + deslocamento, 1));
  return `${data.getUTCFullYear()}-${String(data.getUTCMonth() + 1).padStart(2, "0")}`;
}

function App() {
  const [aba, setAba] = useState<AbaPrincipal>("dashboard");
  const [competencia, setCompetencia] = useState(competenciaAtual());
  const [estado, setEstado] = useState<Estado>({ tipo: "carregando" });
  const [rodando, setRodando] = useState<"nao-gasto" | "triagem" | null>(null);
  const [mensagemOperacao, setMensagemOperacao] = useState<string | null>(null);
  const [tipoRapido, setTipoRapido] = useState<TipoRapido | null>(null);
  const [editando, setEditando] = useState<LancamentoPlanejadoResponse | null>(null);
  const [versaoLancamentos, setVersaoLancamentos] = useState(0);
  const [configuracoesAbertas, setConfiguracoesAbertas] = useState(false);
  const [abaInicialRelatorios, setAbaInicialRelatorios] = useState<AbaRelatorio>("categorias");
  const [marcacaoInicialRelatorios, setMarcacaoInicialRelatorios] = useState<"CUSTO_FIXO" | "PISO_HUMANO" | null>(null);
  const [cartaoInicialRelatorios, setCartaoInicialRelatorios] = useState<string | null>(null);

  const carregar = useCallback((competenciaAlvo: string, signal?: AbortSignal) => {
    setEstado({ tipo: "carregando" });
    verificaCompatibilidade(signal)
      .then(() => buscaDashboardInicio(competenciaAlvo, signal))
      .then((dashboard) => setEstado({ tipo: "pronto", dashboard }))
      .catch((erro: unknown) => {
        if (erro instanceof DOMException && erro.name === "AbortError") return;
        setEstado({ tipo: "erro", mensagem: erro instanceof ApiError ? erro.message : "Não foi possível falar com o servidor." });
      });
  }, []);

  useEffect(() => {
    const controlador = new AbortController();
    carregar(competencia, controlador.signal);
    return () => controlador.abort();
  }, [competencia, carregar]);

  async function rodarNaoGasto() {
    setRodando("nao-gasto");
    setMensagemOperacao(null);
    try {
      const resultado = await postNaoGasto(competencia);
      setMensagemOperacao(`Conciliação concluída: ${resultado.pagamentosDeFaturaCasados} fatura(s) e ${resultado.transferenciasCasadas} transferência(s) identificadas.`);
      carregar(competencia);
    } catch (erro: unknown) {
      setMensagemOperacao(erro instanceof ApiError ? erro.message : "Não foi possível conciliar os lançamentos.");
    } finally {
      setRodando(null);
    }
  }

  async function rodarTriagem() {
    setRodando("triagem");
    setMensagemOperacao(null);
    try {
      const resultado = await postTriagem(competencia);
      setMensagemOperacao(`Análise atualizada: ${resultado.triadas} de ${resultado.analisadas} transações classificadas.`);
      carregar(competencia);
    } catch (erro: unknown) {
      setMensagemOperacao(erro instanceof ApiError ? erro.message : "Não foi possível atualizar a análise.");
    } finally {
      setRodando(null);
    }
  }

  async function concluirLancamento() {
    await rodarNaoGasto();
    await rodarTriagem();
    setVersaoLancamentos((versao) => versao + 1);
    carregar(competencia);
  }

  function atualizarAposPagamento() {
    setVersaoLancamentos((versao) => versao + 1);
    carregar(competencia);
  }

  const exibeCompetencia = aba !== "planilha";

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar__conteudo">
          <button type="button" className="marca" onClick={() => setAba("dashboard")} aria-label="Ir para a visão geral">
            <span className="marca__simbolo">T</span><span>termômetro</span>
          </button>
          <nav className="topbar__nav" aria-label="Navegação principal">
            {NAVEGACAO_PRINCIPAL.map((item) => {
              const Icone = ICONES_NAVEGACAO[item.aba];
              return (
                <button key={item.aba} type="button" className={aba === item.aba ? "nav-item nav-item--ativo" : "nav-item"} onClick={() => setAba(item.aba)} aria-current={aba === item.aba ? "page" : undefined}>
                  <Icone size={16} aria-hidden="true" /><span>{item.rotulo}</span>
                </button>
              );
            })}
          </nav>
          <button type="button" className="botao--icone topbar__configuracoes" onClick={() => setConfiguracoesAbertas(true)} aria-label="Configurações">
            <Settings size={17} aria-hidden="true" />
          </button>
        </div>
      </header>

      <div className={aba === "planilha" ? "app app--planilha" : "app"}>
        <header className="app__cabecalho">
          <div><p className="eyebrow">{TITULOS[aba].apoio}</p><h1>{TITULOS[aba].titulo}</h1></div>
          {exibeCompetencia && (
            <div className="seletor-competencia" role="group" aria-label="Selecionar competência">
              <button type="button" className="botao--icone" onClick={() => setCompetencia((atual) => deslocarCompetencia(atual, -1))} aria-label="Mês anterior"><ChevronLeft size={17} /></button>
              <label htmlFor="competencia-input"><span>Período</span><input id="competencia-input" type="month" value={competencia} onChange={(evento) => setCompetencia(evento.target.value)} /><strong>{formatarCompetencia(competencia)}</strong></label>
              <button type="button" className="botao--icone" onClick={() => setCompetencia((atual) => deslocarCompetencia(atual, 1))} aria-label="Próximo mês"><ChevronRight size={17} /></button>
            </div>
          )}
        </header>

        {mensagemOperacao && <p className="app__mensagem-operacao" role="status">{mensagemOperacao}</p>}

        {estado.tipo === "carregando" ? (
          <Skeleton />
        ) : estado.tipo === "erro" ? (
          <div className="erro" role="alert"><p>{estado.mensagem}</p><button type="button" onClick={() => carregar(competencia)}>Tentar de novo</button></div>
        ) : aba === "planilha" ? (
          <Planilha />
        ) : aba === "lancamentos" ? (
          <Lancamentos competencia={competencia} versao={versaoLancamentos} contas={estado.tipo === "pronto" ? estado.dashboard.contasManuais : []} cartoes={estado.tipo === "pronto" ? estado.dashboard.analise.euDoPresente.cartoesManuais : []} aoNovo={setTipoRapido} aoEditar={setEditando} aoAlterar={concluirLancamento} />
        ) : aba === "relatorios" ? (
          <Relatorios dashboard={estado.dashboard.analise} pendencias={estado.dashboard.pendencias} contas={estado.dashboard.contasManuais} abaInicial={abaInicialRelatorios} marcacaoInicial={marcacaoInicialRelatorios} cartaoInicial={cartaoInicialRelatorios} aoAlterar={atualizarAposPagamento} />
        ) : (
          <PainelVisaoGeral dashboard={estado.dashboard.analise} pendencias={estado.dashboard.pendencias} onRodarNaoGasto={rodarNaoGasto} onRodarTriagem={rodarTriagem} onImportar={() => { setCartaoInicialRelatorios(null); setMarcacaoInicialRelatorios(null); setAbaInicialRelatorios("cartoes"); setAba("relatorios"); }} onNovoLancamento={setTipoRapido} onVerRelatorios={() => { setCartaoInicialRelatorios(null); setMarcacaoInicialRelatorios(null); setAbaInicialRelatorios("categorias"); setAba("relatorios"); }} onVerCartoes={() => { setCartaoInicialRelatorios(null); setMarcacaoInicialRelatorios(null); setAbaInicialRelatorios("cartoes"); setAba("relatorios"); }} onVerFatura={(nome) => { setCartaoInicialRelatorios(nome); setMarcacaoInicialRelatorios(null); setAbaInicialRelatorios("cartoes"); setAba("relatorios"); }} onVerSustentacao={(marcacao) => { setCartaoInicialRelatorios(null); setMarcacaoInicialRelatorios(marcacao); setAbaInicialRelatorios("categorias"); setAba("relatorios"); }} rodando={rodando} />
        )}

        {estado.tipo === "pronto" && (tipoRapido || editando) && (
          <FormularioLancamentoRapido tipo={editando?.tipo ?? tipoRapido!} inicial={editando ?? undefined} contas={estado.dashboard.contasManuais} cartoes={estado.dashboard.analise.euDoPresente.cartoesManuais} aoFechar={() => { setTipoRapido(null); setEditando(null); }} aoConcluir={concluirLancamento} />
        )}

        {configuracoesAbertas && (
          <div className="modal" role="presentation" onMouseDown={() => setConfiguracoesAbertas(false)}>
            <section className="modal__conteudo modal__conteudo--largo" role="dialog" aria-modal="true" aria-labelledby="configuracoes-titulo" onMouseDown={(evento) => evento.stopPropagation()}>
              <div className="modal__cabecalho">
                <h2 id="configuracoes-titulo">Configurações</h2>
                <button className="modal__fechar" type="button" onClick={() => setConfiguracoesAbertas(false)} aria-label="Fechar"><X size={20} /></button>
              </div>
              <Planejamento />
            </section>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
