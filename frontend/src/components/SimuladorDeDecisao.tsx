import { useState } from "react";
import { ApiError, postConfirmarDecisao, postSimularDecisao, type ComandoDeDecisao } from "../api";
import { formatarCompetencia, formatarDinheiro, normalizarDecimal } from "../format";
import type { SimulacaoDecisaoResponse } from "../types";

const SELO_USO: Record<string, string> = {
  DEFICIT_DISFARCADO: "Isso pareceria déficit disfarçado, não ferramenta",
  ATENCAO: "Merece atenção — um dos dois sinais de alerta apareceu",
  FERRAMENTA: "Cabe no seu fluxo como ferramenta, não como sintoma",
};

/**
 * RN-23 — nunca toca em dado real até a confirmação explícita. `de`/`ate` é a mesma janela de
 * meses que a planilha já está mostrando, pra comparar real × simulado lado a lado.
 */
export function SimuladorDeDecisao({ de, ate, aoFechar, aoConfirmar }: {
  de: string;
  ate: string;
  aoFechar: () => void;
  aoConfirmar: () => void;
}) {
  const [data, setData] = useState(de + "-15");
  const [valor, setValor] = useState("");
  const [descricao, setDescricao] = useState("");
  const [formaPagamento, setFormaPagamento] = useState<ComandoDeDecisao["formaPagamento"]>("CREDITO_AVISTA");
  const [parcelas, setParcelas] = useState(1);
  const [resultado, setResultado] = useState<SimulacaoDecisaoResponse | null>(null);
  const [simulando, setSimulando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  function comando(): ComandoDeDecisao | null {
    const valorNormalizado = normalizarDecimal(valor);
    if (!valorNormalizado || !descricao.trim()) return null;
    return { data, valor: valorNormalizado, descricao: descricao.trim(), formaPagamento, parcelas };
  }

  async function simular() {
    const decisao = comando();
    if (!decisao) { setErro("Preencha a descrição e o valor."); return; }
    setSimulando(true);
    setErro(null);
    try {
      setResultado(await postSimularDecisao(de, ate, decisao));
    } catch (causa: unknown) {
      setErro(causa instanceof ApiError ? causa.message : "Não consegui simular essa decisão.");
    } finally {
      setSimulando(false);
    }
  }

  async function confirmar() {
    const decisao = comando();
    if (!decisao) return;
    setConfirmando(true);
    try {
      await postConfirmarDecisao(decisao);
      aoConfirmar();
    } catch (causa: unknown) {
      setErro(causa instanceof ApiError ? causa.message : "Não consegui confirmar essa decisão.");
    } finally {
      setConfirmando(false);
    }
  }

  return (
    <div className="modal" role="presentation" onMouseDown={aoFechar}>
      <section className="modal__conteudo modal__conteudo--largo" role="dialog" aria-modal="true" onMouseDown={(evento) => evento.stopPropagation()}>
        <div className="modal__cabecalho">
          <h2>E se eu fizer essa compra?</h2>
          <button className="modal__fechar" type="button" onClick={aoFechar} aria-label="Fechar">×</button>
        </div>

        {!resultado && (
          <div className="form">
            <div className="form__campo"><label htmlFor="sim-descricao">Descrição</label><input id="sim-descricao" value={descricao} onChange={(e) => setDescricao(e.target.value)} /></div>
            <div className="modal__linha">
              <div className="form__campo"><label htmlFor="sim-valor">Valor</label><input id="sim-valor" placeholder="0,00" value={valor} onChange={(e) => setValor(e.target.value)} /></div>
              <div className="form__campo"><label htmlFor="sim-data">Data</label><input id="sim-data" type="date" value={data} onChange={(e) => setData(e.target.value)} /></div>
            </div>
            <div className="modal__linha">
              <div className="form__campo">
                <label htmlFor="sim-forma">Forma de pagamento</label>
                <select id="sim-forma" value={formaPagamento} onChange={(e) => setFormaPagamento(e.target.value as ComandoDeDecisao["formaPagamento"])}>
                  <option value="DEBITO">Débito</option>
                  <option value="CREDITO_AVISTA">Crédito à vista</option>
                  <option value="CREDITO_PARCELADO">Crédito parcelado</option>
                </select>
              </div>
              {formaPagamento === "CREDITO_PARCELADO" && (
                <div className="form__campo"><label htmlFor="sim-parcelas">Parcelas</label><input id="sim-parcelas" type="number" min={2} value={parcelas} onChange={(e) => setParcelas(Number(e.target.value))} /></div>
              )}
            </div>
            {erro && <p className="form__erro">{erro}</p>}
            <button type="button" onClick={simular} disabled={simulando}>{simulando ? "Simulando..." : "Simular"}</button>
          </div>
        )}

        {resultado && (
          <div className="importacao__proposta">
            {resultado.usoDeCreditoPrevisto && (
              <p className={resultado.usoDeCreditoPrevisto === "FERRAMENTA" ? "" : "form__aviso"}>
                {SELO_USO[resultado.usoDeCreditoPrevisto] ?? resultado.usoDeCreditoPrevisto}
              </p>
            )}

            <ul className="planilha__composicao">
              {resultado.cenarioSimulado.map((mesSimulado, indice) => {
                const mesReal = resultado.cenarioReal[indice];
                return (
                  <li key={mesSimulado.competencia}>
                    <strong>{formatarCompetencia(mesSimulado.competencia)}</strong>
                    <span>Real: {formatarDinheiro(mesReal.saldoFinal)} → Simulado: {formatarDinheiro(mesSimulado.saldoFinal)}</span>
                  </li>
                );
              })}
            </ul>

            {resultado.priorizacaoSeDeficit && (
              <div className="form__aviso">
                <strong>Algum mês fecharia no vermelho.</strong> Ações de maior impacto:
                <ul>
                  {resultado.priorizacaoSeDeficit.acoesPrioritarias.map((acao) => (
                    <li key={acao.categoria}>{acao.descricao}</li>
                  ))}
                </ul>
              </div>
            )}

            {erro && <p className="form__erro">{erro}</p>}
            <div className="importacao__acoes">
              <button type="button" onClick={confirmar} disabled={confirmando}>{confirmando ? "Confirmando..." : "Confirmar decisão"}</button>
              <button type="button" className="botao--secundario" onClick={() => setResultado(null)} disabled={confirmando}>Simular outra coisa</button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
