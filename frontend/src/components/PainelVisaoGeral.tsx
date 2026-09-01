import { useEffect, useState } from "react";
import type { CSSProperties } from "react";
import { ArrowLeftRight, ArrowRight, HeartHandshake, Minus, Plus, RefreshCw, Repeat2, Sparkles, Upload } from "lucide-react";
import { buscaFaturasCartao, buscaTodosLancamentos } from "../api";
import { formatarDespesa, formatarDinheiro, somarDinheiro } from "../format";
import type { DashboardResponse, FaturaCartaoResponse, LancamentoPlanejadoResponse } from "../types";
import { combinaCategorias } from "../resumoCategorias";
import { IconeCategoria } from "./IconeCategoria";
import { GraficoComparativo } from "./GraficoComparativo";
import { LogoCartao } from "./LogoCartao";

const CORES = ["#748af1", "#f19479", "#4fa879", "#d4ad52"];

function paraNumero(valor: string) {
  return Number(valor);
}

/**
 * Fluxo real do mês: receitas menos despesas lançadas/importadas. Não usa o saldo final da
 * Planilha, pois aquele número também carrega saldo inicial e orçamento diário em cascata.
 */
function ResumoMesReal({ competencia }: { competencia: string }) {
  const [mes, setMes] = useState<{ receitas: string; despesas: string; saldo: string; realizado: string } | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    setMes(null);
    setErro(null);
    buscaTodosLancamentos(competencia)
      .then((movimentos) => {
        if (!ativo) return;
        const ativos = movimentos.filter((item) => item.tipo !== "TRANSFERENCIA" && item.status !== "CANCELADO");
        const receitas = somarDinheiro(ativos.filter((item) => item.tipo === "RECEITA").map((item) => item.valor));
        const despesasAtivas = ativos.filter((item) => item.tipo === "DESPESA" && item.categoria?.natureza !== "NAO_E_GASTO");
        const despesas = somarDinheiro(despesasAtivas.map((item) => item.valor));
        const realizados = ativos.filter((item) => item.status === "LIQUIDADO");
        const receitasRealizadas = somarDinheiro(realizados.filter((item) => item.tipo === "RECEITA").map((item) => item.valor));
        const despesasRealizadas = somarDinheiro(realizados.filter((item) => item.tipo === "DESPESA" && item.categoria?.natureza !== "NAO_E_GASTO").map((item) => item.valor));
        setMes({ receitas, despesas, saldo: somarDinheiro([receitas, `-${despesas}`]), realizado: somarDinheiro([receitasRealizadas, `-${despesasRealizadas}`]) });
      })
      .catch(() => {
        if (!ativo) return;
        setErro("Não consegui calcular o saldo real do mês.");
      });
    return () => { ativo = false; };
  }, [competencia]);

  if (erro) {
    return <div className="resumo-mes__texto"><p className="eyebrow">Mês atual</p><p className="form__erro">{erro}</p></div>;
  }
  if (!mes) {
    return <div className="resumo-mes__texto"><p className="eyebrow">Mês atual</p><p className="vazio">Calculando...</p></div>;
  }

  const saldoNegativo = Number(mes.saldo) < 0;
  return (
    <>
      <div className="resumo-mes__texto">
        <p className="eyebrow">Mês atual</p>
        <h2 className={saldoNegativo ? "valor--despesa" : "valor--receita"}>{formatarDinheiro(mes.saldo)}</h2>
        <p>receitas menos despesas lançadas neste mês</p>
      </div>
      <dl className="resumo-mes__numeros">
        <div><dt>Entradas</dt><dd>{formatarDinheiro(mes.receitas)}</dd></div>
        <div><dt>Despesas</dt><dd>{formatarDespesa(mes.despesas)}</dd></div>
        <div><dt>Realizado</dt><dd className={Number(mes.realizado) < 0 ? "valor--despesa" : "valor--receita"}>{formatarDinheiro(mes.realizado)}</dd></div>
      </dl>
    </>
  );
}

/**
 * A home não tenta reproduzir os cadastros. Ela responde primeiro "quanto saiu", "onde saiu" e
 * "em qual cartão caiu". A classificação continua sendo a fonte de verdade para os totais;
 * cartões mostram apenas a forma de pagamento, para que não aconteça dupla contagem.
 */
export function PainelVisaoGeral({
  dashboard,
  pendencias,
  onRodarNaoGasto,
  onRodarTriagem,
  onImportar,
  onNovoLancamento,
  onVerRelatorios,
  onVerCartoes,
  onVerFatura,
  onVerSustentacao,
  rodando,
}: {
  dashboard: DashboardResponse;
  pendencias: LancamentoPlanejadoResponse[];
  onRodarNaoGasto: () => void;
  onRodarTriagem: () => void;
  onImportar: () => void;
  onNovoLancamento: (tipo: "DESPESA" | "RECEITA" | "TRANSFERENCIA") => void;
  onVerRelatorios: () => void;
  onVerCartoes: () => void;
  onVerFatura: (nome: string) => void;
  onVerSustentacao: (marcacao: "CUSTO_FIXO" | "PISO_HUMANO") => void;
  rodando: "nao-gasto" | "triagem" | null;
}) {
  const categorias = combinaCategorias(dashboard.euDoPresente.resumoTriagem, pendencias, dashboard.competencia);
  const [faturas, setFaturas] = useState<FaturaCartaoResponse[] | null>(null);
  const gastoTotal = somarDinheiro(categorias.map(({ total }) => total));
  const maiorGastos = categorias.slice(0, 4);
  const totalParaGrafico = Math.max(paraNumero(gastoTotal), 1);
  const totalExibidoCartoes = somarDinheiro((faturas ?? []).map((fatura) => fatura.saldoAberto));

  useEffect(() => {
    let ativo = true;
    setFaturas(null);
    buscaFaturasCartao(dashboard.competencia)
      .then((resultado) => { if (ativo) setFaturas(resultado); })
      .catch(() => { if (ativo) setFaturas([]); });
    return () => { ativo = false; };
  }, [dashboard.competencia]);

  const fatias = maiorGastos.reduce<{ itens: string[]; acumulado: number }>((resultado, { total }, indice) => {
    const inicio = resultado.acumulado;
    const percentual = (paraNumero(total) / totalParaGrafico) * 100;
    return {
      itens: [...resultado.itens, `${CORES[indice]} ${inicio.toFixed(2)}% ${(inicio + percentual).toFixed(2)}%`],
      acumulado: inicio + percentual,
    };
  }, { itens: [], acumulado: 0 }).itens;
  const graficoStyle = {
    "--fatias": fatias.length > 0 ? fatias.join(", ") : "#e8ebe7 0 100%",
  } as CSSProperties;

  return (
    <>
      <section className="atalhos-rapidos" aria-label="Acesso rápido">
        <div><p className="eyebrow">Acesso rápido</p><h2>O que você quer registrar?</h2></div>
        <div className="atalhos-rapidos__acoes">
          <button type="button" onClick={() => onNovoLancamento("DESPESA")}><Minus size={18} /> <span>Despesa</span></button>
          <button type="button" onClick={() => onNovoLancamento("RECEITA")}><Plus size={18} /> <span>Receita</span></button>
          <button type="button" onClick={() => onNovoLancamento("TRANSFERENCIA")}><ArrowLeftRight size={18} /> <span>Transferir</span></button>
          <button type="button" onClick={onImportar}><Upload size={18} /> <span>Importar</span></button>
        </div>
      </section>
      <section className="resumo-mes" aria-label="Resumo do mês">
        <ResumoMesReal competencia={dashboard.competencia} />
        <div className="resumo-mes__acoes">
          <button type="button" className="botao--secundario" disabled={rodando !== null} onClick={onRodarNaoGasto}>
            <RefreshCw size={16} /> {rodando === "nao-gasto" ? "Conciliando…" : "Conciliar"}
          </button>
          <button type="button" disabled={rodando !== null} onClick={onRodarTriagem}>
            <Sparkles size={16} /> {rodando === "triagem" ? "Atualizando…" : "Atualizar análise"}
          </button>
        </div>
      </section>

      <section className="visao-grid" aria-label="Gastos, cartões e categorias">
        <article className="painel painel--categorias">
          <div className="painel__cabecalho">
            <div>
              <p className="eyebrow">Categorias</p>
              <h2>Maiores gastos do mês</h2>
            </div>
            <span className="valor--despesa">{formatarDespesa(gastoTotal)}</span>
          </div>
          {maiorGastos.length === 0 ? (
            <p className="vazio">Atualize a análise para ver seus gastos por categoria.</p>
          ) : (
            <div className="categorias-grafico">
              <ul className="legenda-categorias">
                {maiorGastos.map(({ nome, total }, indice) => (
                  <li key={nome}>
                    <IconeCategoria nome={nome} cor={CORES[indice]} tamanho={16} />
                    <span>{nome}</span>
                    <strong className="valor--despesa">{formatarDespesa(total)}</strong>
                  </li>
                ))}
              </ul>
              <div className="grafico-rosca" role="img" style={graficoStyle} aria-label="Distribuição dos gastos por categoria">
                <span>{maiorGastos.length}</span>
                <small>categorias</small>
              </div>
            </div>
          )}
          <button type="button" className="link-painel" onClick={onVerRelatorios}>Ver relatório completo <ArrowRight size={15} /></button>
        </article>

        <article className="painel painel--cartoes">
          <div className="painel__cabecalho">
            <div>
              <p className="eyebrow">Forma de pagamento</p>
              <h2>Cartões e faturas</h2>
            </div>
            <span className={Number(totalExibidoCartoes) > 0 ? "valor--despesa" : ""}>{faturas === null ? "—" : Number(totalExibidoCartoes) > 0 ? formatarDespesa(totalExibidoCartoes) : formatarDinheiro(totalExibidoCartoes)}</span>
          </div>
          {faturas === null ? (
            <p className="vazio">Carregando faturas...</p>
          ) : faturas.length === 0 ? (
            <p className="vazio">Nenhuma fatura ou cartão para esta competência.</p>
          ) : (
            <ul className="lista-cartoes-home">
              {faturas.map((fatura) => (
                <li key={fatura.referencia}><button type="button" className="cartao-home__acao" onClick={() => onVerFatura(fatura.nome)}>
                  <span className="cartao-marca cartao-marca--logo"><LogoCartao nome={fatura.nome} /></span>
                  <div><strong>{fatura.nome}</strong><small>{fatura.status === "PAGA" ? "Fatura paga" : fatura.status === "PARCIAL" ? "Pagamento parcial" : fatura.origem === "IMPORTACAO" ? "Calculada pelos imports" : "Fatura declarada"}</small></div>
                  <b className={Number(fatura.saldoAberto) > 0 ? "valor--despesa" : "valor--receita"}>{Number(fatura.saldoAberto) > 0 ? formatarDespesa(fatura.saldoAberto) : formatarDinheiro(fatura.saldoAberto)}</b>
                  <ArrowRight size={15} aria-hidden="true" /></button></li>
              ))}
            </ul>
          )}
          <button type="button" className="link-painel" onClick={onVerCartoes}>Ver relatório de cartões <ArrowRight size={15} /></button>
        </article>

        <article className="painel painel--limite">
          <p className="eyebrow">O que sustenta seu mês</p>
          <h2>Compromissos essenciais</h2>
          <dl className="totais-sustentacao">
            <button type="button" onClick={() => onVerSustentacao("CUSTO_FIXO")}><dt><Repeat2 size={15} /> Custo fixo</dt><dd>{formatarDinheiro(dashboard.viabilidade.custoFixoTotal)}</dd><ArrowRight size={15} aria-hidden="true" /></button>
            <button type="button" onClick={() => onVerSustentacao("PISO_HUMANO")}><dt><HeartHandshake size={15} /> Piso humano</dt><dd>{formatarDinheiro(dashboard.viabilidade.pisoVariavelTotal)}</dd><ArrowRight size={15} aria-hidden="true" /></button>
          </dl>
          <span className="painel--limite__acao">Definidos diretamente nos lançamentos</span>
        </article>
      </section>

      <GraficoComparativo competencia={dashboard.competencia} rendaLiquida={dashboard.euDoPresente.diagnostico.rendaLiquida} />
    </>
  );
}
