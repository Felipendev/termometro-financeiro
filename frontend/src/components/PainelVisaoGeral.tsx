import { useEffect, useState } from "react";
import type { CSSProperties } from "react";
import { ArrowLeftRight, ArrowRight, CreditCard, HeartHandshake, Minus, Plus, RefreshCw, Repeat2, Sparkles, Upload } from "lucide-react";
import { ApiError, buscaPlanilha } from "../api";
import { formatarDespesa, formatarDinheiro, somarDinheiro } from "../format";
import type { DashboardResponse, LancamentoPlanejadoResponse, PlanilhaMesResponse } from "../types";
import { combinaCategorias } from "../resumoCategorias";
import { IconeCategoria } from "./IconeCategoria";
import { GraficoComparativo } from "./GraficoComparativo";

const CORES = ["#748af1", "#f19479", "#4fa879", "#d4ad52"];

function paraNumero(valor: string) {
  return Number(valor);
}

/**
 * O saldo real do mês (RN-24.1, planilha viva) — não o diagnóstico estrutural declarado no
 * catálogo. Se o saldo inicial da planilha ainda não foi configurado, pergunta em vez de quebrar
 * a Visão Geral inteira (a Visão Geral precisa carregar independente de qualquer coisa).
 */
function ResumoMesReal({ competencia }: { competencia: string }) {
  const [mes, setMes] = useState<PlanilhaMesResponse | null>(null);
  const [precisaConfigurar, setPrecisaConfigurar] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    setMes(null);
    setPrecisaConfigurar(false);
    setErro(null);
    buscaPlanilha(competencia)
      .then((resultado) => { if (ativo) setMes(resultado); })
      .catch((causa: unknown) => {
        if (!ativo) return;
        if (causa instanceof ApiError && causa.status === 400) {
          setPrecisaConfigurar(true);
        } else {
          setErro("Não consegui calcular o saldo real do mês.");
        }
      });
    return () => { ativo = false; };
  }, [competencia]);

  if (precisaConfigurar) {
    return (
      <div className="resumo-mes__texto">
        <p className="eyebrow">Mês atual</p>
        <h2 className="resumo-mes__saldo-pendente">Saldo indisponível</h2>
        <p>Defina o saldo inicial na Planilha pra ver o saldo real do mês aqui.</p>
      </div>
    );
  }
  if (erro) {
    return <div className="resumo-mes__texto"><p className="eyebrow">Mês atual</p><p className="form__erro">{erro}</p></div>;
  }
  if (!mes) {
    return <div className="resumo-mes__texto"><p className="eyebrow">Mês atual</p><p className="vazio">Calculando...</p></div>;
  }

  const saldoNegativo = Number(mes.saldoFinal) < 0;
  return (
    <>
      <div className="resumo-mes__texto">
        <p className="eyebrow">Mês atual</p>
        <h2 className={saldoNegativo ? "valor--despesa" : "valor--receita"}>{formatarDinheiro(mes.saldoFinal)}</h2>
        <p>saldo real do mês, com o que já entrou e saiu</p>
      </div>
      <dl className="resumo-mes__numeros">
        <div><dt>Entradas</dt><dd>{formatarDinheiro(mes.totalEntrada)}</dd></div>
        <div><dt>Saídas</dt><dd>{formatarDespesa(mes.totalSaida)}</dd></div>
        <div><dt>Diário do mês</dt><dd>{formatarDinheiro(mes.totalDiario)}</dd></div>
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
  rodando: "nao-gasto" | "triagem" | null;
}) {
  const categorias = combinaCategorias(dashboard.euDoPresente.resumoTriagem, pendencias, dashboard.competencia);
  const gastoTotal = somarDinheiro(categorias.map(({ total }) => total));
  const maiorGastos = categorias.slice(0, 4);
  const totalParaGrafico = Math.max(paraNumero(gastoTotal), 1);
  const fatutasManuais = dashboard.euDoPresente.cartoesManuais;
  const totalFaturasManuais = somarDinheiro(fatutasManuais.map((cartao) => cartao.valorFatura));
  const totalExibidoCartoes = dashboard.euDoPresente.cartoes.cartoes.length > 0
    ? dashboard.euDoPresente.cartoes.totalGastoEmCartoes
    : totalFaturasManuais;

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
            <span className="valor--despesa">{formatarDespesa(totalExibidoCartoes)}</span>
          </div>
          {dashboard.euDoPresente.cartoes.cartoes.length === 0 && fatutasManuais.length === 0 ? (
            <p className="vazio">Nenhuma fatura ou cartão para esta competência.</p>
          ) : (
            <ul className="lista-cartoes-home">
              {dashboard.euDoPresente.cartoes.cartoes.map((cartao) => (
                <li key={cartao.identificador}>
                  <span className="cartao-marca"><CreditCard size={16} /></span>
                  <div><strong>{cartao.nome}</strong><small>Gasto no período</small></div>
                  <b className="valor--despesa">{formatarDespesa(cartao.gastoNoMes)}</b>
                </li>
              ))}
              {fatutasManuais.map((cartao) => (
                <li key={cartao.id}>
                  <span className="cartao-marca cartao-marca--manual"><CreditCard size={16} /></span>
                  <div><strong>{cartao.nome}</strong><small>Fatura declarada</small></div>
                  <b className="valor--despesa">{formatarDespesa(cartao.valorFatura)}</b>
                </li>
              ))}
            </ul>
          )}
          <button type="button" className="link-painel" onClick={onVerCartoes}>Ver relatório de cartões <ArrowRight size={15} /></button>
        </article>

        <article className="painel painel--limite">
          <p className="eyebrow">O que sustenta seu mês</p>
          <h2>Compromissos essenciais</h2>
          <dl className="totais-sustentacao">
            <div><dt><Repeat2 size={15} /> Custo fixo</dt><dd>{formatarDinheiro(dashboard.viabilidade.custoFixoTotal)}</dd></div>
            <div><dt><HeartHandshake size={15} /> Piso humano</dt><dd>{formatarDinheiro(dashboard.viabilidade.pisoVariavelTotal)}</dd></div>
          </dl>
          <span className="painel--limite__acao">Definidos diretamente nos lançamentos</span>
        </article>
      </section>

      <GraficoComparativo competencia={dashboard.competencia} rendaLiquida={dashboard.euDoPresente.diagnostico.rendaLiquida} />
    </>
  );
}
