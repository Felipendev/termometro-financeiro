import type { EuDoPresenteResponse } from "../types";
import { formatarDinheiro, formatarPercentual } from "../format";

const LEITURA_NATUREZA: Record<string, string> = {
  FIXO: "Fixo",
  VARIAVEL: "Variável",
  NAO_E_GASTO: "Não é gasto",
};

/** O mês corrente: diagnóstico (RN-08), triagem 4 cores (RN-05) e vampiros (RN-07). */
export function ColunaPresente({ dados }: { dados: EuDoPresenteResponse }) {
  const { diagnostico } = dados;

  return (
    <section className="coluna coluna--presente" aria-labelledby="titulo-presente">
      <h2 id="titulo-presente">Eu do Presente</h2>
      <p className="coluna__subtitulo">Como o mês está indo, agora.</p>

      <div className="cartao">
        <h3>Cartões</h3>
        <p className="cartao__legenda">
          Gasto real por cartão nesta competência — não é somado em nenhum outro lugar da tela
          (parte do custo fixo já é paga pelo próprio cartão; somar duplicaria).
        </p>
        {dados.cartoes.cartoes.length === 0 ? (
          <p className="vazio">Nenhuma compra por cartão neste período.</p>
        ) : (
          <>
            <ul className="lista lista--cartoes">
              {dados.cartoes.cartoes.map((c) => (
                <li key={c.identificador}>
                  <div className="lista--cartoes__cabecalho">
                    <span>{c.nome}</span>
                    <span className="lista__valor">{formatarDinheiro(c.gastoNoMes)}</span>
                  </div>
                  {c.limite !== null && (
                    <div className="lista--cartoes__detalhe">
                      <span>de {formatarDinheiro(c.limite)}</span>
                      {c.percentualUsado !== null && (
                        <span
                          className={
                            Number(c.percentualUsado) >= 0.9
                              ? "etiqueta etiqueta--reajuste"
                              : "lista__meta"
                          }
                        >
                          {formatarPercentual(c.percentualUsado)} do limite
                        </span>
                      )}
                    </div>
                  )}
                </li>
              ))}
            </ul>
            <p className="cartao__total">
              Total nos cartões: {formatarDinheiro(dados.cartoes.totalGastoEmCartoes)}
            </p>
          </>
        )}

        {dados.cartoesManuais.length > 0 && (
          <>
            <h4 className="cartao__subtitulo">Fatura declarada à mão</h4>
            <p className="cartao__legenda">
              Atualize em Configurações quando a fatura fechar.
            </p>
            <ul className="lista lista--cartoes">
              {dados.cartoesManuais.map((c) => (
                <li key={c.id}>
                  <div className="lista--cartoes__cabecalho">
                    <span>{c.nome}</span>
                    <span className="lista__valor">{formatarDinheiro(c.valorFatura)}</span>
                  </div>
                  {c.limite !== null && (
                    <div className="lista--cartoes__detalhe">
                      <span>de {formatarDinheiro(c.limite)}</span>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          </>
        )}
      </div>

      <div className="cartao">
        <h3>Saldo de sobrevivência</h3>
        <dl className="grid-2">
          <div>
            <dt>Renda líquida</dt>
            <dd>{formatarDinheiro(diagnostico.rendaLiquida)}</dd>
          </div>
          <div>
            <dt>Total comprometido</dt>
            <dd>{formatarDinheiro(diagnostico.totalComprometido)}</dd>
          </div>
        </dl>
        <p className={`saldo ${diagnostico.deficit ? "saldo--deficit" : "saldo--positivo"}`}>
          {diagnostico.deficit ? "Déficit: " : "Sobra: "}
          {formatarDinheiro(diagnostico.deficit ? diagnostico.rendaExtraNecessaria : diagnostico.saldo)}
        </p>
      </div>

      <div className="cartao">
        <h3>Triagem por categoria</h3>
        {dados.resumoTriagem.length === 0 ? (
          <p className="vazio">Sem triagem rodada para o mês ainda.</p>
        ) : (
          <ul className="lista lista--triagem">
            {dados.resumoTriagem.map((r) => (
              <li key={r.categoria}>
                <div className="lista--triagem__cabecalho">
                  <span>{r.categoria}</span>
                  <span className="lista__meta">{LEITURA_NATUREZA[r.natureza] ?? r.natureza}</span>
                </div>
                <div className="barra-cores">
                  {(
                    [
                      ["azul", r.totalAzul],
                      ["amarelo", r.totalAmarelo],
                      ["vermelho", r.totalVermelho],
                    ] as const
                  ).map(([cor, valor]) =>
                    Number(valor) > 0 ? (
                      <span
                        key={cor}
                        className={`barra-cores__item barra-cores__item--${cor}`}
                        title={`${cor}: ${formatarDinheiro(valor)}`}
                      >
                        {formatarDinheiro(valor)}
                      </span>
                    ) : null,
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="cartao">
        <h3>Vampiros — assinaturas silenciosas</h3>
        {dados.vampiros.length === 0 ? (
          <p className="vazio">Nenhuma recorrência detectada.</p>
        ) : (
          <ul className="lista">
            {dados.vampiros.map((v) => (
              <li key={v.nomeNormalizado}>
                <span>
                  {v.nomeNormalizado}
                  {v.cobrancaSilenciosa && <span className="etiqueta etiqueta--silenciosa">baixo valor</span>}
                  {v.reajusteDetectado && <span className="etiqueta etiqueta--reajuste">reajustou</span>}
                </span>
                <span className="lista__valor">{v.mensagem}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
