// Espelha os records em br.com.felipe.termometro.dashboard.application.api.response
// (backend, fatia 13/RN-11). Dinheiro e Percentual trafegam como string — ver JsonConfig
// no backend: number em JSON vira double no JS e volta com centavo errado.

/** "1234.56" / "-89.90" — string canônica de Dinheiro.paraJson() */
export type DinheiroStr = string;

/** "0.285714" — fração de Percentual.paraJson() (multiplique por 100 pra exibir como %) */
export type PercentualStr = string;

export type Veredito = "VIAVEL" | "VIAVEL_PARCIALMENTE" | "INVIAVEL";
export type StatusProjecao = "VIAVEL" | "VIAVEL_COM_APERTO" | "INVIAVEL";
export type NivelDeReserva = "UM_MES" | "TRES_MESES" | "SEIS_MESES";
export type Periodicidade = "MENSAL" | "ANUAL";
export type Natureza = "FIXO" | "VARIAVEL" | "NAO_E_GASTO";

export interface QuedaDeRendaResponse {
  rendaAnterior: DinheiroStr;
  rendaAtual: DinheiroStr;
  quedaPct: PercentualStr;
  pesoFixoAntes: PercentualStr;
  pesoFixoAgora: PercentualStr;
  excedenteEstrutural: DinheiroStr;
  mensagem: string;
}

export interface ViabilidadeResponse {
  competencia: string;
  rendaLiquida: DinheiroStr;
  custoFixoTotal: DinheiroStr;
  pisoVariavelTotal: DinheiroStr;
  custoMinimoVida: DinheiroStr;
  economiaMaxima: DinheiroStr;
  taxaMaxima: PercentualStr;
  metaEconomia: PercentualStr;
  veredito: Veredito;
  alvoReducaoFixo: DinheiroStr;
  quedaDeRenda: QuedaDeRendaResponse | null;
  leitura: string;
}

export interface DividaResponse {
  id: string;
  nome: string;
  valorParcela: DinheiroStr;
  competenciaUltimaParcela: string;
  observacao: string | null;
}

export interface CompromissoFuturoItemResponse {
  descricao: string;
  competencia: string;
  valor: DinheiroStr;
}

export interface EuDoPassadoResponse {
  compromissosFuturos: CompromissoFuturoItemResponse[];
  dividas: DividaResponse[];
}

export interface SaldoDeSobrevivenciaResponse {
  competencia: string;
  rendaLiquida: DinheiroStr;
  comprometidoFixo: DinheiroStr;
  minimoVariavel: DinheiroStr;
  servicoDivida: DinheiroStr;
  totalComprometido: DinheiroStr;
  saldo: DinheiroStr;
  deficit: boolean;
  rendaExtraNecessaria: DinheiroStr;
}

export interface ResumoDeCategoriaResponse {
  categoria: string;
  natureza: Natureza;
  totalAzul: DinheiroStr;
  totalAmarelo: DinheiroStr;
  totalVermelho: DinheiroStr;
  totalVerde: DinheiroStr;
  totalNaoTriada: DinheiroStr;
}

export interface RecorrenciaResponse {
  nomeNormalizado: string;
  periodicidade: Periodicidade;
  valorMedio: DinheiroStr;
  custoAnual: DinheiroStr;
  confianca: PercentualStr;
  primeiraOcorrencia: string;
  ultimaOcorrencia: string;
  ocorrencias: number;
  reajusteDetectado: boolean;
  cobrancaSilenciosa: boolean;
  mensagem: string;
}

/**
 * Gasto real por cartão de crédito na competência (extensão "Cartões", fatia 13) — soma bruta
 * das transações, não depende de classificação/triagem/não-gasto terem rodado. Puramente
 * informativo: nunca some com custo fixo em outro lugar da tela (parte do custo fixo já é paga
 * pelo próprio cartão).
 */
export interface CartaoResponse {
  identificador: string;
  nome: string;
  limite: DinheiroStr | null;
  gastoNoMes: DinheiroStr;
  percentualUsado: PercentualStr | null;
}

export interface ResumoCartoesResponse {
  cartoes: CartaoResponse[];
  totalGastoEmCartoes: DinheiroStr;
}

/**
 * Cartão cadastrado à mão (módulo `cartao`, backend) — nome + limite opcional + valor da fatura
 * digitado por Felipe, estado atual sem histórico por competência (mesmo espírito de
 * DividaRotativa.saldoDevedor).
 */
export interface CartaoManualResponse {
  id: string;
  nome: string;
  limite: DinheiroStr | null;
  valorFatura: DinheiroStr;
  observacao: string | null;
}

export interface EuDoPresenteResponse {
  diagnostico: SaldoDeSobrevivenciaResponse;
  resumoTriagem: ResumoDeCategoriaResponse[];
  vampiros: RecorrenciaResponse[];
  cartoes: ResumoCartoesResponse;
  cartoesManuais: CartaoManualResponse[];
}

/** POST /v1/nao-gasto/{competencia} — RN-03, roda sob pedido. */
export interface ResultadoDaConciliacaoResponse {
  pagamentosDeFaturaCasados: number;
  transferenciasCasadas: number;
  estornosCasados: number;
  valorTotalIgnorado: number;
  detalhes: string[];
}

/** POST /v1/triagem/{competencia} — RN-05, roda sob pedido. */
export interface ResultadoDaTriagemResponse {
  competencia: string;
  analisadas: number;
  triadas: number;
  porEtiqueta: Record<string, number>;
}

export interface MarcosResponse {
  dataQuitacao: string | null;
  primeiroRealGuardado: string | null;
  reservaCompleta: string | null;
  jurosTotaisPagos: DinheiroStr;
  mesesAteQuitacao: number | null;
}

export interface NivelDeReservaResponse {
  nivel: NivelDeReserva;
  alvo: DinheiroStr;
  atingido: boolean;
  competenciaPrevista: string | null;
}

export interface PainelDeReservaResponse {
  custoMensal: DinheiroStr;
  niveis: NivelDeReservaResponse[];
  proximoNivel: NivelDeReserva | null;
}

/** Campos BigDecimal "crus" (não Dinheiro/Percentual) do plano de ajuste chegam como number — decisão do backend (RN-15), não deste front. */
export interface AlvoMensalResponse {
  mes: number;
  alvo: number;
  reducaoPercentual: number;
}

export interface ItemDoPlanoResponse {
  categoria: string;
  tipo: string;
  valorAtual: number;
  alvoFinal: number;
  alvosMensais: AlvoMensalResponse[];
  rampaAlongada: boolean;
  dor: number;
  economiaMensalFinal: number;
}

export interface AcaoPrioritariaResponse {
  categoria: string;
  descricao: string;
  economiaMensal: number;
  dor: number;
  impacto: number;
}

export interface PlanoDeAjusteResponse {
  competenciaInicio: string;
  itens: ItemDoPlanoResponse[];
  avisos: string[];
  acoesPrioritarias: AcaoPrioritariaResponse[];
  economiaMensalFinalTotal: number;
}

export interface EuDoFuturoResponse {
  marcos: MarcosResponse;
  reserva?: PainelDeReservaResponse | null;
  reservaIndisponivel?: string | null;
  planoAjuste: PlanoDeAjusteResponse;
}

export interface DashboardResponse {
  competencia: string;
  viabilidade: ViabilidadeResponse;
  euDoPassado: EuDoPassadoResponse;
  euDoPresente: EuDoPresenteResponse;
  euDoFuturo: EuDoFuturoResponse;
}

export interface ContaManualResponse { id: string; identificador: string; nome: string; tipo: string; saldo: DinheiroStr; }
export interface CategoriaDoLancamentoResponse { nome: string; grupo: string; natureza: string; }
export type MarcacaoPlanejamento = "NENHUMA" | "CUSTO_FIXO" | "PISO_HUMANO";
export type OrigemReceita = "SALARIO" | "INVESTIMENTO" | "EMPRESTIMO";
export interface LancamentoPlanejadoResponse { id: string; descricao: string; tipo: "DESPESA" | "RECEITA" | "TRANSFERENCIA"; valor: DinheiroStr; vencimento: string; status: string; contaOrigemId: string | null; contaDestinoId: string | null; categoria: CategoriaDoLancamentoResponse | null; cartaoManualId: string | null; transacaoId: string | null; marcacaoPlanejamento: MarcacaoPlanejamento; contaOuCartao: string | null; editavel: boolean; origem: string; origemReceita: OrigemReceita | null; }
export interface LancamentoPlanejadoRequest { descricao: string; tipo: "DESPESA" | "RECEITA" | "TRANSFERENCIA"; valor: number; vencimento: string; contaOrigemId?: string | null; contaDestinoId?: string | null; categoria?: string | null; grupoCategoria?: string | null; naturezaCategoria?: string | null; cartaoManualId?: string | null; marcacaoPlanejamento?: MarcacaoPlanejamento; origemReceita?: OrigemReceita | null; }
export interface ConsultaLancamentosResponse {
  itens: LancamentoPlanejadoResponse[];
  totalDeItens: number;
  totalDespesas: DinheiroStr;
  totalReceitas: DinheiroStr;
  saldoRealizado: DinheiroStr;
  saldoPrevisto: DinheiroStr;
  quantidadeAtrasados: number;
  pagina: number;
  tamanho: number;
  temMais: boolean;
}
export interface DashboardInicioResponse { analise: DashboardResponse; contasManuais: ContaManualResponse[]; pendencias: LancamentoPlanejadoResponse[]; }
export interface LancamentoDaPlanilhaResponse {
  id: string | null;
  descricao: string;
  valor: DinheiroStr;
  tipo: "ENTRADA" | "SAIDA";
  origem: string;
  usoDeCredito: string | null;
  editavel: boolean;
  categoria: string | null;
  grupoCategoria: string | null;
  naturezaCategoria: string | null;
  origemReceita: OrigemReceita | null;
}
export interface LancamentoDaPlanilhaRequest {
  descricao: string;
  tipo: "ENTRADA" | "SAIDA";
  valor: number;
  categoria?: string | null;
  grupoCategoria?: string | null;
  naturezaCategoria?: string | null;
  origemReceita?: OrigemReceita | null;
}
export interface DiaDaPlanilhaResponse { data: string; entrada: DinheiroStr; saida: DinheiroStr; diario: DinheiroStr; diarioSobrescrito: boolean; saldo: DinheiroStr; faixaSaldo: string; lancamentos: LancamentoDaPlanilhaResponse[]; observacao: string | null; }
export interface PlanilhaMesResponse { competencia: string; dias: DiaDaPlanilhaResponse[]; totalEntrada: DinheiroStr; totalSaida: DinheiroStr; totalDiario: DinheiroStr; saldoFinal: DinheiroStr; totalDeficitDisfarcado: DinheiroStr; transacoesEmAtencao: number; }
export interface SaldoInicialResponse { dataReferencia: string; valor: DinheiroStr; }

export interface AcaoPriorizadaDaPlanilhaResponse { categoria: string; descricao: string; economiaMensal: DinheiroStr; dor: number; }
export interface PriorizacaoResponse { competenciaInicio: string; acoesPrioritarias: AcaoPriorizadaDaPlanilhaResponse[]; economiaMensalFinalTotal: DinheiroStr; avisos: string[]; }
export interface SimulacaoDecisaoResponse {
  cenarioReal: PlanilhaMesResponse[];
  cenarioSimulado: PlanilhaMesResponse[];
  usoDeCreditoPrevisto: string | null;
  priorizacaoSeDeficit: PriorizacaoResponse | null;
}
export interface ConfirmarDecisaoResponse { lancamentosCriados: string[]; }

export interface PontoComparativoResponse {
  grupo: string;
  atual: PercentualStr;
  bom: PercentualStr | null;
  ideal: PercentualStr | null;
  ruim: PercentualStr | null;
}

export interface ProximoPassoContribuicaoResponse { competencia: string; percentualProposto: PercentualStr; valorProposto: DinheiroStr; }
export interface MetaContribuicaoResponse {
  nome: "DIZIMO" | "OFERTA";
  percentualAtual: PercentualStr;
  percentualAlvo: PercentualStr;
  valorMensalAtual?: DinheiroStr | null;
  proximoPassoSugerido?: ProximoPassoContribuicaoResponse | null;
  informacaoNecessaria?: string | null;
}

export interface MesDoRollupResponse { competencia: string; entrada: DinheiroStr; saida: DinheiroStr; taxaEconomia: PercentualStr; }

export interface ResultadoDaImportacaoResponse {
  transacoesLidas: number;
  totalDeDespesas: DinheiroStr;
  confiavel: boolean;
  avisos: string[];
  competenciasProcessadas: string[];
}

export interface TransacaoPropostaResponse { data: string; descricao: string; valor: DinheiroStr; }
export interface PropostaImportacaoResponse {
  formatoDetectado: string | null;
  reconhecido: boolean;
  reconciliacaoFechou: boolean;
  totalLido: DinheiroStr;
  transacoesEncontradas: number;
  amostra: TransacaoPropostaResponse[];
  avisos: string[];
  formatosDisponiveis: string[];
}
