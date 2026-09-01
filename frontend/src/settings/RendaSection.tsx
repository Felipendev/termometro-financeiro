import { useEffect, useState } from "react";
import { ApiError } from "../api";
import { competenciaAnterior, formatarCompetencia, normalizarDecimal } from "../format";
import { getRenda, putRenda } from "./api";

type Estado =
  | { tipo: "carregando" }
  | { tipo: "erro"; mensagem: string }
  | { tipo: "pronto"; sugestaoDoMesAnterior: boolean };

/**
 * Renda do mês — pré-preenchida com a última declarada quando o mês atual ainda não tem valor
 * próprio (fallback de um único passo para o mês anterior; a API do catálogo só faz busca exata
 * por competência, não tem endpoint de histórico). Salvar sempre grava na competência selecionada,
 * nunca na de origem do fallback.
 */
export function RendaSection({ competencia }: { competencia: string }) {
  const [estado, setEstado] = useState<Estado>({ tipo: "carregando" });
  const [valor, setValor] = useState("");
  const [observacao, setObservacao] = useState("");
  const [erroValidacao, setErroValidacao] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [salvoOk, setSalvoOk] = useState(false);

  useEffect(() => {
    const controlador = new AbortController();

    async function carregar() {
      try {
        const renda = await getRenda(competencia, controlador.signal);
        setSalvoOk(false);
        setValor(renda.valorLiquido);
        setObservacao(renda.observacao ?? "");
        setEstado({ tipo: "pronto", sugestaoDoMesAnterior: false });
      } catch (erro: unknown) {
        if (erro instanceof DOMException && erro.name === "AbortError") return;
        if (erro instanceof ApiError && erro.status === 404) {
          try {
            const anterior = await getRenda(competenciaAnterior(competencia), controlador.signal);
            setSalvoOk(false);
            setValor(anterior.valorLiquido);
            setObservacao("");
            setEstado({ tipo: "pronto", sugestaoDoMesAnterior: true });
          } catch (erroFallback: unknown) {
            if (erroFallback instanceof DOMException && erroFallback.name === "AbortError") return;
            setValor("");
            setObservacao("");
            setEstado({ tipo: "pronto", sugestaoDoMesAnterior: false });
          }
          return;
        }
        const mensagem = erro instanceof ApiError ? erro.message : "Não foi possível carregar a renda.";
        setEstado({ tipo: "erro", mensagem });
      }
    }

    void carregar();
    return () => controlador.abort();
  }, [competencia]);

  async function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    const valorNormalizado = normalizarDecimal(valor);
    if (valorNormalizado === null) {
      setErroValidacao("Informe um valor válido (ex.: 8500 ou 8500,00).");
      return;
    }
    setErroValidacao(null);
    setSalvando(true);
    setSalvoOk(false);
    try {
      const salvo = await putRenda(competencia, {
        valorLiquido: valorNormalizado,
        observacao: observacao.trim() || null,
      });
      setValor(salvo.valorLiquido);
      setEstado({ tipo: "pronto", sugestaoDoMesAnterior: false });
      setSalvoOk(true);
    } catch (erro: unknown) {
      const mensagem = erro instanceof ApiError ? erro.message : "Não foi possível salvar a renda.";
      setEstado({ tipo: "erro", mensagem });
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section className="cartao">
      <h3>Renda líquida — {formatarCompetencia(competencia)}</h3>
      <p className="cartao__legenda">
        Total líquido do mês, incluindo entradas fixas e variáveis. O orçamento de gastos variáveis
        é um limite de saída separado, não uma segunda renda.
      </p>

      {estado.tipo === "carregando" && <p className="vazio">Carregando…</p>}

      {estado.tipo === "erro" && <p className="form__erro">{estado.mensagem}</p>}

      {estado.tipo !== "carregando" && (
        <form className="form" onSubmit={salvar}>
          {estado.tipo === "pronto" && estado.sugestaoDoMesAnterior && (
            <p className="form__aviso">
              Sem valor declarado para este mês — pré-preenchido com o do mês anterior. Confirme ou ajuste.
            </p>
          )}

          <div className="form__campo">
            <label htmlFor="renda-valor">Valor líquido (R$)</label>
            <input
              id="renda-valor"
              type="text"
              inputMode="decimal"
              placeholder="8500,00"
              value={valor}
              onChange={(e) => setValor(e.target.value)}
            />
          </div>

          <div className="form__campo">
            <label htmlFor="renda-observacao">Observação (opcional)</label>
            <input
              id="renda-observacao"
              type="text"
              value={observacao}
              onChange={(e) => setObservacao(e.target.value)}
            />
          </div>

          {erroValidacao && <p className="form__erro">{erroValidacao}</p>}

          <div className="form__acoes">
            <button type="submit" disabled={salvando}>
              {salvando ? "Salvando…" : "Salvar"}
            </button>
            {salvoOk && <span className="form__ok">Salvo.</span>}
          </div>
        </form>
      )}
    </section>
  );
}
