import { useEffect, useState } from "react";
import { ApiError } from "../api";
import { formatarDinheiro, formatarPercentual, normalizarDecimal, somarDinheiro } from "../format";
import { getDividasRotativas, putDividaRotativa } from "./api";
import type { DividaRotativaRequest, DividaRotativaResponse } from "./types";

interface FormularioProps {
  inicial?: DividaRotativaResponse;
  onSalvar: (request: DividaRotativaRequest) => Promise<void>;
  onCancelar?: () => void;
}

function FormularioDividaRotativa({ inicial, onSalvar, onCancelar }: FormularioProps) {
  const [nome, setNome] = useState(inicial?.nome ?? "");
  const [saldoDevedor, setSaldoDevedor] = useState(inicial?.saldoDevedor ?? "");
  const [taxaJurosMensal, setTaxaJurosMensal] = useState(inicial?.taxaJurosMensal ?? "");
  const [taxaEstimada, setTaxaEstimada] = useState(inicial?.taxaEstimada ?? true);
  const [observacao, setObservacao] = useState(inicial?.observacao ?? "");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  // Só pra preview visual — o que é enviado é a string digitada, normalizada, nunca este número.
  const previewTaxa = normalizarDecimal(taxaJurosMensal);
  const previewPct = previewTaxa !== null ? (Number(previewTaxa) * 100).toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }) : null;

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!nome.trim()) {
      setErro("Informe o nome da dívida.");
      return;
    }
    const saldoNormalizado = normalizarDecimal(saldoDevedor);
    if (saldoNormalizado === null) {
      setErro("Informe um saldo devedor válido (ex.: 1200 ou 1200,00).");
      return;
    }
    const taxaNormalizada = normalizarDecimal(taxaJurosMensal);
    if (taxaNormalizada === null) {
      setErro("Informe a taxa como fração ao mês (ex.: 0,0636 para 6,36%).");
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await onSalvar({
        nome: nome.trim(),
        saldoDevedor: saldoNormalizado,
        taxaJurosMensal: taxaNormalizada,
        taxaEstimada,
        observacao: observacao.trim() || null,
      });
      if (!inicial) {
        setNome("");
        setSaldoDevedor("");
        setTaxaJurosMensal("");
        setTaxaEstimada(true);
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
        placeholder="Saldo devedor (R$)"
        value={saldoDevedor}
        onChange={(e) => setSaldoDevedor(e.target.value)}
      />
      <div className="form__campo-inline">
        <input
          type="text"
          inputMode="decimal"
          placeholder="Taxa a.m. (fração, ex.: 0,0636)"
          value={taxaJurosMensal}
          onChange={(e) => setTaxaJurosMensal(e.target.value)}
        />
        {previewPct !== null && <span className="form__preview">≈ {previewPct}% a.m.</span>}
      </div>
      <label className="form__checkbox">
        <input type="checkbox" checked={taxaEstimada} onChange={(e) => setTaxaEstimada(e.target.checked)} />
        Taxa estimada
      </label>
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
 * Saldos rotativos (RN-09) — sem DELETE separado. Quitar de verdade é editar `saldoDevedor`
 * para 0: some sozinho da lista (GET filtra `saldoDevedor > 0`), não precisa remover a linha.
 */
export function DividasRotativasSection() {
  const [itens, setItens] = useState<DividaRotativaResponse[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [mostrarNovo, setMostrarNovo] = useState(false);
  const total = itens === null ? null : somarDinheiro(itens.map((item) => item.saldoDevedor));

  useEffect(() => {
    const controlador = new AbortController();
    getDividasRotativas(controlador.signal)
      .then(setItens)
      .catch((erroCarga: unknown) => {
        if (erroCarga instanceof DOMException && erroCarga.name === "AbortError") return;
        setErro(
          erroCarga instanceof ApiError ? erroCarga.message : "Não foi possível carregar as dívidas rotativas.",
        );
      });
    return () => controlador.abort();
  }, []);

  async function salvarNovo(request: DividaRotativaRequest) {
    const id = crypto.randomUUID();
    const salvo = await putDividaRotativa(id, request);
    setItens((atual) => [...(atual ?? []), salvo].sort((a, b) => a.nome.localeCompare(b.nome)));
    setMostrarNovo(false);
  }

  async function salvarEdicao(item: DividaRotativaResponse, request: DividaRotativaRequest) {
    const salvo = await putDividaRotativa(item.id, request);
    setItens((atual) => (atual ?? []).map((i) => (i.id === item.id ? salvo : i)));
    setEditandoId(null);
  }

  async function quitar(item: DividaRotativaResponse) {
    try {
      const salvo = await putDividaRotativa(item.id, {
        nome: item.nome,
        saldoDevedor: "0",
        taxaJurosMensal: item.taxaJurosMensal,
        taxaEstimada: item.taxaEstimada,
        observacao: item.observacao,
      });
      // saldoDevedor virou 0 -> não passa mais no filtro "> 0" do backend, some da lista.
      setItens((atual) => (atual ?? []).filter((i) => i.id !== salvo.id));
    } catch (erroQuitar: unknown) {
      setErro(erroQuitar instanceof ApiError ? erroQuitar.message : "Não foi possível quitar.");
    }
  }

  return (
    <section className="cartao">
      <h3>Dívidas rotativas {total !== null && <span className="cartao__total-titulo">{formatarDinheiro(total)}</span>}</h3>
      <p className="cartao__legenda">Cartão, cheque especial e afins. Saldo zerado some da lista sozinho.</p>

      {erro && <p className="form__erro">{erro}</p>}

      {itens === null ? (
        <p className="vazio">Carregando…</p>
      ) : itens.length === 0 ? (
        <p className="vazio">Nenhuma dívida rotativa ativa.</p>
      ) : (
        <ul className="lista lista--config">
          {itens.map((item) =>
            editandoId === item.id ? (
              <li key={item.id}>
                <FormularioDividaRotativa
                  inicial={item}
                  onSalvar={(request) => salvarEdicao(item, request)}
                  onCancelar={() => setEditandoId(null)}
                />
              </li>
            ) : (
              <li key={item.id}>
                <span>{item.nome}</span>
                <span className="lista__valor">{formatarDinheiro(item.saldoDevedor)}</span>
                <span className="lista__meta">
                  {formatarPercentual(item.taxaJurosMensal, 2)} a.m.
                  {item.taxaEstimada ? " (estimada)" : ""}
                </span>
                <div className="form__acoes">
                  <button type="button" className="botao--secundario" onClick={() => setEditandoId(item.id)}>
                    Editar
                  </button>
                  <button type="button" className="botao--perigo" onClick={() => quitar(item)}>
                    Quitar
                  </button>
                </div>
              </li>
            ),
          )}
        </ul>
      )}

      {mostrarNovo ? (
        <FormularioDividaRotativa onSalvar={salvarNovo} onCancelar={() => setMostrarNovo(false)} />
      ) : (
        <button type="button" className="botao--secundario" onClick={() => setMostrarNovo(true)}>
          + Adicionar dívida rotativa
        </button>
      )}
    </section>
  );
}
