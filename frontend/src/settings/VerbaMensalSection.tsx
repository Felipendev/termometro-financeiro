import { useState } from "react";
import { ApiError } from "../api";
import { formatarCompetencia, normalizarDecimal } from "../format";
import { putVerbaMensal } from "./api";

/**
 * Teto de gastos variáveis do mês (RN-20, módulo `orcamento`) — não é renda. A ausência desse
 * valor deixa apenas o cálculo de reserva indisponível; o restante do dashboard continua útil.
 * Formulário write-only: não existe `GET` do valor cru pra pré-preencher (o endpoint de
 * leitura devolve o `VerbaDoDia` já derivado — não dá pra reconstruir verbaVariavel/provisao
 * originais a partir dele). Salvar sobrescreve o mês inteiro, não soma.
 */
export function VerbaMensalSection({ competencia }: { competencia: string }) {
  const [verbaVariavel, setVerbaVariavel] = useState("");
  const [provisao, setProvisao] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [salvoOk, setSalvoOk] = useState(false);

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    const verbaNormalizada = normalizarDecimal(verbaVariavel);
    if (verbaNormalizada === null) {
      setErro("Informe o limite mensal de gastos variáveis (ex.: 2200 ou 2200,00).");
      return;
    }
    const provisaoNormalizada = normalizarDecimal(provisao);
    if (provisaoNormalizada === null) {
      setErro("Informe a provisão para eventos (pode ser 0).");
      return;
    }
    setErro(null);
    setSalvando(true);
    setSalvoOk(false);
    try {
      await putVerbaMensal(competencia, {
        verbaVariavel: verbaNormalizada,
        provisao: provisaoNormalizada,
      });
      setSalvoOk(true);
    } catch (erroSalvar: unknown) {
      setErro(erroSalvar instanceof ApiError ? erroSalvar.message : "Não foi possível salvar a verba.");
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section className="cartao">
      <h3>Orçamento de gastos variáveis — {formatarCompetencia(competencia)}</h3>
      <p className="cartao__legenda">
        Não é renda: é o limite da sua renda líquida que você separou para gastos variáveis neste
        mês. A provisão fica dentro do limite — ex.: 2.200 com provisão de 200 deixa 2.000 para o dia a dia.
      </p>

      <form className="form" onSubmit={salvar}>
        <div className="form__campo">
          <label htmlFor="verba-valor">Limite mensal de gastos variáveis (R$)</label>
          <input
            id="verba-valor"
            type="text"
            inputMode="decimal"
            placeholder="2200,00"
            value={verbaVariavel}
            onChange={(e) => setVerbaVariavel(e.target.value)}
          />
        </div>

        <div className="form__campo">
          <label htmlFor="verba-provisao">Provisão para eventos (R$)</label>
          <input
            id="verba-provisao"
            type="text"
            inputMode="decimal"
            placeholder="200,00"
            value={provisao}
            onChange={(e) => setProvisao(e.target.value)}
          />
        </div>

        {erro && <p className="form__erro">{erro}</p>}

        <div className="form__acoes">
          <button type="submit" disabled={salvando}>
            {salvando ? "Salvando…" : "Salvar"}
          </button>
          {salvoOk && <span className="form__ok">Salvo.</span>}
        </div>
      </form>
    </section>
  );
}
