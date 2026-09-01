import type { EuDoPassadoResponse } from "../types";
import { formatarCompetencia, formatarDinheiro, somarDinheiro } from "../format";

/** "O que eu já assinei e ainda vou pagar" — compromissos futuros (RN-04) + dívidas ativas (RN-08). */
export function ColunaPassado({ dados }: { dados: EuDoPassadoResponse }) {
  const totalCompromissos = somarDinheiro(dados.compromissosFuturos.map((c) => c.valor));
  const totalDividas = somarDinheiro(dados.dividas.map((d) => d.valorParcela));

  return (
    <section className="coluna coluna--passado" aria-labelledby="titulo-passado">
      <h2 id="titulo-passado">Eu do Passado</h2>
      <p className="coluna__subtitulo">O que já foi decidido e ainda vai cair na conta.</p>

      <div className="cartao">
        <h3>Dívidas ativas</h3>
        {dados.dividas.length === 0 ? (
          <p className="vazio">Nenhuma dívida ativa.</p>
        ) : (
          <ul className="lista">
            {dados.dividas.map((divida) => (
              <li key={divida.id}>
                <span>{divida.nome}</span>
                <span className="lista__valor">{formatarDinheiro(divida.valorParcela)}/mês</span>
                <span className="lista__meta">
                  até {formatarCompetencia(divida.competenciaUltimaParcela)}
                </span>
              </li>
            ))}
          </ul>
        )}
        {dados.dividas.length > 0 && (
          <p className="cartao__total">Total: {formatarDinheiro(totalDividas)}/mês</p>
        )}
      </div>

      <div className="cartao">
        <h3>Compromissos futuros (próximos 12 meses)</h3>
        {dados.compromissosFuturos.length === 0 ? (
          <p className="vazio">Nenhuma parcela pendente nos próximos 12 meses.</p>
        ) : (
          <ul className="lista">
            {dados.compromissosFuturos.map((c, i) => (
              <li key={`${c.descricao}-${c.competencia}-${i}`}>
                <span>{c.descricao}</span>
                <span className="lista__valor">{formatarDinheiro(c.valor)}</span>
                <span className="lista__meta">{formatarCompetencia(c.competencia)}</span>
              </li>
            ))}
          </ul>
        )}
        {dados.compromissosFuturos.length > 0 && (
          <p className="cartao__total">Total: {formatarDinheiro(totalCompromissos)}</p>
        )}
      </div>
    </section>
  );
}
