import type { EuDoFuturoResponse, NivelDeReserva } from "../types";
import { formatarCompetencia, formatarDinheiro } from "../format";

const LEITURA_NIVEL: Record<NivelDeReserva, string> = {
  UM_MES: "1× o custo mensal",
  TRES_MESES: "3× o custo mensal",
  SEIS_MESES: "6× o custo mensal",
};

/** Quando/se quita (RN-09), reserva em níveis (RN-21) e o plano de ajuste (RN-15). */
export function ColunaFuturo({ dados }: { dados: EuDoFuturoResponse }) {
  const { marcos, reserva, planoAjuste } = dados;

  return (
    <section className="coluna coluna--futuro" aria-labelledby="titulo-futuro">
      <h2 id="titulo-futuro">Eu do Futuro</h2>
      <p className="coluna__subtitulo">Pra onde a trajetória atual está levando.</p>

      <div className="cartao">
        <h3>Marcos da quitação</h3>
        <dl className="grid-2">
          <div>
            <dt>Quitação da dívida</dt>
            <dd>{formatarCompetencia(marcos.dataQuitacao)}</dd>
          </div>
          <div>
            <dt>Primeiro real guardado</dt>
            <dd>{formatarCompetencia(marcos.primeiroRealGuardado)}</dd>
          </div>
          <div>
            <dt>Reserva completa</dt>
            <dd>{formatarCompetencia(marcos.reservaCompleta)}</dd>
          </div>
          <div>
            <dt>Juros totais até lá</dt>
            <dd>{formatarDinheiro(marcos.jurosTotaisPagos)}</dd>
          </div>
        </dl>
      </div>

      <div className="cartao">
        <h3>Reserva de emergência</h3>
        {reserva ? <>
          <p className="cartao__legenda">Custo mensal considerado: {formatarDinheiro(reserva.custoMensal)}</p>
          <ul className="lista lista--niveis">
            {reserva.niveis.map((nivel) => (
            <li key={nivel.nivel} className={nivel.atingido ? "nivel nivel--atingido" : "nivel"}>
              <span>{LEITURA_NIVEL[nivel.nivel]}</span>
              <span className="lista__valor">{formatarDinheiro(nivel.alvo)}</span>
              <span className="lista__meta">
                {nivel.atingido
                  ? `atingido em ${formatarCompetencia(nivel.competenciaPrevista)}`
                  : nivel.competenciaPrevista
                    ? `previsto para ${formatarCompetencia(nivel.competenciaPrevista)}`
                    : "fora do horizonte simulado"}
              </span>
            </li>
            ))}
          </ul>
        </> : <p className="form__aviso" role="status">
          <strong>Informação necessária.</strong>{" "}
          {dados.reservaIndisponivel ?? "Cadastre o orçamento mensal para calcular a reserva."}
        </p>}
      </div>

      <div className="cartao">
        <h3>Plano de ajuste — próximas ações</h3>
        {planoAjuste.acoesPrioritarias.length === 0 ? (
          <p className="vazio">Nenhuma ação prioritária no momento.</p>
        ) : (
          <ol className="lista lista--acoes">
            {planoAjuste.acoesPrioritarias.map((acao) => (
              <li key={`${acao.categoria}-${acao.descricao}`}>
                <span>{acao.descricao}</span>
                <span className="lista__meta">{acao.categoria}</span>
              </li>
            ))}
          </ol>
        )}
        {planoAjuste.avisos.length > 0 && (
          <ul className="avisos">
            {planoAjuste.avisos.map((aviso, i) => (
              <li key={i}>{aviso}</li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
