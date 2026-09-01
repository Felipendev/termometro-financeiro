// Espelha os records em br.com.felipe.termometro.catalogo.application.api.{request,response}
// (backend, extensão de escrita da fatia 13). Mesma convenção do dashboard: Dinheiro/Percentual
// trafegam como string (ver DinheiroStr/PercentualStr em ../types) — number vira double no JS.
//
// Convenção de Request: campos monetários/percentuais são string "crua" (não DinheiroStr), já
// que quem preenche é o próprio usuário digitando num formulário — ver normalizarDecimal em
// ../format.ts. Nunca passam por Number() antes de ir pro body do fetch.

import type { DinheiroStr, PercentualStr } from "../types";

export interface CustoFixoItemResponse {
  id: string;
  nome: string;
  valor: DinheiroStr;
  formaPagamento: string | null;
  observacao: string | null;
}

export interface CustoFixoItemRequest {
  nome: string;
  valor: string;
  formaPagamento: string | null;
  observacao: string | null;
  ativo: boolean;
}

export interface PisoHumanoResponse {
  categoria: string;
  valorPiso: DinheiroStr;
  justificativa: string | null;
  estimado: boolean;
}

export interface PisoHumanoRequest {
  valorPiso: string;
  justificativa: string | null;
  estimado: boolean;
}

export interface RendaResponse {
  competencia: string;
  valorLiquido: DinheiroStr;
  observacao: string | null;
}

export interface RendaRequest {
  valorLiquido: string;
  observacao: string | null;
}

/** DividaResponse é a mesma forma usada no dashboard (Eu do Passado) — reaproveitada de ../types. */
export interface DividaRequest {
  nome: string;
  valorParcela: string;
  competenciaUltimaParcela: string;
  observacao: string | null;
}

export interface DividaRotativaResponse {
  id: string;
  nome: string;
  saldoDevedor: DinheiroStr;
  taxaJurosMensal: PercentualStr;
  taxaEstimada: boolean;
  observacao: string | null;
}

/** taxaJurosMensal é uma fração ("0.0636" = 6,36% ao mês), não percentual "6.36" — igual ao backend. */
export interface DividaRotativaRequest {
  nome: string;
  saldoDevedor: string;
  taxaJurosMensal: string;
  taxaEstimada: boolean;
  observacao: string | null;
}

/**
 * Teto de gasto variável do mês (módulo `orcamento`, RN-20 — não é `catalogo`, mas mora aqui na
 * tela porque sem ele o dashboard nem carrega: RN-21/reserva depende da verba real declarada).
 * Sem endpoint de leitura do valor cru (`GET /v1/orcamento/{competencia}` devolve o
 * `VerbaDoDia` já derivado, não dá pra reconstruir verbaVariavel/provisao originais a partir
 * dele) — o formulário é só de escrita, sem pré-preenchimento.
 */
export interface VerbaMensalRequest {
  verbaVariavel: string;
  provisao: string;
}

/**
 * CartaoManualResponse é a mesma forma usada no dashboard (Eu do Presente) — reaproveitada de
 * ../types. Módulo `cartao` (backend), não `catalogo` — mesmo espírito de VerbaMensal: mora nesta
 * tela porque é cadastro manual, mas é bounded context próprio.
 */
export interface CartaoManualRequest {
  nome: string;
  limite: string | null;
  valorFatura: string;
  observacao: string | null;
}
