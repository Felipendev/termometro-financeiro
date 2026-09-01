import { useEffect, useState } from "react";
import { ApiError } from "../api";
import { formatarDinheiro, formatarPercentual } from "../format";
import { getMetasContribuicao, postAutorizarProximoPasso } from "./api";
import type { MetaContribuicaoResponse } from "../types";

const NOME_EXIBIDO: Record<string, string> = { DIZIMO: "Dízimo", OFERTA: "Ofertas" };

/**
 * RN-28 — o sistema só sugere o próximo passo quando a projeção do mês seguinte mostra espaço de
 * verdade (RN-28.1); autorizar é sempre uma ação explícita, nunca automática.
 */
export function ContribuicaoSection({ competencia }: { competencia: string }) {
  const [metas, setMetas] = useState<MetaContribuicaoResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [autorizando, setAutorizando] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  function carregar() {
    setCarregando(true);
    setErro(null);
    getMetasContribuicao(competencia)
      .then(setMetas)
      .catch((causa: unknown) => setErro(causa instanceof ApiError ? causa.message : "Não consegui carregar as metas."))
      .finally(() => setCarregando(false));
  }

  useEffect(carregar, [competencia]);

  async function autorizar(nome: string) {
    setAutorizando(nome);
    setErro(null);
    try {
      await postAutorizarProximoPasso(nome, competencia);
      carregar();
    } catch (causa: unknown) {
      setErro(causa instanceof ApiError ? causa.message : "Não consegui autorizar esse passo.");
    } finally {
      setAutorizando(null);
    }
  }

  if (carregando) return <section className="cartao"><p className="eyebrow">Dízimo e ofertas</p><p className="vazio">Carregando...</p></section>;

  return (
    <section className="cartao">
      <p className="eyebrow">Dízimo e ofertas</p>
      <h3>Contribuição progressiva</h3>
      <p className="cartao__legenda">O sistema propõe o próximo aumento só quando sobra espaço de verdade no mês seguinte.</p>
      {erro && <p className="form__erro">{erro}</p>}
      <ul className="lista">
        {metas.map((meta) => (
          <li key={meta.nome}>
            <div className="lista--cartoes__cabecalho">
              <strong>{NOME_EXIBIDO[meta.nome] ?? meta.nome}</strong>
              <span className="lista__valor">{formatarPercentual(meta.percentualAtual)} de {formatarPercentual(meta.percentualAlvo)}</span>
            </div>
            {meta.valorMensalAtual != null && (
              <div className="lista--cartoes__detalhe">
                <span>{formatarDinheiro(meta.valorMensalAtual)}/mês na projeção</span>
              </div>
            )}
            {meta.proximoPassoSugerido ? (
              <div className="form__aviso" style={{ marginTop: 8 }}>
                Espaço encontrado: subir para {formatarPercentual(meta.proximoPassoSugerido.percentualProposto)}
                {" "}({formatarDinheiro(meta.proximoPassoSugerido.valorProposto)}/mês) a partir de{" "}
                {meta.proximoPassoSugerido.competencia}.
                <div style={{ marginTop: 8 }}>
                  <button type="button" onClick={() => autorizar(meta.nome)} disabled={autorizando === meta.nome}>
                    {autorizando === meta.nome ? "Autorizando..." : "Autorizar este passo"}
                  </button>
                </div>
              </div>
            ) : meta.informacaoNecessaria ? (
              <p className="form__aviso" role="status">{meta.informacaoNecessaria}</p>
            ) : (
              <span className="lista__meta">Sem espaço projetado ainda para o próximo passo.</span>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
