import { useEffect, useRef, useState } from "react";
import { buscaComparativoCategorias } from "../api";
import { formatarDinheiro } from "../format";
import type { DinheiroStr, PontoComparativoResponse } from "../types";

const NOME_DO_GRUPO: Record<string, string> = {
  MORADIA: "Moradia",
  ALIMENTACAO: "Alimentação",
  TRANSPORTE: "Transporte",
  SAUDE: "Saúde",
  LAZER: "Lazer",
  LAVANDERIA: "Lavanderia",
  ASSINATURAS: "Assinaturas",
  IMPOSTOS: "Impostos",
  SERVICOS: "Serviços",
  OUTROS: "Outros",
};

function paraPercentual(fracao: string) {
  return Number(fracao) * 100;
}

function formatarPercentual(fracao: string) {
  return `${paraPercentual(fracao).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`;
}

function formatarComoReal(fracao: number, rendaLiquida: string) {
  const valor = fracao * Number(rendaLiquida);
  return formatarDinheiro(valor.toFixed(2) as DinheiroStr);
}

function Marcador({
  tipo,
  valor,
  escala,
  grupo,
  rendaReferencia,
  aoEntrar,
  aoSair,
}: {
  tipo: "atual" | "bom" | "ideal" | "ruim";
  valor: string;
  escala: number;
  grupo: string;
  rendaReferencia: DinheiroStr;
  aoEntrar: (elemento: HTMLElement, fracao: number) => void;
  aoSair: () => void;
}) {
  const nome = tipo === "atual" ? "Atual" : tipo === "bom" ? "Bom" : tipo === "ideal" ? "Ideal" : "Ruim";
  const descricao = `${nome} de ${grupo}: ${formatarPercentual(valor)} · ${formatarComoReal(Number(valor), rendaReferencia)}`;
  return (
    <span
      className={`ponto ponto--${tipo} grafico-comparativo__ponto`}
      style={{ left: `${(paraPercentual(valor) / escala) * 100}%` }}
      tabIndex={0}
      aria-label={descricao}
      data-tooltip={descricao}
      onMouseEnter={(evento) => aoEntrar(evento.currentTarget, Number(valor))}
      onFocus={(evento) => aoEntrar(evento.currentTarget, Number(valor))}
      onMouseLeave={aoSair}
      onBlur={aoSair}
    />
  );
}

/**
 * RN-30 — atual sempre aparece; bom/ideal/ruim só para os grupos calibrados com o Felipe
 * (`ReferenciaDoGrupo`, backend). Nenhum ponto de meta é fabricado aqui no front.
 *
 * <p>No hover/foco de qualquer ponto, uma linha pontilhada desce até a régua mostrando o valor
 * real em reais (não só o percentual) — pedido do Felipe depois de ver o protótipo.
 */
export function GraficoComparativo({ competencia, rendaLiquida }: { competencia: string; rendaLiquida: DinheiroStr }) {
  const [pontos, setPontos] = useState<PontoComparativoResponse[] | null>(null);
  const [indisponivel, setIndisponivel] = useState(false);
  const [guia, setGuia] = useState<{ esquerda: number; valor: string } | null>(null);
  const [grupoAberto, setGrupoAberto] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const rendaReferencia = pontos?.[0]?.rendaReferencia ?? rendaLiquida;

  useEffect(() => {
    let ativo = true;
    setPontos(null);
    setIndisponivel(false);
    buscaComparativoCategorias(competencia)
      .then((resultado) => { if (ativo) { setPontos(resultado); setGrupoAberto(null); } })
      .catch(() => { if (ativo) setIndisponivel(true); });
    return () => { ativo = false; };
  }, [competencia]);

  function mostrarGuia(elemento: HTMLElement, fracao: number) {
    if (!containerRef.current) return;
    const containerRect = containerRef.current.getBoundingClientRect();
    const pontoRect = elemento.getBoundingClientRect();
    const esquerda = pontoRect.left + pontoRect.width / 2 - containerRect.left;
    setGuia({ esquerda, valor: formatarComoReal(fracao, rendaReferencia) });
  }
  function esconderGuia() {
    setGuia(null);
  }

  if (indisponivel || (pontos && pontos.length === 0)) return null;
  if (!pontos) return null;

  const maiorValor = Math.max(
    ...pontos.flatMap((ponto) => [
      paraPercentual(ponto.atual),
      ponto.bom ? paraPercentual(ponto.bom) : 0,
      ponto.ideal ? paraPercentual(ponto.ideal) : 0,
      ponto.ruim ? paraPercentual(ponto.ruim) : 0,
    ]),
    5,
  );
  const escala = Math.max(Math.ceil((maiorValor * 1.15) / 5) * 5, 5);
  const ordenados = [...pontos].sort((a, b) => paraPercentual(b.atual) - paraPercentual(a.atual));

  return (
    <article className="painel grafico-comparativo">
      <div className="painel__cabecalho">
        <div>
          <p className="eyebrow">Comparativo</p>
          <h2>Seus gastos hoje e as referências</h2>
          <p className="grafico-comparativo__ajuda">Ruim marca o ponto 25% acima do limite bom. Passe o ponteiro pelas bolinhas e clique na categoria para ver tudo que compõe o valor.</p>
        </div>
      </div>
      <div className="grafico-comparativo__legenda">
        <span><i className="ponto ponto--atual" aria-hidden="true" />Atual</span>
        <span><i className="ponto ponto--bom" aria-hidden="true" />Bom</span>
        <span><i className="ponto ponto--ideal" aria-hidden="true" />Ideal</span>
        <span><i className="ponto ponto--ruim" aria-hidden="true" />Ruim</span>
      </div>
      <div className="grafico-comparativo__linhas grafico-comparativo__linhas--com-guia" ref={containerRef}>
        {guia && (
          <>
            <div className="grafico-comparativo__linha-guia" style={{ left: guia.esquerda }} aria-hidden="true" />
            <div className="grafico-comparativo__balao-guia" style={{ left: guia.esquerda }}>{guia.valor}</div>
          </>
        )}
        {ordenados.map((ponto) => {
          const grupo = NOME_DO_GRUPO[ponto.grupo] ?? ponto.grupo;
          return (
          <div className="grafico-comparativo__grupo" key={ponto.grupo}>
          <div className="grafico-comparativo__linha">
            <button type="button" className="grafico-comparativo__rotulo" aria-expanded={grupoAberto === ponto.grupo} onClick={() => setGrupoAberto((atual) => atual === ponto.grupo ? null : ponto.grupo)}>{grupo}</button>
            <div className="grafico-comparativo__trilho">
              {ponto.ideal && <Marcador tipo="ideal" valor={ponto.ideal} escala={escala} grupo={grupo} rendaReferencia={rendaReferencia} aoEntrar={mostrarGuia} aoSair={esconderGuia} />}
              {ponto.bom && <Marcador tipo="bom" valor={ponto.bom} escala={escala} grupo={grupo} rendaReferencia={rendaReferencia} aoEntrar={mostrarGuia} aoSair={esconderGuia} />}
              {ponto.ruim && <Marcador tipo="ruim" valor={ponto.ruim} escala={escala} grupo={grupo} rendaReferencia={rendaReferencia} aoEntrar={mostrarGuia} aoSair={esconderGuia} />}
              <Marcador tipo="atual" valor={ponto.atual} escala={escala} grupo={grupo} rendaReferencia={rendaReferencia} aoEntrar={mostrarGuia} aoSair={esconderGuia} />
            </div>
            <button type="button" className="grafico-comparativo__valor" aria-label={`Ver itens de ${grupo}`} onClick={() => setGrupoAberto((atual) => atual === ponto.grupo ? null : ponto.grupo)}>{formatarPercentual(ponto.atual)}</button>
          </div>
          {grupoAberto === ponto.grupo && (
            <div className="grafico-comparativo__detalhe">
              <div><strong>{formatarDinheiro(ponto.valorAtual)}</strong><span>{ponto.fonte === "LANCAMENTOS_DO_MES" ? "Lançamentos reais deste mês" : "Catálogo usado como fallback"}</span></div>
              <ul>{ponto.itens.map((item, indice) => <li key={`${item.descricao}-${indice}`}><span><strong>{item.descricao}</strong><small>{item.categoria} · {item.origem}</small></span><b>{formatarDinheiro(item.valor)}</b></li>)}</ul>
            </div>
          )}
          </div>
        );})}
        <div className="grafico-comparativo__eixo" aria-label={`Escala do gráfico: zero a ${formatarComoReal(escala / 100, rendaReferencia)}`}>
          <span aria-hidden="true" />
          <div className="grafico-comparativo__eixo-valores">
            {[0, 0.25, 0.5, 0.75, 1].map((parte) => (
              <span key={parte} style={{ left: `${parte * 100}%` }}>{formatarComoReal((escala * parte) / 100, rendaReferencia)}</span>
            ))}
          </div>
          <span aria-hidden="true" />
        </div>
      </div>
    </article>
  );
}
