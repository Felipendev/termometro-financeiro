import { useEffect, useState } from "react";
import { ApiError } from "../api";
import { formatarDinheiro, normalizarDecimal, somarDinheiro } from "../format";
import { deletePisoHumano, getPisoHumano, putPisoHumano } from "./api";
import type { PisoHumanoRequest, PisoHumanoResponse } from "./types";

interface FormularioProps {
  inicial?: PisoHumanoResponse;
  onSalvar: (categoria: string, request: PisoHumanoRequest) => Promise<void>;
  onCancelar?: () => void;
}

function FormularioPisoHumano({ inicial, onSalvar, onCancelar }: FormularioProps) {
  const [categoria, setCategoria] = useState(inicial?.categoria ?? "");
  const [valorPiso, setValorPiso] = useState(inicial?.valorPiso ?? "");
  const [justificativa, setJustificativa] = useState(inicial?.justificativa ?? "");
  const [estimado, setEstimado] = useState(inicial?.estimado ?? true);
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!categoria.trim()) {
      setErro("Informe a categoria.");
      return;
    }
    const valorNormalizado = normalizarDecimal(valorPiso);
    if (valorNormalizado === null) {
      setErro("Informe um valor válido (ex.: 500 ou 500,00).");
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await onSalvar(categoria.trim(), {
        valorPiso: valorNormalizado,
        justificativa: justificativa.trim() || null,
        estimado,
      });
      if (!inicial) {
        setCategoria("");
        setValorPiso("");
        setJustificativa("");
        setEstimado(true);
      }
    } catch (erroSalvar: unknown) {
      setErro(erroSalvar instanceof ApiError ? erroSalvar.message : "Não foi possível salvar.");
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="form form--linha" onSubmit={enviar}>
      <input
        type="text"
        placeholder="Categoria (ex.: Mercado)"
        value={categoria}
        onChange={(e) => setCategoria(e.target.value)}
        disabled={!!inicial}
      />
      <input
        type="text"
        inputMode="decimal"
        placeholder="Piso (R$)"
        value={valorPiso}
        onChange={(e) => setValorPiso(e.target.value)}
      />
      <input
        type="text"
        placeholder="Justificativa (opcional)"
        value={justificativa}
        onChange={(e) => setJustificativa(e.target.value)}
      />
      <label className="form__checkbox">
        <input type="checkbox" checked={estimado} onChange={(e) => setEstimado(e.target.checked)} />
        Estimado
      </label>
      <div className="form__acoes">
        <button type="submit" disabled={salvando}>
          {salvando ? "Salvando…" : inicial ? "Salvar" : "Adicionar"}
        </button>
        {onCancelar && (
          <button type="button" className="botao--secundario" onClick={onCancelar}>
            Cancelar
          </button>
        )}
      </div>
      {erro && <p className="form__erro">{erro}</p>}
    </form>
  );
}

/** Piso humano (RN-05, RN-08) — categoria é a chave natural; tem DELETE de verdade (não é upsert de ativo/inativo). */
export function PisoHumanoSection() {
  const [itens, setItens] = useState<PisoHumanoResponse[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoCategoria, setEditandoCategoria] = useState<string | null>(null);
  const [mostrarNovo, setMostrarNovo] = useState(false);
  const total = itens === null ? null : somarDinheiro(itens.map((item) => item.valorPiso));

  useEffect(() => {
    const controlador = new AbortController();
    getPisoHumano(controlador.signal)
      .then(setItens)
      .catch((erroCarga: unknown) => {
        if (erroCarga instanceof DOMException && erroCarga.name === "AbortError") return;
        setErro(erroCarga instanceof ApiError ? erroCarga.message : "Não foi possível carregar o piso humano.");
      });
    return () => controlador.abort();
  }, []);

  async function salvarNovo(categoria: string, request: PisoHumanoRequest) {
    const salvo = await putPisoHumano(categoria, request);
    setItens((atual) => [
      ...(atual ?? []).filter((i) => i.categoria !== categoria),
      salvo,
    ].sort((a, b) => a.categoria.localeCompare(b.categoria)));
    setMostrarNovo(false);
  }

  async function salvarEdicao(categoria: string, request: PisoHumanoRequest) {
    const salvo = await putPisoHumano(categoria, request);
    setItens((atual) => (atual ?? []).map((i) => (i.categoria === categoria ? salvo : i)));
    setEditandoCategoria(null);
  }

  async function remover(categoria: string) {
    try {
      await deletePisoHumano(categoria);
      setItens((atual) => (atual ?? []).filter((i) => i.categoria !== categoria));
    } catch (erroRemover: unknown) {
      setErro(erroRemover instanceof ApiError ? erroRemover.message : "Não foi possível remover.");
    }
  }

  return (
    <section className="cartao">
      <h3>Piso humano {total !== null && <span className="cartao__total-titulo">{formatarDinheiro(total)}/mês</span>}</h3>
      <p className="cartao__legenda">Mínimo de dignidade por categoria variável (RN-05).</p>

      {erro && <p className="form__erro">{erro}</p>}

      {itens === null ? (
        <p className="vazio">Carregando…</p>
      ) : itens.length === 0 ? (
        <p className="vazio">Nenhum piso humano cadastrado.</p>
      ) : (
        <ul className="lista lista--config">
          {itens.map((item) =>
            editandoCategoria === item.categoria ? (
              <li key={item.categoria}>
                <FormularioPisoHumano
                  inicial={item}
                  onSalvar={salvarEdicao}
                  onCancelar={() => setEditandoCategoria(null)}
                />
              </li>
            ) : (
              <li key={item.categoria}>
                <span>{item.categoria}</span>
                <span className="lista__valor">{formatarDinheiro(item.valorPiso)}</span>
                <span className="lista__meta">{item.estimado ? "estimado" : "confirmado"}</span>
                <div className="form__acoes">
                  <button
                    type="button"
                    className="botao--secundario"
                    onClick={() => setEditandoCategoria(item.categoria)}
                  >
                    Editar
                  </button>
                  <button type="button" className="botao--perigo" onClick={() => remover(item.categoria)}>
                    Excluir
                  </button>
                </div>
              </li>
            ),
          )}
        </ul>
      )}

      {mostrarNovo ? (
        <FormularioPisoHumano onSalvar={salvarNovo} onCancelar={() => setMostrarNovo(false)} />
      ) : (
        <button type="button" className="botao--secundario" onClick={() => setMostrarNovo(true)}>
          + Adicionar piso humano
        </button>
      )}
    </section>
  );
}
