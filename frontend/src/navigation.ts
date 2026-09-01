export type AbaPrincipal = "dashboard" | "lancamentos" | "planilha" | "relatorios";

export const NAVEGACAO_PRINCIPAL: ReadonlyArray<{ aba: AbaPrincipal; rotulo: string }> = [
  { aba: "dashboard", rotulo: "Visão geral" },
  { aba: "lancamentos", rotulo: "Lançamentos" },
  { aba: "planilha", rotulo: "Planilha" },
  { aba: "relatorios", rotulo: "Relatórios" },
];
