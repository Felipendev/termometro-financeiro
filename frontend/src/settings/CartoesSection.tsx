import { useEffect, useState } from "react";
import { ApiError } from "../api";
import { formatarDinheiro, normalizarDecimal, somarDinheiro } from "../format";
import type { CartaoManualResponse } from "../types";
import { deleteCartaoManual, getCartoesManuais, putCartaoManual } from "./api";
import type { CartaoManualRequest } from "./types";

interface FormularioProps {
  inicial?: CartaoManualResponse;
  onSalvar: (request: CartaoManualRequest) => Promise<void>;
  onCancelar?: () => void;
}

function FormularioCartao({ inicial, onSalvar, onCancelar }: FormularioProps) {
  const [nome, setNome] = useState(inicial?.nome ?? "");
  const [limite, setLimite] = useState(inicial?.limite ?? "");
  const [valorFatura, setValorFatura] = useState(inicial?.valorFatura ?? "");
  const [observacao, setObservacao] = useState(inicial?.observacao ?? "");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!nome.trim()) {
      setErro("Informe o nome do cartão.");
      return;
    }
    let limiteNormalizado: string | null = null;
    if (limite.trim()) {
      limiteNormalizado = normalizarDecimal(limite);
      if (limiteNormalizado === null) {
        setErro("Informe um limite válido (ex.: 5000 ou 5000,00), ou deixe em branco.");
        return;
      }
    }
    const valorFaturaNormalizado = normalizarDecimal(valorFatura);
    if (valorFaturaNormalizado === null) {
      setErro("Informe o valor da fatura (ex.: 1234,56). Use 0 se não houver gasto no mês.");
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await onSalvar({
        nome: nome.trim(),
        limite: limiteNormalizado,
        valorFatura: valorFaturaNormalizado,
        observacao: observacao.trim() || null,
      });
      if (!inicial) {
        setNome("");
        setLimite("");
        setValorFatura("");
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
      <input
        type="text"
        placeholder="Nome (ex.: Nubank)"
        value={nome}
        onChange={(e) => setNome(e.target.value)}
      />
      <input
        type="text"
        inputMode="decimal"
        placeholder="Limite (opcional)"
        value={limite}
        onChange={(e) => setLimite(e.target.value)}
      />
      <input
        type="text"
        inputMode="decimal"
        placeholder="Fatura atual (referência)"
        value={valorFatura}
        onChange={(e) => setValorFatura(e.target.value)}
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
 * Cadastro manual de cartão. O valor digitado é apenas a referência da competência atual;
 * o histórico mensal é mantido na seção de faturas.
 *
 * Sem competência: é o valor atual do cartão, editado quando a fatura fechar — mesmo espírito de
 * "Dívidas Rotativas" (saldo devedor que se atualiza, não uma série mensal).
 *
 * Excluir é soft delete no backend: a linha continua salva, só some da lista.
 */
export function CartoesSection() {
  const [itens, setItens] = useState<CartaoManualResponse[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [mostrarNovo, setMostrarNovo] = useState(false);
  const totalFaturas = itens === null ? null : somarDinheiro(itens.map((item) => item.valorFatura));

  useEffect(() => {
    const controlador = new AbortController();
    getCartoesManuais(controlador.signal)
      .then(setItens)
      .catch((erroCarga: unknown) => {
        if (erroCarga instanceof DOMException && erroCarga.name === "AbortError") return;
        setErro(erroCarga instanceof ApiError ? erroCarga.message : "Não foi possível carregar os cartões.");
      });
    return () => controlador.abort();
  }, []);

  async function salvarNovo(request: CartaoManualRequest) {
    const id = crypto.randomUUID();
    const salvo = await putCartaoManual(id, request);
    setItens((atual) => [...(atual ?? []), salvo].sort((a, b) => a.nome.localeCompare(b.nome)));
    setMostrarNovo(false);
  }

  async function salvarEdicao(item: CartaoManualResponse, request: CartaoManualRequest) {
    const salvo = await putCartaoManual(item.id, request);
    setItens((atual) => (atual ?? []).map((i) => (i.id === item.id ? salvo : i)));
    setEditandoId(null);
  }

  async function remover(item: CartaoManualResponse) {
    try {
      await deleteCartaoManual(item.id);
      setItens((atual) => (atual ?? []).filter((i) => i.id !== item.id));
    } catch (erroRemover: unknown) {
      setErro(erroRemover instanceof ApiError ? erroRemover.message : "Não foi possível remover.");
    }
  }

  return (
    <section className="cartao">
      <h3>Cartões {totalFaturas !== null && <span className="cartao__total-titulo">{formatarDinheiro(totalFaturas)}</span>}</h3>
      <p className="cartao__legenda">
        Este cadastro identifica o cartão. Em Cartões, cada mês pode ter um valor próprio e os imports têm prioridade.
      </p>

      {erro && <p className="form__erro">{erro}</p>}

      {itens === null ? (
        <p className="vazio">Carregando…</p>
      ) : itens.length === 0 ? (
        <p className="vazio">Nenhum cartão cadastrado à mão ainda.</p>
      ) : (
        <ul className="lista lista--config">
          {itens.map((item) =>
            editandoId === item.id ? (
              <li key={item.id}>
                <FormularioCartao
                  inicial={item}
                  onSalvar={(request) => salvarEdicao(item, request)}
                  onCancelar={() => setEditandoId(null)}
                />
              </li>
            ) : (
              <li key={item.id}>
                <span>{item.nome}</span>
                <span className="lista__valor">{formatarDinheiro(item.valorFatura)}</span>
                <span className="lista__meta">
                  {item.limite !== null ? `limite ${formatarDinheiro(item.limite)}` : "sem limite declarado"}
                </span>
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
        <FormularioCartao onSalvar={salvarNovo} onCancelar={() => setMostrarNovo(false)} />
      ) : (
        <button type="button" className="botao--secundario" onClick={() => setMostrarNovo(true)}>
          + Adicionar cartão
        </button>
      )}
    </section>
  );
}
