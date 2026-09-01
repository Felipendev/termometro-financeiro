import { Fragment, useCallback, useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, Pencil, Plus, Rows3, Trash2, Wand2 } from "lucide-react";
import {
  ApiError,
  buscaPlanilha,
  deleteLancamentoNaPlanilha,
  putDiarioDoDia,
  putDiarioEmSerie,
  putObservacaoDoDia,
  putSaldoInicialPlanilha,
} from "../api";
import { SimuladorDeDecisao } from "../components/SimuladorDeDecisao";
import { EditorLancamentoPlanilha } from "../components/EditorLancamentoPlanilha";
import { formatarCompetencia, formatarDespesa, formatarDinheiro, normalizarDecimal } from "../format";
import type { DiaDaPlanilhaResponse, LancamentoDaPlanilhaResponse, PlanilhaMesResponse } from "../types";

const MESES_VISIVEIS = 4;
const NOMES_DIA_SEMANA = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"];

function competenciaMaisMeses(competencia: string, quantidade: number): string {
  const [ano, mes] = competencia.split("-").map(Number);
  const data = new Date(Date.UTC(ano, mes - 1 + quantidade, 1));
  return `${data.getUTCFullYear()}-${String(data.getUTCMonth() + 1).padStart(2, "0")}`;
}

function diaDaSemanaCurto(data: string): string {
  const [ano, mes, dia] = data.split("-").map(Number);
  return NOMES_DIA_SEMANA[new Date(Date.UTC(ano, mes - 1, dia)).getUTCDay()];
}

function numeroDoDia(data: string): string {
  return data.split("-")[2];
}

function ValorDaPlanilha({ valor }: { valor: string }) {
  return (
    <>
      <span className="planilha__valor-desktop">{formatarDinheiro(valor)}</span>
      <span className="planilha__valor-mobile">{formatarDinheiro(valor).replace(/R\$\s?/, "")}</span>
    </>
  );
}

const CLASSE_POR_FAIXA: Record<string, string> = {
  VERMELHO: "planilha-saldo--vermelho",
  LARANJA: "planilha-saldo--laranja",
  AMARELO: "planilha-saldo--amarelo",
  VERDE_CLARO: "planilha-saldo--verde-claro",
  VERDE: "planilha-saldo--verde",
};

const SELO_USO_DE_CREDITO: Record<string, { texto: string; classe: string }> = {
  DEFICIT_DISFARCADO: { texto: "Déficit disfarçado", classe: "planilha__selo--perigo" },
  ATENCAO: { texto: "Atenção", classe: "planilha__selo--alerta" },
};

type Estado =
  | { tipo: "carregando" }
  | { tipo: "precisa-saldo-inicial" }
  | { tipo: "erro"; mensagem: string }
  | { tipo: "pronto"; meses: PlanilhaMesResponse[] };

type Arraste = { competencia: string; dataOrigem: string; valor: string } | null;

export function Planilha() {
  const [mesInicial, setMesInicial] = useState(() => {
    const hoje = new Date();
    return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, "0")}`;
  });
  const [estado, setEstado] = useState<Estado>({ tipo: "carregando" });
  const [celulaEmEdicao, setCelulaEmEdicao] = useState<{ competencia: string; data: string } | null>(null);
  const [valorEmEdicao, setValorEmEdicao] = useState("");
  const [arraste, setArraste] = useState<Arraste>(null);
  const [dataAlvoArraste, setDataAlvoArraste] = useState<string | null>(null);
  const [composicao, setComposicao] = useState<DiaDaPlanilhaResponse | null>(null);
  const [editorLancamento, setEditorLancamento] = useState<{ item?: LancamentoDaPlanilhaResponse } | null>(null);
  const [erroComposicao, setErroComposicao] = useState<string | null>(null);
  const [observacaoAberta, setObservacaoAberta] = useState<DiaDaPlanilhaResponse | null>(null);
  const [textoObservacao, setTextoObservacao] = useState("");
  const [saldoInicialData, setSaldoInicialData] = useState("");
  const [saldoInicialValor, setSaldoInicialValor] = useState("");
  const [enviandoSaldoInicial, setEnviandoSaldoInicial] = useState(false);
  const [simuladorAberto, setSimuladorAberto] = useState(false);
  const [layoutCompacto, setLayoutCompacto] = useState(() => window.matchMedia("(max-width: 900px)").matches);
  const arrasteRef = useRef<Arraste>(null);

  useEffect(() => {
    const consulta = window.matchMedia("(max-width: 900px)");
    const atualizarLayout = () => setLayoutCompacto(consulta.matches);
    consulta.addEventListener("change", atualizarLayout);
    return () => consulta.removeEventListener("change", atualizarLayout);
  }, []);

  const carregar = useCallback((mesBase: string) => {
    setEstado({ tipo: "carregando" });
    const competencias = Array.from({ length: MESES_VISIVEIS }, (_, indice) =>
      competenciaMaisMeses(mesBase, indice),
    );
    Promise.all(competencias.map((competencia) => buscaPlanilha(competencia)))
      .then((meses) => setEstado({ tipo: "pronto", meses }))
      .catch((erro: unknown) => {
        if (erro instanceof ApiError && erro.message.toLowerCase().includes("saldo inicial")) {
          setEstado({ tipo: "precisa-saldo-inicial" });
          return;
        }
        setEstado({ tipo: "erro", mensagem: erro instanceof ApiError ? erro.message : "Não foi possível carregar a planilha." });
      });
  }, []);

  useEffect(() => carregar(mesInicial), [mesInicial, carregar]);

  async function confirmarSaldoInicial() {
    const valorNormalizado = normalizarDecimal(saldoInicialValor);
    if (!saldoInicialData || !valorNormalizado) return;
    setEnviandoSaldoInicial(true);
    try {
      await putSaldoInicialPlanilha(saldoInicialData, valorNormalizado);
      carregar(mesInicial);
    } catch (erro: unknown) {
      setEstado({ tipo: "erro", mensagem: erro instanceof ApiError ? erro.message : "Não foi possível salvar o saldo inicial." });
    } finally {
      setEnviandoSaldoInicial(false);
    }
  }

  function abrirEdicaoDiario(competencia: string, dia: DiaDaPlanilhaResponse) {
    setCelulaEmEdicao({ competencia, data: dia.data });
    setValorEmEdicao(dia.diario === "0.00" ? "" : dia.diario.replace(".", ","));
  }

  async function confirmarDiario() {
    if (!celulaEmEdicao) return;
    const valorNormalizado = normalizarDecimal(valorEmEdicao || "0");
    const alvo = celulaEmEdicao;
    setCelulaEmEdicao(null);
    if (!valorNormalizado) return;
    await putDiarioDoDia(alvo.data, valorNormalizado);
    carregar(mesInicial);
  }

  function iniciarArraste(competencia: string, dia: DiaDaPlanilhaResponse) {
    const valor = arraste ?? { competencia, dataOrigem: dia.data, valor: dia.diario };
    setArraste(valor);
    arrasteRef.current = valor;
    setDataAlvoArraste(dia.data);
  }

  useEffect(() => {
    if (!arraste) return;

    function aoMover(evento: MouseEvent) {
      const alvo = document.elementFromPoint(evento.clientX, evento.clientY);
      const celula = alvo instanceof Element ? alvo.closest<HTMLElement>("[data-diario-dia]") : null;
      if (!celula || celula.dataset.diarioCompetencia !== arrasteRef.current?.competencia) return;
      setDataAlvoArraste(celula.dataset.diarioDia ?? null);
    }

    async function aoSoltar() {
      const atual = arrasteRef.current;
      const alvo = dataAlvoArraste;
      setArraste(null);
      arrasteRef.current = null;
      setDataAlvoArraste(null);
      if (!atual || !alvo || alvo === atual.dataOrigem) return;
      const [de, ate] = alvo > atual.dataOrigem ? [atual.dataOrigem, alvo] : [alvo, atual.dataOrigem];
      await putDiarioEmSerie(de, ate, atual.valor);
      carregar(mesInicial);
    }

    document.addEventListener("mousemove", aoMover);
    document.addEventListener("mouseup", aoSoltar);
    return () => {
      document.removeEventListener("mousemove", aoMover);
      document.removeEventListener("mouseup", aoSoltar);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [arraste, dataAlvoArraste, mesInicial]);

  function abrirObservacao(dia: DiaDaPlanilhaResponse) {
    setObservacaoAberta(dia);
    setTextoObservacao(dia.observacao ?? "");
  }

  function abrirComposicao(dia: DiaDaPlanilhaResponse) {
    setComposicao(dia);
    setEditorLancamento(null);
    setErroComposicao(null);
  }

  function lancamentoSalvo() {
    setEditorLancamento(null);
    setComposicao(null);
    carregar(mesInicial);
  }

  async function removerLancamento(item: LancamentoDaPlanilhaResponse) {
    if (!item.id || !window.confirm(`Remover “${item.descricao}” deste dia?`)) return;
    setErroComposicao(null);
    try {
      await deleteLancamentoNaPlanilha(item.id);
      setComposicao(null);
      carregar(mesInicial);
    } catch (erro) {
      setErroComposicao(erro instanceof Error ? erro.message : "Não foi possível remover o lançamento.");
    }
  }

  async function salvarObservacao() {
    if (!observacaoAberta || !textoObservacao.trim()) return;
    await putObservacaoDoDia(observacaoAberta.data, textoObservacao.trim());
    setObservacaoAberta(null);
    carregar(mesInicial);
  }

  if (estado.tipo === "precisa-saldo-inicial") {
    return (
      <div className="estado-vazio estado-vazio--alto planilha-saldo-inicial">
        <Rows3 size={28} />
        <strong>Antes de ver a planilha, informe um saldo de partida</strong>
        <p>Um saldo conhecido numa data específica — a cascata parte dele pra frente.</p>
        <div className="planilha-saldo-inicial__form">
          <label>
            <span>Saldo em</span>
            <input type="date" value={saldoInicialData} onChange={(evento) => setSaldoInicialData(evento.target.value)} />
          </label>
          <label>
            <span>Valor</span>
            <input type="text" placeholder="0,00" value={saldoInicialValor} onChange={(evento) => setSaldoInicialValor(evento.target.value)} />
          </label>
          <button type="button" onClick={confirmarSaldoInicial} disabled={enviandoSaldoInicial}>
            {enviandoSaldoInicial ? "Salvando..." : "Salvar e ver a planilha"}
          </button>
        </div>
      </div>
    );
  }

  if (estado.tipo === "carregando") {
    return <div className="estado-vazio estado-vazio--alto"><Rows3 size={28} /><strong>Carregando a planilha...</strong></div>;
  }

  if (estado.tipo === "erro") {
    return (
      <div className="erro" role="alert">
        <p>{estado.mensagem}</p>
        <button type="button" onClick={() => carregar(mesInicial)}>Tentar de novo</button>
      </div>
    );
  }

  const mesesExibidos = estado.meses.slice(0, layoutCompacto ? 1 : 2);
  const maiorQuantidadeDeDias = Math.max(...mesesExibidos.map((mes) => mes.dias.length));

  return (
    <div className="planilha">
      <div className="planilha__navegacao">
        <button type="button" className="botao--icone" onClick={() => setMesInicial((atual) => competenciaMaisMeses(atual, -1))} aria-label="Meses anteriores">
          <ChevronLeft size={17} />
        </button>
        <strong className="planilha__periodo">
          <span className="planilha__periodo-desktop">{layoutCompacto ? formatarCompetencia(mesesExibidos[0].competencia) : `${formatarCompetencia(mesesExibidos[0].competencia)} — ${formatarCompetencia(mesesExibidos[mesesExibidos.length - 1].competencia)}`}</span>
          <span className="planilha__periodo-mobile">{formatarCompetencia(estado.meses[0].competencia)}</span>
        </strong>
        <button type="button" className="botao--icone" onClick={() => setMesInicial((atual) => competenciaMaisMeses(atual, 1))} aria-label="Próximos meses">
          <ChevronRight size={17} />
        </button>
        <button type="button" className="botao--secundario" onClick={() => setSimuladorAberto(true)}>
          <Wand2 size={15} /> E se eu fizer essa compra?
        </button>
      </div>

      <p className="planilha__moeda-mobile">Valores em R$</p>
      <div className="planilha__scroll">
        <table className="planilha__tabela">
          <thead>
            <tr className="planilha__linha-mes">
              {mesesExibidos.map((mes) => (
                <th key={mes.competencia} colSpan={5}>
                  {formatarCompetencia(mes.competencia)}
                  {mes.totalDeficitDisfarcado !== "0.00" && (
                    <span className="planilha__selo planilha__selo--perigo" title="Soma de compras classificadas como déficit disfarçado no mês">
                      {formatarDinheiro(mes.totalDeficitDisfarcado)} em déficit disfarçado
                    </span>
                  )}
                </th>
              ))}
            </tr>
            <tr className="planilha__linha-colunas">
              {mesesExibidos.map((mes) => (
                <Fragment key={mes.competencia}>
                  <th className="planilha__col-data">Data</th>
                  <th>Entrada</th>
                  <th>Saída</th>
                  <th>Diário</th>
                  <th>Saldo</th>
                </Fragment>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: maiorQuantidadeDeDias }, (_, linha) => (
              <tr key={linha}>
                {mesesExibidos.map((mes) => {
                  const dia = mes.dias[linha];
                  if (!dia) return <Fragment key={mes.competencia}><td colSpan={5} /></Fragment>;
                  const emEdicao = celulaEmEdicao?.competencia === mes.competencia && celulaEmEdicao.data === dia.data;
                  const emPreVisualizacaoDeArraste =
                    arraste?.competencia === mes.competencia &&
                    dataAlvoArraste !== null &&
                    dia.data > arraste.dataOrigem &&
                    dia.data <= dataAlvoArraste;
                  return (
                    <Fragment key={mes.competencia}>
                      <td className="planilha__col-data">
                        <button type="button" className="planilha__botao-dia" onClick={() => abrirObservacao(dia)}
                          title={dia.observacao ?? "Adicionar observação"}>
                          <span className="planilha__daynum">{numeroDoDia(dia.data)}</span>
                          <span className="planilha__weekday">{diaDaSemanaCurto(dia.data)}</span>
                          {dia.observacao && <span className="planilha__nota" aria-hidden="true" />}
                        </button>
                      </td>
                      <td className="planilha__valor planilha__valor--clicavel" onClick={() => abrirComposicao(dia)}>
                        {dia.entrada !== "0.00" ? <ValorDaPlanilha valor={dia.entrada} /> : "—"}
                      </td>
                      <td className="planilha__valor planilha__valor--clicavel" onClick={() => abrirComposicao(dia)}>
                        {dia.saida !== "0.00" ? <ValorDaPlanilha valor={dia.saida} /> : "—"}
                      </td>
                      <td
                        className={`planilha__diario ${emPreVisualizacaoDeArraste ? "planilha__diario--alvo" : ""}`}
                        data-diario-dia={dia.data}
                        data-diario-competencia={mes.competencia}
                      >
                        {emEdicao ? (
                          <input
                            autoFocus
                            type="text"
                            value={valorEmEdicao}
                            onChange={(evento) => setValorEmEdicao(evento.target.value)}
                            onBlur={confirmarDiario}
                            onKeyDown={(evento) => evento.key === "Enter" && confirmarDiario()}
                          />
                        ) : (
                          <span onClick={() => abrirEdicaoDiario(mes.competencia, dia)}>
                            <ValorDaPlanilha valor={dia.diario} />
                          </span>
                        )}
                        <span
                          className="planilha__alca"
                          onMouseDown={(evento) => { evento.stopPropagation(); iniciarArraste(mes.competencia, dia); }}
                        />
                      </td>
                      <td className={`planilha__saldo ${CLASSE_POR_FAIXA[dia.faixaSaldo] ?? ""}`}>
                        <ValorDaPlanilha valor={dia.saldo} />
                      </td>
                    </Fragment>
                  );
                })}
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              {mesesExibidos.map((mes) => (
                <Fragment key={mes.competencia}>
                  <td className="planilha__col-data">Total</td>
                  <td><ValorDaPlanilha valor={mes.totalEntrada} /></td>
                  <td><ValorDaPlanilha valor={mes.totalSaida} /></td>
                  <td><ValorDaPlanilha valor={mes.totalDiario} /></td>
                  <td><ValorDaPlanilha valor={mes.saldoFinal} /></td>
                </Fragment>
              ))}
            </tr>
          </tfoot>
        </table>
      </div>

      <div className="planilha__legenda">
        <span><i className="planilha__amostra planilha-saldo--vermelho" /> déficit</span>
        <span><i className="planilha__amostra planilha-saldo--laranja" /> pressionado</span>
        <span><i className="planilha__amostra planilha-saldo--amarelo" /> seguindo bem</span>
        <span><i className="planilha__amostra planilha-saldo--verde-claro" /> folga</span>
        <span><i className="planilha__amostra planilha-saldo--verde" /> ideal</span>
      </div>

      {composicao && (
        <div className="modal" role="presentation" onMouseDown={() => setComposicao(null)}>
          <section className="modal__conteudo" role="dialog" aria-modal="true" onMouseDown={(evento) => evento.stopPropagation()}>
            <div className="modal__cabecalho">
              <h2>{editorLancamento ? (editorLancamento.item ? "Editar lançamento" : "Novo lançamento") : `Composição do dia ${numeroDoDia(composicao.data)}`}</h2>
              <button className="modal__fechar" type="button" onClick={() => { setComposicao(null); setEditorLancamento(null); }} aria-label="Fechar">×</button>
            </div>
            {editorLancamento ? (
              <EditorLancamentoPlanilha
                data={composicao.data}
                item={editorLancamento.item}
                aoSalvar={lancamentoSalvo}
                aoCancelar={() => setEditorLancamento(null)}
              />
            ) : (
              <>
              <div className="planilha__composicao-cabecalho">
                <p>Entradas e saídas que formam os valores desta linha.</p>
                <button type="button" onClick={() => setEditorLancamento({})}><Plus size={15} /> Novo lançamento</button>
              </div>
              {erroComposicao && <p className="form__erro" role="alert">{erroComposicao}</p>}
              {composicao.lancamentos.length === 0 ? (
                <p className="estado-vazio">Nenhum lançamento neste dia. Você pode adicionar o primeiro acima.</p>
              ) : (
              <ul className="planilha__composicao">
                {composicao.lancamentos.map((item, indice) => (
                  <li key={item.id ?? `${item.origem}-${item.descricao}-${indice}`}>
                    <span className={`planilha__origem planilha__origem--${item.origem === "MANUAL" ? "manual" : "importado"}`}>
                      {item.origem === "MANUAL" ? "Manual" : item.origem}
                    </span>
                    <strong>{item.descricao}</strong>
                    {item.marcacaoPlanejamento === "CUSTO_FIXO" && <span className="planilha__selo">Recorrente</span>}
                    {item.marcacaoPlanejamento === "PISO_HUMANO" && <span className="planilha__selo">Piso humano</span>}
                    {item.marcacaoPlanejamento === "RECEITA_RECORRENTE" && <span className="planilha__selo">Receita recorrente</span>}
                    {item.usoDeCredito && SELO_USO_DE_CREDITO[item.usoDeCredito] && (
                      <span className={`planilha__selo ${SELO_USO_DE_CREDITO[item.usoDeCredito].classe}`}>
                        {SELO_USO_DE_CREDITO[item.usoDeCredito].texto}
                      </span>
                    )}
                    <b className={item.tipo === "SAIDA" ? "valor--despesa" : "valor--receita"}>
                      {item.tipo === "SAIDA" ? formatarDespesa(item.valor) : formatarDinheiro(item.valor)}
                    </b>
                    {item.editavel && item.id ? (
                      <span className="planilha__composicao-acoes">
                        <button type="button" className="botao--icone" onClick={() => setEditorLancamento({ item })} aria-label={`Editar ${item.descricao}`}><Pencil size={14} /></button>
                        <button type="button" className="botao--icone botao--icone-perigo" onClick={() => removerLancamento(item)} aria-label={`Remover ${item.descricao}`}><Trash2 size={14} /></button>
                      </span>
                    ) : (
                      <span className="planilha__somente-leitura" title="Itens importados são alterados na origem para preservar o histórico">Somente leitura</span>
                    )}
                  </li>
                ))}
              </ul>
              )}
              </>
            )}
          </section>
        </div>
      )}

      {observacaoAberta && (
        <div className="modal" role="presentation" onMouseDown={() => setObservacaoAberta(null)}>
          <section className="modal__conteudo" role="dialog" aria-modal="true" onMouseDown={(evento) => evento.stopPropagation()}>
            <div className="modal__cabecalho">
              <h2>Observação — dia {numeroDoDia(observacaoAberta.data)}</h2>
              <button className="modal__fechar" type="button" onClick={() => setObservacaoAberta(null)} aria-label="Fechar">×</button>
            </div>
            <textarea
              className="planilha__textarea-observacao"
              value={textoObservacao}
              onChange={(evento) => setTextoObservacao(evento.target.value)}
              placeholder="Uma nota sobre este dia..."
              autoFocus
            />
            <button type="button" onClick={salvarObservacao}>Salvar observação</button>
          </section>
        </div>
      )}

      {simuladorAberto && (
        <SimuladorDeDecisao
          de={estado.meses[0].competencia}
          ate={estado.meses[estado.meses.length - 1].competencia}
          aoFechar={() => setSimuladorAberto(false)}
          aoConfirmar={() => { setSimuladorAberto(false); carregar(mesInicial); }}
        />
      )}
    </div>
  );
}
