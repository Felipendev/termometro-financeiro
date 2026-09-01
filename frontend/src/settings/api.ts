import { apiFetch } from "../api";
import type { CartaoManualResponse, DividaResponse, MetaContribuicaoResponse } from "../types";
import type {
  CartaoManualRequest,
  CustoFixoItemRequest,
  CustoFixoItemResponse,
  DividaRequest,
  DividaRotativaRequest,
  DividaRotativaResponse,
  PisoHumanoRequest,
  PisoHumanoResponse,
  RendaRequest,
  RendaResponse,
  VerbaMensalRequest,
} from "./types";

const JSON_HEADERS = { "Content-Type": "application/json" };

// ------------------------------------------------------------------------------------ leitura

export function getCustoFixo(signal?: AbortSignal): Promise<CustoFixoItemResponse[]> {
  return apiFetch<CustoFixoItemResponse[]>("/v1/catalogo/custo-fixo", { signal });
}

export function getPisoHumano(signal?: AbortSignal): Promise<PisoHumanoResponse[]> {
  return apiFetch<PisoHumanoResponse[]>("/v1/catalogo/piso-humano", { signal });
}

export function getRenda(competencia: string, signal?: AbortSignal): Promise<RendaResponse> {
  return apiFetch<RendaResponse>(`/v1/catalogo/renda?competencia=${encodeURIComponent(competencia)}`, {
    signal,
  });
}

export function getDividasAtivas(competencia: string, signal?: AbortSignal): Promise<DividaResponse[]> {
  return apiFetch<DividaResponse[]>(`/v1/catalogo/dividas?competencia=${encodeURIComponent(competencia)}`, {
    signal,
  });
}

export function getDividasRotativas(signal?: AbortSignal): Promise<DividaRotativaResponse[]> {
  return apiFetch<DividaRotativaResponse[]>("/v1/catalogo/dividas-rotativas", { signal });
}

export function getCartoesManuais(signal?: AbortSignal): Promise<CartaoManualResponse[]> {
  return apiFetch<CartaoManualResponse[]>("/v1/cartoes/manuais", { signal });
}

// ------------------------------------------------------------------------------------ escrita

export function putRenda(competencia: string, request: RendaRequest): Promise<RendaResponse> {
  return apiFetch<RendaResponse>(`/v1/catalogo/renda/${encodeURIComponent(competencia)}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(request),
  });
}

export function putCustoFixo(id: string, request: CustoFixoItemRequest): Promise<CustoFixoItemResponse> {
  return apiFetch<CustoFixoItemResponse>(`/v1/catalogo/custo-fixo/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(request),
  });
}

export function putPisoHumano(categoria: string, request: PisoHumanoRequest): Promise<PisoHumanoResponse> {
  return apiFetch<PisoHumanoResponse>(`/v1/catalogo/piso-humano/${encodeURIComponent(categoria)}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(request),
  });
}

export function deletePisoHumano(categoria: string): Promise<void> {
  return apiFetch<void>(`/v1/catalogo/piso-humano/${encodeURIComponent(categoria)}`, { method: "DELETE" });
}

export function putDivida(id: string, request: DividaRequest): Promise<DividaResponse> {
  return apiFetch<DividaResponse>(`/v1/catalogo/dividas/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(request),
  });
}

export function deleteDivida(id: string): Promise<void> {
  return apiFetch<void>(`/v1/catalogo/dividas/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export function putDividaRotativa(
  id: string,
  request: DividaRotativaRequest,
): Promise<DividaRotativaResponse> {
  return apiFetch<DividaRotativaResponse>(`/v1/catalogo/dividas-rotativas/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(request),
  });
}

export function deleteDividaRotativa(id: string): Promise<void> {
  return apiFetch<void>(`/v1/catalogo/dividas-rotativas/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export function putCartaoManual(id: string, request: CartaoManualRequest): Promise<CartaoManualResponse> {
  return apiFetch<CartaoManualResponse>(`/v1/cartoes/manuais/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(request),
  });
}

/** Soft delete no backend (ver CartaoRepository) — some da listagem, o cadastro continua salvo. */
export function deleteCartaoManual(id: string): Promise<void> {
  return apiFetch<void>(`/v1/cartoes/manuais/${encodeURIComponent(id)}`, { method: "DELETE" });
}

// -------------------------------------------------------------- orçamento (fora do catálogo)

/** Módulo `orcamento`, não `catalogo` — mesmo padrão de path/verbo, endpoint pré-existente do MVP. */
export function putVerbaMensal(competencia: string, request: VerbaMensalRequest): Promise<void> {
  return apiFetch<void>(`/v1/orcamento/${encodeURIComponent(competencia)}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(request),
  });
}

// -------------------------------------------------------------- contribuição (RN-28)

export function getMetasContribuicao(competencia: string, signal?: AbortSignal): Promise<MetaContribuicaoResponse[]> {
  return apiFetch<MetaContribuicaoResponse[]>(`/v1/metas-contribuicao?competencia=${encodeURIComponent(competencia)}`, { signal });
}

export function postAutorizarProximoPasso(nome: string, competencia: string): Promise<MetaContribuicaoResponse> {
  return apiFetch<MetaContribuicaoResponse>(
    `/v1/metas-contribuicao/${nome}/autorizar-proximo-passo?competencia=${encodeURIComponent(competencia)}`,
    { method: "POST" },
  );
}
