import { useEffect, useState } from "react";
import { ApiError } from "../api";
import { formatarCompetencia, formatarDinheiro, normalizarDecimal, somarDinheiro } from "../format";
import type { DividaResponse } from "../types";
import { deleteDivida, getDividasAtivas, putDivida } from "./api";
import type { DividaRequest } from "./types";

interface FormularioProps {
  inicial?: DividaResponse;
  onSalvar: (request: DividaRequest) => Promise<void>;
  onCancelar?: () => void;
}

function FormularioDivida({ inicial, onSalvar, onCancelar }: FormularioProps) {
  const [nome, setNome] = useState(inicial?.nome ?? "");
  const [valorParcela, setValorParcela] = useState(inicial?.valorParcela ?? "");
  const [competenciaUltimaParcela, setCompetenciaUltimaParcela] = useState(
    inicial?.competenciaUltimaParcela ?? "",
  );
  const [observacao, setObservacao] = useState(inicial?.observacao ?? "");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!nome.trim()) {
      setErro("Informe o nome da dívida.");
      return;
    }
    const valorNormalizado = normalizarDecimal(valorParcela);
    if (valorNormalizado === null) {
      setErro("Informe um valor de parcela válido (ex.: 250 ou 250,00).");
      return;
    }
    if (!competenciaUltimaParcela) {
      setErro("Informe a competência da última parcela.");
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await onSalvar({
        nome: nome.trim(),
        valorParcela: valorNormalizado,
        competenciaUltimaParcela,
        observacao: observacao.trim() || null,
      });
      if (!inicial) {
        setNome("");
        setValorParcela("");
        setCompetenciaUltimaParcela("");
        setObservacao("");
      }
    } catch (erroSalvar: unknown) {
      setErro(erroSalvar instanceof ApiError ? erroSalvar.message : "Não foi possível salvar.");
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="form form--linha" onSubmit={enviar}>
      <input type="text" placeholder="Nome" value={nome} onChange={(e) => setNome(e.target.value)} />
      <input
        type="text"
        inputMode="decimal"
        placeholder="Parcela (R$)"
        value={valorParcela}
        onChange={(e) => setValorParcela(e.target.value)}
      />
      <input
        type="month"
        title="Última parcela"
        value={competenciaUltimaParcela}
        onChange={(e) => setCompetenciaUltimaParcela(e.target.value)}
      />
      <input
        type="text"
        placeholder="Observação (opcional)"
        value={observacao}
        onChange={(e) => setObservacao(e.target.value)}
      />
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

/**
 * Dívidas de parcela fixa ativas na competência selecionada. Excluir aqui é correção de
 * cadastro (registrei errado) — quitação normal é automática quando `competenciaUltimaParcela`
 * passa (RN-08), não precisa de ação nenhuma.
 */
export function DividasSection({ competencia }: { competencia: string }) {
  const [itens, setItens] = useState<DividaResponse[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [mostrarNovo, setMostrarNovo] = useState(false);
  const totalParcelas = itens === null ? null : somarDinheiro(itens.map((item) => item.valorParcela));

  useEffect(() => {
    const controlador = new AbortController();
    getDividasAtivas(competencia, controlador.signal)
      .then(setItens)
      .catch((erroCarga: unknown) => {
        if (erroCarga instanceof DOMException && erroCarga.name === "AbortError") return;
        setErro(erroCarga instanceof ApiError ? erroCarga.message : "Não foi possível carregar as dívidas.");
      });
    return () => controlador.abort();
  }, [competencia]);

  async function salvarNovo(request: DividaRequest) {
    const id = crypto.randomUUID();
    const salvo = await putDivida(id, request);
    setItens((atual) => [...(atual ?? []), salvo].sort((a, b) => a.nome.localeCompare(b.nome)));
    setMostrarNovo(false);
  }

  async function salvarEdicao(item: DividaResponse, request: DividaRequest) {
    const salvo = await putDivida(item.id, request);
    setItens((atual) => (atual ?? []).map((i) => (i.id === item.id ? salvo : i)));
    setEditandoId(null);
  }

  async function remover(item: DividaResponse) {
    try {
      await deleteDivida(item.id);
      setItens((atual) => (atual ?? []).filter((i) => i.id !== item.id));
    } catch (erroRemover: unknown) {
      setErro(erroRemover instanceof ApiError ? erroRemover.message : "Não foi possível remover.");
    }
  }

  return (
    <section className="cartao">
      <h3>Dívidas — {formatarCompetencia(competencia)} {totalParcelas !== null && <span className="cartao__total-titulo">{formatarDinheiro(totalParcelas)}/mês</span>}</h3>
      <p className="cartao__legenda">Ativas na competência selecionada. Excluir é só correção de cadastro.</p>

      {erro && <p className="form__erro">{erro}</p>}

      {itens === null ? (
        <p className="vazio">Carregando…</p>
      ) : itens.length === 0 ? (
        <p className="vazio">Nenhuma dívida ativa nesta competência.</p>
      ) : (
        <ul className="lista lista--config">
          {itens.map((item) =>
            editandoId === item.id ? (
              <li key={item.id}>
                <FormularioDivida
                  inicial={item}
                  onSalvar={(request) => salvarEdicao(item, request)}
                  onCancelar={() => setEditandoId(null)}
                />
              </li>
            ) : (
              <li key={item.id}>
                <span>{item.nome}</span>
                <span className="lista__valor">{formatarDinheiro(item.valorParcela)}/mês</span>
                <span className="lista__meta">até {formatarCompetencia(item.competenciaUltimaParcela)}</span>
                <div className="form__acoes">
                  <button type="button" className="botao--secundario" onClick={() => setEditandoId(item.id)}>
                    Editar
                  </button>
                  <button type="button" className="botao--perigo" onClick={() => remover(item)}>
                    Excluir
                  </button>
                </div>
              </li>
            ),
          )}
        </ul>
      )}

      {mostrarNovo ? (
        <FormularioDivida onSalvar={salvarNovo} onCancelar={() => setMostrarNovo(false)} />
      ) : (
        <button type="button" className="botao--secundario" onClick={() => setMostrarNovo(true)}>
          + Adicionar dívida
        </button>
      )}
    </section>
  );
}
