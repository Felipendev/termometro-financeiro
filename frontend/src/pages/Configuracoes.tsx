import { useState } from "react";
import { competenciaAtual, formatarCompetencia } from "../format";
import { RendaSection } from "../settings/RendaSection";
import { VerbaMensalSection } from "../settings/VerbaMensalSection";
import { CustoFixoSection } from "../settings/CustoFixoSection";
import { PisoHumanoSection } from "../settings/PisoHumanoSection";
import { DividasSection } from "../settings/DividasSection";
import { ContribuicaoSection } from "../settings/ContribuicaoSection";

/**
 * Cadastro manual do catálogo (renda, custo fixo, piso humano, dívidas) + verba variável do
 * orçamento (módulo separado, RN-20 — mas mora aqui porque sem ela o dashboard não carrega) +
 * cadastro manual de cartão (módulo `cartao`) — fluxo tipo Organizze, pra não depender de mim
 * rodando SQL a cada mudança. Renda, Verba e Dívidas são escopadas por competência (própria API);
 * Custo Fixo, Piso Humano, Dívidas Rotativas e Cartões não.
 */
export function Planejamento() {
  const [competencia, setCompetencia] = useState(competenciaAtual());

  return (
    <div className="area-cadastro">
      <div className="area-cadastro__cabecalho">
        <div>
          <p className="eyebrow">Planejamento</p>
          <h2>Seu plano para {formatarCompetencia(competencia)}</h2>
          <p>Defina as premissas que deixam o diagnóstico e as projeções honestos.</p>
        </div>
        <div className="app__controles">
          <label htmlFor="config-competencia-input">Competência do plano</label>
          <input
            id="config-competencia-input"
            type="month"
            value={competencia}
            onChange={(evento) => setCompetencia(evento.target.value)}
          />
        </div>
      </div>

      <div className="area-cadastro__grupo">
        <div>
          <p className="eyebrow">Mês atual</p>
          <h3>Entradas e limites</h3>
        </div>
        <div className="config__grid">
        <RendaSection competencia={competencia} />
        <VerbaMensalSection competencia={competencia} />
        </div>
      </div>

      <div className="area-cadastro__grupo">
        <div>
          <p className="eyebrow">Estrutura</p>
          <h3>O que sustenta seu mês</h3>
        </div>
        <div className="config__grid">
        <DividasSection competencia={competencia} />
        <CustoFixoSection />
        <PisoHumanoSection />
        </div>
      </div>

      <div className="area-cadastro__grupo">
        <div>
          <p className="eyebrow">Objetivos</p>
          <h3>Contribuição progressiva</h3>
        </div>
        <div className="config__grid config__grid--duas-colunas">
        <ContribuicaoSection competencia={competencia} />
        </div>
      </div>
    </div>
  );
}
