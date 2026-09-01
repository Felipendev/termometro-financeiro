import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, ChevronDown, ChevronUp, CreditCard, ReceiptText } from "lucide-react";
import { ApiError, buscaFaturasCartao, declaraValorFaturaCartao, pagaFaturaCartao } from "../api";
import { formatarDespesa, formatarDinheiro, normalizarDecimal } from "../format";
import type { ContaManualResponse, FaturaCartaoResponse, LancamentoPlanejadoResponse } from "../types";
import { LogoCartao } from "./LogoCartao";

function dataInicial(competencia: string) {
  const hoje = new Date();
  const atual = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, "0")}`;
  if (competencia === atual) return `${competencia}-${String(hoje.getDate()).padStart(2, "0")}`;
  const [ano, mes] = competencia.split("-").map(Number);
  const ultimoDia = new Date(ano, mes, 0).getDate();
  return `${competencia}-${ultimoDia}`;
}

export function FaturasCartaoSection({ competencia, contas, movimentos, nomeInicial, aoPagar }: {
  competencia: string;
  contas: ContaManualResponse[];
  movimentos: LancamentoPlanejadoResponse[];
  nomeInicial?: string | null;
  aoPagar?: () => void;
}) {
  const [faturas, setFaturas] = useState<FaturaCartaoResponse[] | null>(null);
  const [aberta, setAberta] = useState<string | null>(null);
  const [pagando, setPagando] = useState<string | null>(null);
  const [ajustando, setAjustando] = useState<string | null>(null);
  const [valor, setValor] = useState("");
  const [dataPagamento, setDataPagamento] = useState(dataInicial(competencia));
  const [contaOrigemId, setContaOrigemId] = useState("");
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    setFaturas(null);
    setErro(null);
    buscaFaturasCartao(competencia)
      .then((resultado) => {
        if (!ativo) return;
        setFaturas(resultado);
        if (nomeInicial) setAberta(resultado.find((item) => item.nome === nomeInicial)?.referencia ?? null);
      })
      .catch((causa: unknown) => { if (ativo) setErro(causa instanceof Error ? causa.message : "Não foi possível carregar as faturas."); });
    return () => { ativo = false; };
  }, [competencia, nomeInicial]);

  const itensPorFatura = useMemo(() => new Map((faturas ?? []).map((fatura) => {
    const manualId = fatura.referencia.startsWith("MANUAL:") ? fatura.referencia.slice(7) : null;
    const importadoId = fatura.referencia.startsWith("IMPORTADO:") ? fatura.referencia.slice(10) : null;
    const itens = movimentos.filter((item) => item.tipo === "DESPESA" && item.status !== "CANCELADO"
      && item.categoria?.natureza !== "NAO_E_GASTO"
      && (manualId ? item.cartaoManualId === manualId || item.contaOuCartao === manualId
        : item.contaOuCartao === importadoId || item.contaOuCartao?.toLocaleLowerCase("pt-BR") === fatura.nome.toLocaleLowerCase("pt-BR")));
    return [fatura.referencia, itens] as const;
  })), [faturas, movimentos]);

  function iniciarPagamento(fatura: FaturaCartaoResponse) {
    setPagando(fatura.referencia);
    setValor(fatura.saldoAberto.replace(".", ","));
    setDataPagamento(dataInicial(competencia));
    setContaOrigemId("");
    setErro(null);
  }

  async function confirmarPagamento(fatura: FaturaCartaoResponse) {
    const normalizado = normalizarDecimal(valor);
    if (!normalizado || Number(normalizado) <= 0) {
      setErro("Informe um valor de pagamento maior que zero.");
      return;
    }
    try {
      const atualizada = await pagaFaturaCartao(competencia, {
        referencia: fatura.referencia,
        valor: normalizado,
        dataPagamento,
        contaOrigemId: contaOrigemId || null,
      });
      setFaturas((atuais) => (atuais ?? []).map((item) => item.referencia === atualizada.referencia ? atualizada : item));
      setPagando(null);
      setErro(null);
      aoPagar?.();
    } catch (causa: unknown) {
      setErro(causa instanceof ApiError ? causa.message : "Não foi possível registrar o pagamento.");
    }
  }

  async function confirmarAjuste(fatura: FaturaCartaoResponse) {
    const normalizado = normalizarDecimal(valor);
    if (normalizado === null || Number(normalizado) < 0) {
      setErro("Informe um valor de fatura válido.");
      return;
    }
    try {
      const atualizada = await declaraValorFaturaCartao(competencia, { referencia: fatura.referencia, valor: normalizado });
      setFaturas((atuais) => (atuais ?? []).map((item) => item.referencia === atualizada.referencia ? atualizada : item));
      setAjustando(null);
      setErro(null);
      aoPagar?.();
    } catch (causa: unknown) {
      setErro(causa instanceof ApiError ? causa.message : "Não foi possível ajustar a fatura do mês.");
    }
  }

  if (erro && !faturas) return <p className="form__erro" role="alert">{erro}</p>;
  if (!faturas) return <p className="vazio">Carregando faturas...</p>;
  if (faturas.length === 0) return <div className="estado-vazio"><CreditCard size={26} /><strong>Nenhuma fatura nesta competência</strong><p>Importe uma fatura ou cadastre um cartão manual.</p></div>;

  return <section className="faturas" aria-label="Faturas de cartão">
    {erro && <p className="form__erro" role="alert">{erro}</p>}
    {faturas.map((fatura) => {
      const expandida = aberta === fatura.referencia;
      const itens = itensPorFatura.get(fatura.referencia) ?? [];
      return <article className={`fatura ${expandida ? "fatura--aberta" : ""}`} key={fatura.referencia}>
        <button type="button" className="fatura__resumo" aria-expanded={expandida} onClick={() => setAberta(expandida ? null : fatura.referencia)}>
          <span className="cartao-marca cartao-marca--logo"><LogoCartao nome={fatura.nome} /></span>
          <span><strong>{fatura.nome}</strong><small>{fatura.origem === "IMPORTACAO" ? "Calculada pelos imports" : "Valor declarado"}</small></span>
          <span className={`fatura__status fatura__status--${fatura.status.toLocaleLowerCase("pt-BR")}`}>{fatura.status === "PAGA" ? "Paga" : fatura.status === "PARCIAL" ? "Parcial" : fatura.status === "SEM_MOVIMENTO" ? "Sem movimento" : "Aberta"}</span>
          <b>{formatarDinheiro(fatura.saldoAberto)}</b>
          {expandida ? <ChevronUp size={17} /> : <ChevronDown size={17} />}
        </button>
        {expandida && <div className="fatura__detalhe">
          <dl><div><dt>Total da fatura</dt><dd>{formatarDinheiro(fatura.valorTotal)}</dd></div><div><dt>Já pago</dt><dd className="valor--receita">{formatarDinheiro(fatura.valorPago)}</dd></div><div><dt>Em aberto</dt><dd className={Number(fatura.saldoAberto) > 0 ? "valor--despesa" : "valor--receita"}>{formatarDinheiro(fatura.saldoAberto)}</dd></div></dl>
          <div className="fatura__itens"><h4><ReceiptText size={16} /> Valores incluídos</h4>{itens.length === 0 ? <p className="vazio">O valor foi declarado manualmente; não há compras importadas para detalhar.</p> : <ul>{itens.map((item) => <li key={item.id}><span><strong>{item.descricao}</strong><small>{item.vencimento} · {item.categoria?.nome ?? "Sem categoria"}</small></span><b>{formatarDespesa(item.valor)}</b></li>)}</ul>}</div>
          {fatura.pagamentos.length > 0 && <div className="fatura__pagamentos"><h4><CheckCircle2 size={16} /> Pagamentos registrados</h4><ul>{fatura.pagamentos.map((item) => <li key={item.id}><span>{item.data}</span><strong>{formatarDinheiro(item.valor)}</strong></li>)}</ul></div>}
          {pagando === fatura.referencia ? <div className="fatura__form-pagamento">
            <label>Valor a pagar<input type="text" inputMode="decimal" value={valor} onChange={(evento) => setValor(evento.target.value)} /></label>
            <label>Data<input type="date" min={`${competencia}-01`} max={dataInicial(competencia)} value={dataPagamento} onChange={(evento) => setDataPagamento(evento.target.value)} /></label>
            <label>Conta de saída (opcional)<select value={contaOrigemId} onChange={(evento) => setContaOrigemId(evento.target.value)}><option value="">Não alterar saldo de conta</option>{contas.map((conta) => <option key={conta.id} value={conta.id}>{conta.nome}</option>)}</select></label>
            <div><button type="button" onClick={() => void confirmarPagamento(fatura)}>Confirmar pagamento</button><button type="button" className="botao--secundario" onClick={() => setPagando(null)}>Cancelar</button></div>
          </div> : ajustando === fatura.referencia ? <div className="fatura__form-pagamento fatura__form-pagamento--ajuste"><label>Valor total desta competência<input type="text" inputMode="decimal" value={valor} onChange={(evento) => setValor(evento.target.value)} /></label><div><button type="button" onClick={() => void confirmarAjuste(fatura)}>Salvar valor do mês</button><button type="button" className="botao--secundario" onClick={() => setAjustando(null)}>Cancelar</button></div></div> : <div className="fatura__acoes">{Number(fatura.saldoAberto) > 0 && <button type="button" onClick={() => iniciarPagamento(fatura)}>Pagar fatura</button>}{fatura.referencia.startsWith("MANUAL:") && <button type="button" className="botao--secundario" onClick={() => { setAjustando(fatura.referencia); setPagando(null); setValor(fatura.valorTotal.replace(".", ",")); }}>Ajustar valor deste mês</button>}</div>}
        </div>}
      </article>;
    })}
  </section>;
}
