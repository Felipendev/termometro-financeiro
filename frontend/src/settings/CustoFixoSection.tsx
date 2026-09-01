import { useEffect, useState } from "react";
import { ApiError } from "../api";
import { formatarDinheiro, normalizarDecimal, somarDinheiro } from "../format";
import { getCustoFixo, putCustoFixo } from "./api";
import type { CustoFixoItemRequest, CustoFixoItemResponse } from "./types";

interface FormularioProps {
  inicial?: CustoFixoItemResponse;
  onSalvar: (request: CustoFixoItemRequest) => Promise<void>;
  onCancelar?: () => void;
}

function FormularioCustoFixo({ inicial, onSalvar, onCancelar }: FormularioProps) {
  const [nome, setNome] = useState(inicial?.nome ?? "");
  const [valor, setValor] = useState(inicial?.valor ?? "");
  const [formaPagamento, setFormaPagamento] = useState(inicial?.formaPagamento ?? "");
  const [observacao, setObservacao] = useState(inicial?.observacao ?? "");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!nome.trim()) {
      setErro("Informe o nome do item.");
      return;
    }
    const valorNormalizado = normalizarDecimal(valor);
    if (valorNormalizado === null) {
      setErro("Informe um valor válido (ex.: 350 ou 350,00).");
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await onSalvar({
        nome: nome.trim(),
        valor: valorNormalizado,
        formaPagamento: formaPagamento.trim() || null,
        observacao: observacao.trim() || null,
        ativo: true,
      });
      if (!inicial) {
        setNome("");
        setValor("");
        setFormaPagamento("");
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
        placeholder="Nome (ex.: Aluguel)"
        value={nome}
        onChange={(e) => setNome(e.target.value)}
      />
      <input
        type="text"
        inputMode="decimal"
        placeholder="Valor"
        value={valor}
        onChange={(e) => setValor(e.target.value)}
      />
      <input
        type="text"
        placeholder="Forma de pagamento (opcional)"
        value={formaPagamento}
        onChange={(e) => setFormaPagamento(e.target.value)}
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
 * Custo fixo não tem DELETE — "remover" é editar `ativo: false` (ver Javadoc de CatalogoAPI).
 * GET só retorna itens ativos, então desativar some da lista imediatamente; não existe tela
 * pra reativar um item já desativado (limite aceito — pior caso é recadastrar).
 */
export function CustoFixoSection() {
  const [itens, setItens] = useState<CustoFixoItemResponse[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [mostrarNovo, setMostrarNovo] = useState(false);
  const total = itens === null ? null : somarDinheiro(itens.map((item) => item.valor));

  useEffect(() => {
    const controlador = new AbortController();
    getCustoFixo(controlador.signal)
      .then(setItens)
      .catch((erroCarga: unknown) => {
        if (erroCarga instanceof DOMException && erroCarga.name === "AbortError") return;
        setErro(erroCarga instanceof ApiError ? erroCarga.message : "Não foi possível carregar o custo fixo.");
      });
    return () => controlador.abort();
  }, []);

  async function salvarNovo(request: CustoFixoItemRequest) {
    const id = crypto.randomUUID();
    const salvo = await putCustoFixo(id, request);
    setItens((atual) => [...(atual ?? []), salvo].sort((a, b) => a.nome.localeCompare(b.nome)));
    setMostrarNovo(false);
  }

  async function salvarEdicao(item: CustoFixoItemResponse, request: CustoFixoItemRequest) {
    const salvo = await putCustoFixo(item.id, request);
    setItens((atual) => (atual ?? []).map((i) => (i.id === item.id ? salvo : i)));
    setEditandoId(null);
  }

  async function desativar(item: CustoFixoItemResponse) {
    try {
      await putCustoFixo(item.id, {
        nome: item.nome,
        valor: item.valor,
        formaPagamento: item.formaPagamento,
        observacao: item.observacao,
        ativo: false,
      });
      setItens((atual) => (atual ?? []).filter((i) => i.id !== item.id));
    } catch (erroDesativar: unknown) {
      setErro(erroDesativar instanceof ApiError ? erroDesativar.message : "Não foi possível desativar.");
    }
  }

  return (
    <section className="cartao">
      <h3>Custo fixo {total !== null && <span className="cartao__total-titulo">{formatarDinheiro(total)}/mês</span>}</h3>
      <p className="cartao__legenda">Itens ativos. Desativar não apaga o histórico já processado.</p>

      {erro && <p className="form__erro">{erro}</p>}

      {itens === null ? (
        <p className="vazio">Carregando…</p>
      ) : itens.length === 0 ? (
        <p className="vazio">Nenhum custo fixo cadastrado.</p>
      ) : (
        <ul className="lista lista--config">
          {itens.map((item) =>
            editandoId === item.id ? (
              <li key={item.id}>
                <FormularioCustoFixo
                  inicial={item}
                  onSalvar={(request) => salvarEdicao(item, request)}
                  onCancelar={() => setEditandoId(null)}
                />
              </li>
            ) : (
              <li key={item.id}>
                <span>{item.nome}</span>
                <span className="lista__valor">{formatarDinheiro(item.valor)}</span>
                <span className="lista__meta">{item.formaPagamento || "—"}</span>
                <div className="form__acoes">
                  <button type="button" className="botao--secundario" onClick={() => setEditandoId(item.id)}>
                    Editar
                  </button>
                  <button type="button" className="botao--perigo" onClick={() => desativar(item)}>
                    Desativar
                  </button>
                </div>
              </li>
            ),
          )}
        </ul>
      )}

      {mostrarNovo ? (
        <FormularioCustoFixo onSalvar={salvarNovo} onCancelar={() => setMostrarNovo(false)} />
      ) : (
        <button type="button" className="botao--secundario" onClick={() => setMostrarNovo(true)}>
          + Adicionar custo fixo
        </button>
      )}
    </section>
  );
}
