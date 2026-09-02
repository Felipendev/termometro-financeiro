import { useRef, useState, type MouseEvent, type ReactNode } from "react";

export interface SegmentoRosca {
  nome: string;
  inicio: number;
  fim: number;
  percentual: number;
  cor: string;
}

const RAIO_VIEWBOX = 48;

/**
 * Donut em SVG (arcos via stroke-dasharray) em vez de conic-gradient — cada fatia é um elemento
 * de verdade, então dá pra reagir ao hover por fatia: a fatia sob o cursor ganha cor mais forte e
 * uma borda; as outras perdem opacidade. A fatia sob o cursor é achada por trigonometria (ângulo
 * a partir do topo, mesmo sentido que o conic-gradient tinha) e por raio, pra ignorar o buraco
 * central e fora do círculo.
 */
export function GraficoRosca({
  segmentos,
  tamanho,
  grande,
  furoRaio,
  centro,
}: {
  segmentos: SegmentoRosca[];
  tamanho: number;
  grande?: boolean;
  furoRaio: number;
  centro: ReactNode;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hover, setHover] = useState<{ indice: number; x: number; y: number } | null>(null);

  function aoMoverMouse(evento: MouseEvent<HTMLDivElement>) {
    const caixa = containerRef.current?.getBoundingClientRect();
    if (!caixa) return;
    const raioExterno = caixa.width / 2;
    const dx = evento.clientX - (caixa.left + raioExterno);
    const dy = evento.clientY - (caixa.top + raioExterno);
    const distancia = Math.sqrt(dx * dx + dy * dy);
    const raioFuroRelativo = (furoRaio / tamanho) * caixa.width;
    if (distancia > raioExterno || distancia < raioFuroRelativo) {
      setHover(null);
      return;
    }
    const anguloDoTopo = ((Math.atan2(dx, -dy) * 180) / Math.PI + 360) % 360;
    const percentualDoPonto = (anguloDoTopo / 360) * 100;
    const indice = segmentos.findIndex((s) => percentualDoPonto >= s.inicio && percentualDoPonto < s.fim);
    if (indice === -1) {
      setHover(null);
      return;
    }
    setHover({ indice, x: evento.clientX - caixa.left, y: evento.clientY - caixa.top });
  }

  const raioFuro = RAIO_VIEWBOX * (furoRaio / tamanho) * 2;
  const raioMeio = (RAIO_VIEWBOX + raioFuro) / 2;
  const espessura = RAIO_VIEWBOX - raioFuro;
  const circunferencia = 2 * Math.PI * raioMeio;

  return (
    <div
      ref={containerRef}
      className={grande ? "grafico-rosca grafico-rosca--grande" : "grafico-rosca"}
      role="img"
      aria-label="Distribuição dos gastos por categoria"
      style={segmentos.length === 0 ? { background: "#e8ebe7" } : undefined}
      onMouseMove={aoMoverMouse}
      onMouseLeave={() => setHover(null)}
    >
      <svg className="grafico-rosca__svg" viewBox="0 0 100 100">
        <g transform="rotate(-90 50 50)">
          {hover && (
            <circle
              cx={50}
              cy={50}
              r={raioMeio}
              fill="none"
              stroke="var(--cor-texto)"
              strokeWidth={espessura + 5}
              strokeDasharray={`${((segmentos[hover.indice].fim - segmentos[hover.indice].inicio) / 100) * circunferencia} ${circunferencia}`}
              strokeDashoffset={-((segmentos[hover.indice].inicio / 100) * circunferencia)}
            />
          )}
          {segmentos.map((segmento, indice) => (
            <circle
              key={segmento.nome}
              cx={50}
              cy={50}
              r={raioMeio}
              fill="none"
              stroke={segmento.cor}
              strokeWidth={espessura}
              strokeDasharray={`${((segmento.fim - segmento.inicio) / 100) * circunferencia} ${circunferencia}`}
              strokeDashoffset={-((segmento.inicio / 100) * circunferencia)}
              opacity={!hover || hover.indice === indice ? 1 : 0.3}
              style={hover?.indice === indice ? { filter: "saturate(1.4) brightness(.92)" } : undefined}
            />
          ))}
        </g>
      </svg>
      {centro}
      {hover && (
        <span className="grafico-rosca__tooltip" style={{ left: hover.x, top: hover.y }}>
          {segmentos[hover.indice].nome} · {segmentos[hover.indice].percentual.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%
        </span>
      )}
    </div>
  );
}
