import type { ViabilidadeResponse } from "../types";
import { formatarDinheiro, formatarPercentual } from "../format";

const LEITURA_VEREDITO: Record<ViabilidadeResponse["veredito"], string> = {
  VIAVEL: "Viável",
  VIAVEL_PARCIALMENTE: "Viável, parcialmente",
  INVIAVEL: "Inviável",
};

/**
 * Bloco de queda estrutural de renda (RN-16.1) — só aparece quando o veredito de viabilidade
 * (RN-16) não é VIAVEL. O backend sempre manda `viabilidade`; a decisão de exibir é só deste
 * componente, como combinado na spec da fatia 13.
 */
export function Banner({ viabilidade }: { viabilidade: ViabilidadeResponse }) {
  if (viabilidade.veredito === "VIAVEL") return null;

  return (
    <section className={`banner banner--${viabilidade.veredito.toLowerCase()}`}>
      <div className="banner__cabecalho">
        <span className="banner__selo">{LEITURA_VEREDITO[viabilidade.veredito]}</span>
        <p className="banner__leitura">{viabilidade.leitura}</p>
      </div>

      {viabilidade.quedaDeRenda && (
        <div className="banner__detalhe">
          <p>{viabilidade.quedaDeRenda.mensagem}</p>
          <dl className="banner__grid">
            <div>
              <dt>Renda anterior</dt>
              <dd>{formatarDinheiro(viabilidade.quedaDeRenda.rendaAnterior)}</dd>
            </div>
            <div>
              <dt>Renda atual</dt>
              <dd>{formatarDinheiro(viabilidade.quedaDeRenda.rendaAtual)}</dd>
            </div>
            <div>
              <dt>Queda</dt>
              <dd>{formatarPercentual(viabilidade.quedaDeRenda.quedaPct)}</dd>
            </div>
            <div>
              <dt>Peso do fixo, antes → agora</dt>
              <dd>
                {formatarPercentual(viabilidade.quedaDeRenda.pesoFixoAntes)} →{" "}
                {formatarPercentual(viabilidade.quedaDeRenda.pesoFixoAgora)}
              </dd>
            </div>
          </dl>
        </div>
      )}

      <dl className="banner__grid">
        <div>
          <dt>Custo mínimo de vida</dt>
          <dd>{formatarDinheiro(viabilidade.custoMinimoVida)}</dd>
        </div>
        <div>
          <dt>Economia máxima possível</dt>
          <dd>{formatarDinheiro(viabilidade.economiaMaxima)}</dd>
        </div>
        <div>
          <dt>Taxa máxima × meta</dt>
          <dd>
            {formatarPercentual(viabilidade.taxaMaxima)} × {formatarPercentual(viabilidade.metaEconomia)}
          </dd>
        </div>
        <div>
          <dt>Alvo de redução do fixo</dt>
          <dd>{formatarDinheiro(viabilidade.alvoReducaoFixo)}</dd>
        </div>
      </dl>
    </section>
  );
}
