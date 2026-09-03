import type {
  DashboardResponse,
  DashboardInicioResponse,
  ResultadoDaConciliacaoResponse,
  ResultadoDaTriagemResponse,
  ResultadoDaImportacaoResponse,
  LancamentoPlanejadoRequest,
  LancamentoPlanejadoResponse,
  ConsultaLancamentosResponse,
  PlanilhaMesResponse,
  SaldoInicialResponse,
  PropostaImportacaoResponse,
  SimulacaoDecisaoResponse,
  ConfirmarDecisaoResponse,
  PontoComparativoResponse,
  MesDoRollupResponse,
  LancamentoDaPlanilhaRequest,
} from "./types";

const BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ??
  "http://localhost:8080/termometro/api";

// ------------------------------------------------------------------ sessão (login único, ver auth.ts)

const CHAVE_TOKEN = "termometro.auth.token";
const CHAVE_EXPIRA_EM = "termometro.auth.expiraEm";

export function getToken(): string | null {
  return localStorage.getItem(CHAVE_TOKEN);
}

export function salvaSessao(token: string, expiraEm: string): void {
  localStorage.setItem(CHAVE_TOKEN, token);
  localStorage.setItem(CHAVE_EXPIRA_EM, expiraEm);
}

export function limpaSessao(): void {
  localStorage.removeItem(CHAVE_TOKEN);
  localStorage.removeItem(CHAVE_EXPIRA_EM);
}

export function sessaoValida(): boolean {
  const token = localStorage.getItem(CHAVE_TOKEN);
  const expiraEm = localStorage.getItem(CHAVE_EXPIRA_EM);
  if (!token || !expiraEm) return false;
  return new Date(expiraEm).getTime() > Date.now();
}

/** Registrado pelo `App.tsx` — chamado sempre que uma chamada de API voltar 401 (token ausente/expirado). */
let aoNaoAutenticado: (() => void) | null = null;
export function registraCallbackNaoAutenticado(callback: () => void): void {
  aoNaoAutenticado = callback;
}

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

const CONTRATO_API = "2026-09-01-faturas-saldo-recorrencia-v2";

export async function verificaCompatibilidade(signal?: AbortSignal): Promise<void> {
  let resposta: { contratoApi: string };
  try {
    resposta = await apiFetch<{ contratoApi: string }>("/v1/sistema/compatibilidade", { signal });
  } catch (erro) {
    if (erro instanceof DOMException && erro.name === "AbortError") throw erro;
    if (erro instanceof ApiError && erro.status === 404) {
      throw new ApiError(
        "A interface foi atualizada, mas o servidor ainda é de uma versão antiga. Reinicie o servidor e tente novamente.",
        409,
      );
    }
    throw erro;
  }
  if (resposta.contratoApi !== CONTRATO_API) {
    throw new ApiError(
      "A interface e o servidor estão em versões diferentes. Reinicie os dois antes de continuar.",
      409,
    );
  }
}

/** RFC 7807 (application/problem+json) — formato de erro do backend, ver ESPEC seção 6. */
interface ProblemDetail {
  title?: string;
  detail?: string;
  message?: string;
  description?: string;
}

/**
 * Wrapper compartilhado por todos os clientes de API (dashboard e catálogo) — centraliza o
 * parse de erro RFC 7807 e evita duplicar a lógica em cada módulo. `T = void` para respostas
 * 204 No Content (os DELETE do catálogo).
 */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const cabecalhos = new Headers(init?.headers);
  if (token) cabecalhos.set("Authorization", `Bearer ${token}`);

  const resposta = await fetch(`${BASE_URL}${path}`, { ...init, headers: cabecalhos });

  if (!resposta.ok) {
    let mensagem = `Falha na requisição (HTTP ${resposta.status}).`;
    try {
      const problema = (await resposta.json()) as ProblemDetail;
      mensagem = problema.detail ?? problema.message ?? problema.title ?? mensagem;
    } catch {
      // corpo não é JSON — mantém a mensagem genérica
    }
    // /v1/auth/login nunca dispara o gate de sessão: um 401 ali é "senha errada", tratado inline
    // pela tela de login — não uma sessão que expirou no meio do uso.
    if (resposta.status === 401 && path !== "/v1/auth/login") {
      limpaSessao();
      aoNaoAutenticado?.();
    }
    throw new ApiError(mensagem, resposta.status);
  }

  if (resposta.status === 204) return undefined as T;
  return (await resposta.json()) as T;
}

export function buscaDashboard(competencia: string, signal?: AbortSignal): Promise<DashboardResponse> {
  return apiFetch<DashboardResponse>(
    `/v1/dashboard/tres-eus?competencia=${encodeURIComponent(competencia)}`,
    { signal },
  );
}

export function buscaDashboardInicio(competencia: string, signal?: AbortSignal): Promise<DashboardInicioResponse> {
  return apiFetch<DashboardInicioResponse>(`/v1/dashboard/inicio?competencia=${encodeURIComponent(competencia)}`, { signal });
}
export function buscaPlanilha(competencia: string, signal?: AbortSignal): Promise<PlanilhaMesResponse> {
  return apiFetch<PlanilhaMesResponse>(`/v1/planilha?competencia=${encodeURIComponent(competencia)}`, { signal });
}

export function putSaldoInicialPlanilha(dataReferencia: string, valor: string): Promise<SaldoInicialResponse> {
  return apiFetch<SaldoInicialResponse>("/v1/planilha/saldo-inicial", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ dataReferencia, valor }),
  });
}

export function putDiarioDoDia(data: string, valor: string): Promise<void> {
  return apiFetch<void>(`/v1/planilha/${data}/diario`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ valor }),
  });
}

export function putDiarioEmSerie(de: string, ate: string, valor: string): Promise<void> {
  return apiFetch<void>("/v1/planilha/diario-serie", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ de, ate, valor }),
  });
}

export function putObservacaoDoDia(data: string, texto: string): Promise<void> {
  return apiFetch<void>(`/v1/planilha/${data}/observacao`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ texto }),
  });
}

export function postLancamentoNaPlanilha(data: string, request: LancamentoDaPlanilhaRequest): Promise<void> {
  return apiFetch<void>(`/v1/planilha/${data}/lancamentos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function putLancamentoNaPlanilha(data: string, id: string, request: LancamentoDaPlanilhaRequest): Promise<void> {
  return apiFetch<void>(`/v1/planilha/${data}/lancamentos/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function deleteLancamentoNaPlanilha(id: string): Promise<void> {
  return apiFetch<void>(`/v1/planilha/lancamentos/${id}`, { method: "DELETE" });
}

export interface ComandoDeDecisao {
  data: string;
  valor: string;
  descricao: string;
  formaPagamento: "DEBITO" | "CREDITO_AVISTA" | "CREDITO_PARCELADO";
  parcelas: number;
}

export function postSimularDecisao(de: string, ate: string, decisao: ComandoDeDecisao): Promise<SimulacaoDecisaoResponse> {
  return apiFetch<SimulacaoDecisaoResponse>("/v1/planilha/simular-decisao", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ de, ate, ...decisao }),
  });
}

export function postConfirmarDecisao(decisao: ComandoDeDecisao): Promise<ConfirmarDecisaoResponse> {
  return apiFetch<ConfirmarDecisaoResponse>("/v1/planilha/simular-decisao/confirmar", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(decisao),
  });
}

export function buscaComparativoCategorias(competencia: string): Promise<PontoComparativoResponse[]> {
  return apiFetch<PontoComparativoResponse[]>(`/v1/visao-geral/comparativo-categorias?competencia=${encodeURIComponent(competencia)}`);
}

export function buscaFaturasCartao(competencia: string): Promise<import("./types").FaturaCartaoResponse[]> {
  return apiFetch<import("./types").FaturaCartaoResponse[]>(`/v1/faturas-cartao?competencia=${encodeURIComponent(competencia)}`);
}

export function pagaFaturaCartao(competencia: string, request: {
  referencia: string;
  valor: string;
  dataPagamento: string;
  contaOrigemId: string | null;
}): Promise<import("./types").FaturaCartaoResponse> {
  return apiFetch<import("./types").FaturaCartaoResponse>(`/v1/faturas-cartao/pagamentos?competencia=${encodeURIComponent(competencia)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function declaraValorFaturaCartao(competencia: string, request: {
  referencia: string;
  valor: string;
}): Promise<import("./types").FaturaCartaoResponse> {
  return apiFetch<import("./types").FaturaCartaoResponse>(`/v1/faturas-cartao/valor-declarado?competencia=${encodeURIComponent(competencia)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function buscaRollupAnual(ano: number): Promise<MesDoRollupResponse[]> {
  return apiFetch<MesDoRollupResponse[]>(`/v1/relatorios/rollup-anual?ano=${ano}`);
}

// ------------------------------------------------------------------ gatilhos manuais (RN-03/RN-05)

/**
 * RN-03 — precisa rodar antes da triagem: sem isso, pagamento de fatura no débito conta como
 * gasto duplicado do que já apareceu na fatura do cartão.
 */
export function postNaoGasto(competencia: string): Promise<ResultadoDaConciliacaoResponse> {
  return apiFetch<ResultadoDaConciliacaoResponse>(`/v1/nao-gasto/${encodeURIComponent(competencia)}`, {
    method: "POST",
  });
}

export function postTriagem(competencia: string): Promise<ResultadoDaTriagemResponse> {
  return apiFetch<ResultadoDaTriagemResponse>(`/v1/triagem/${encodeURIComponent(competencia)}`, {
    method: "POST",
  });
}

/** Upload manual do CSV Nubank. O browser envia só ao backend local configurado para o app. */
export function postImportacaoNubankCsv(
  identificadorConta: string,
  arquivo: File,
): Promise<ResultadoDaImportacaoResponse> {
  const corpo = new FormData();
  corpo.append("arquivo", arquivo);
  return apiFetch<ResultadoDaImportacaoResponse>(
    `/v1/importacoes/nubank-csv?identificadorConta=${encodeURIComponent(identificadorConta)}`,
    { method: "POST", body: corpo },
  );
}

/** PDF textual de fatura. A conferência contra o total é devolvida antes de o usuário confiar na análise. */
export function postImportacaoFaturaPdf(
  identificadorConta: string,
  formato: "ITAU_PDF" | "PICPAY_PDF",
  arquivo: File,
): Promise<ResultadoDaImportacaoResponse> {
  const corpo = new FormData();
  corpo.append("arquivo", arquivo);
  return apiFetch<ResultadoDaImportacaoResponse>(
    `/v1/importacoes/fatura-pdf?identificadorConta=${encodeURIComponent(identificadorConta)}&formato=${formato}`,
    { method: "POST", body: corpo },
  );
}

/** RN-27.1 — detecção automática por conteúdo, nada persistido ainda. */
export function postPropostaImportacao(arquivo: File): Promise<PropostaImportacaoResponse> {
  const corpo = new FormData();
  corpo.append("arquivo", arquivo);
  return apiFetch<PropostaImportacaoResponse>("/v1/importacoes/propor", { method: "POST", body: corpo });
}

export function putLancamentoPlanejado(id: string, dados: LancamentoPlanejadoRequest): Promise<LancamentoPlanejadoResponse> {
  return apiFetch<LancamentoPlanejadoResponse>(`/v1/lancamentos-planejados/${id}`, { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(dados) });
}

export function postLiquidarLancamentoPlanejado(id: string): Promise<LancamentoPlanejadoResponse> {
  return apiFetch<LancamentoPlanejadoResponse>(`/v1/lancamentos-planejados/${id}/liquidar`, { method: "POST" });
}
export function buscaLancamentos(competencia: string, filtros: {
  tipo?: string;
  status?: string;
  contaId?: string;
  cartaoId?: string;
  categoria?: string;
  q?: string;
  pagina?: number;
  tamanho?: number;
}): Promise<ConsultaLancamentosResponse> {
  const p = new URLSearchParams({
    competencia,
    pagina: String(filtros.pagina ?? 0),
    tamanho: String(filtros.tamanho ?? 30),
  });
  if (filtros.tipo) p.set("tipo", filtros.tipo);
  if (filtros.status) p.set("status", filtros.status);
  if (filtros.contaId) p.set("contaId", filtros.contaId);
  if (filtros.cartaoId) p.set("cartaoId", filtros.cartaoId);
  if (filtros.categoria) p.set("categoria", filtros.categoria);
  if (filtros.q) p.set("q", filtros.q);
  return apiFetch<ConsultaLancamentosResponse>(`/v1/lancamentos?${p}`);
}
export async function buscaTodosLancamentos(competencia: string): Promise<LancamentoPlanejadoResponse[]> {
  const itens: LancamentoPlanejadoResponse[] = [];
  let pagina = 0;
  let temMais: boolean;
  do {
    const resposta = await buscaLancamentos(competencia, { pagina, tamanho: 100 });
    itens.push(...resposta.itens);
    temMais = resposta.temMais;
    pagina += 1;
  } while (temMais);
  return itens;
}
export function postComandoLancamento(id: string, comando: "liquidar" | "reabrir" | "cancelar"): Promise<LancamentoPlanejadoResponse> { return apiFetch(`/v1/lancamentos-planejados/${id}/${comando}`, { method: "POST" }); }
export function deleteLancamentoPlanejado(id: string): Promise<void> { return apiFetch<void>(`/v1/lancamentos-planejados/${id}`, { method: "DELETE" }); }
export function postClassificarTransacao(id: string, dados: { categoria: string; grupo: string; natureza: string; aplicarAoGrupo: boolean }): Promise<unknown> {
  return apiFetch(`/v1/transacoes/${id}/classificar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });
}
