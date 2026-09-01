import { useEffect, useState } from "react";
import { ApiError, postImportacaoFaturaPdf, postImportacaoNubankCsv, postPropostaImportacao } from "../api";
import { getCartoesManuais } from "../settings/api";
import { formatarDinheiro } from "../format";
import type { CartaoManualResponse, PropostaImportacaoResponse, ResultadoDaImportacaoResponse } from "../types";

const NOME_DO_FORMATO: Record<string, string> = {
  CSV_NUBANK: "Nubank (CSV)",
  ITAU_PDF: "Itaú (PDF)",
  PICPAY_PDF: "PicPay (PDF)",
};

/**
 * RN-27 — um botão só. O sistema tenta reconhecer o banco pelo conteúdo do arquivo; se não
 * conseguir, cai no fallback de escolha manual (RN-27.1). Nada é persistido até a confirmação
 * explícita (RN-27.2) — a proposta é só leitura.
 */
export function ImportarInteligente() {
  const [cartoes, setCartoes] = useState<CartaoManualResponse[]>([]);
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [cartaoId, setCartaoId] = useState("");
  const [formatoEscolhido, setFormatoEscolhido] = useState("");
  const [proposta, setProposta] = useState<PropostaImportacaoResponse | null>(null);
  const [analisando, setAnalisando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [resultado, setResultado] = useState<ResultadoDaImportacaoResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    getCartoesManuais().then(setCartoes).catch(() => setCartoes([]));
  }, []);

  async function aoEscolherArquivo(novoArquivo: File | null) {
    setArquivo(novoArquivo);
    setProposta(null);
    setResultado(null);
    setErro(null);
    if (!novoArquivo) return;
    setAnalisando(true);
    try {
      const propostaRecebida = await postPropostaImportacao(novoArquivo);
      setProposta(propostaRecebida);
      setFormatoEscolhido(propostaRecebida.formatoDetectado ?? propostaRecebida.formatosDisponiveis[0] ?? "");
    } catch (causa: unknown) {
      setErro(causa instanceof ApiError ? causa.message : "Não consegui analisar o arquivo.");
    } finally {
      setAnalisando(false);
    }
  }

  async function confirmar() {
    if (!arquivo || !cartaoId || !formatoEscolhido) {
      setErro("Escolha o cartão antes de confirmar a importação.");
      return;
    }
    setConfirmando(true);
    setErro(null);
    try {
      const resposta = formatoEscolhido === "CSV_NUBANK"
        ? await postImportacaoNubankCsv(cartaoId, arquivo)
        : await postImportacaoFaturaPdf(cartaoId, formatoEscolhido as "ITAU_PDF" | "PICPAY_PDF", arquivo);
      setResultado(resposta);
      setProposta(null);
    } catch (causa: unknown) {
      setErro(causa instanceof ApiError ? causa.message : "Não foi possível importar a fatura.");
    } finally {
      setConfirmando(false);
    }
  }

  function recomecar() {
    setArquivo(null);
    setProposta(null);
    setResultado(null);
    setErro(null);
  }

  return (
    <section className="cartao cartao--importacao">
      <p className="eyebrow">Fatura do cartão</p>
      <h3>Importar</h3>
      <p className="cartao__legenda">
        Solte o CSV ou PDF da fatura — o sistema identifica o banco sozinho e mostra o total
        reconciliado antes de importar de verdade.
      </p>

      {!proposta && !resultado && (
        <div className="form">
          <div className="form__campo">
            <label htmlFor="importacao-arquivo">Arquivo da fatura</label>
            <input
              id="importacao-arquivo"
              type="file"
              accept=".csv,.pdf,application/pdf,text/csv"
              onChange={(evento) => aoEscolherArquivo(evento.target.files?.[0] ?? null)}
            />
          </div>
          {analisando && <p>Analisando o arquivo...</p>}
        </div>
      )}

      {proposta && !resultado && (
        <div className="importacao__proposta">
          {proposta.reconhecido ? (
            <p>
              Identifiquei <strong>{NOME_DO_FORMATO[proposta.formatoDetectado ?? ""] ?? proposta.formatoDetectado}</strong>{" "}
              — {proposta.transacoesEncontradas} lançamentos, {formatarDinheiro(proposta.totalLido)}.{" "}
              {proposta.reconciliacaoFechou ? "A soma fechou com o total impresso." : "A soma não fechou — revise os avisos."}
            </p>
          ) : (
            <p>Não reconheci o arquivo automaticamente — escolha o banco manualmente.</p>
          )}

          {!proposta.reconhecido && (
            <div className="form__campo">
              <label htmlFor="importacao-formato-manual">Banco</label>
              <select id="importacao-formato-manual" value={formatoEscolhido} onChange={(evento) => setFormatoEscolhido(evento.target.value)}>
                {proposta.formatosDisponiveis.map((formato) => (
                  <option key={formato} value={formato}>{NOME_DO_FORMATO[formato] ?? formato}</option>
                ))}
              </select>
            </div>
          )}

          <div className="form__campo">
            <label htmlFor="importacao-cartao">Este arquivo é fatura de qual cartão?</label>
            <select id="importacao-cartao" value={cartaoId} onChange={(evento) => setCartaoId(evento.target.value)}>
              <option value="">Selecione...</option>
              {cartoes.map((cartao) => <option key={cartao.id} value={cartao.id}>{cartao.nome}</option>)}
            </select>
          </div>

          {proposta.avisos.length > 0 && <ul className="importacao__avisos">{proposta.avisos.map((aviso) => <li key={aviso}>{aviso}</li>)}</ul>}

          <div className="importacao__acoes">
            <button type="button" onClick={confirmar} disabled={confirmando || !cartaoId}>
              {confirmando ? "Importando..." : "Confirmar importação"}
            </button>
            <button type="button" className="botao--secundario" onClick={recomecar} disabled={confirmando}>Cancelar</button>
          </div>
        </div>
      )}

      {erro && <p className="form__erro">{erro}</p>}

      {resultado && (
        <div className="importacao__resultado">
          <strong>{resultado.transacoesLidas} lançamentos lidos · {formatarDinheiro(resultado.totalDeDespesas)}</strong>
          <span>{resultado.confiavel ? "Fatura conciliada." : "A soma não fechou: revise os avisos antes de analisar."}</span>
          {resultado.competenciasProcessadas.length > 0 && <span>Classificação e análise atualizadas para {resultado.competenciasProcessadas.join(", ")}.</span>}
          {resultado.avisos.length > 0 && <ul>{resultado.avisos.map((aviso) => <li key={aviso}>{aviso}</li>)}</ul>}
          <button type="button" onClick={recomecar}>Importar outra fatura</button>
        </div>
      )}
    </section>
  );
}
