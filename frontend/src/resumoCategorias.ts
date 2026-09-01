import { somarDinheiro } from "./format";
import type { LancamentoPlanejadoResponse, ResumoDeCategoriaResponse } from "./types";

function chave(nome: string) {
  return nome.normalize("NFD").replace(/\p{Diacritic}/gu, "").replaceAll("_", " ").trim().toLocaleUpperCase("pt-BR");
}

export function nomeLegivelDaCategoria(nome: string) {
  return nome.toLocaleLowerCase("pt-BR").replaceAll("_", " ").replace(/(^|\s)\p{L}/gu, (letra) => letra.toLocaleUpperCase("pt-BR"));
}

function totalClassificado(categoria: ResumoDeCategoriaResponse) {
  return somarDinheiro([categoria.totalAzul, categoria.totalAmarelo, categoria.totalVermelho, categoria.totalNaoTriada]);
}

/** Une realizados classificados e pendências; liquidados não são duplicados porque só entram pelo resumo. */
export function combinaCategorias(resumos: ResumoDeCategoriaResponse[], pendencias: LancamentoPlanejadoResponse[], competencia: string) {
  const totais = new Map<string, { nome: string; total: string }>();
  for (const resumo of resumos) {
    const id = chave(resumo.categoria);
    if (id.includes("TRANSFERENCIA")) continue;
    totais.set(id, { nome: nomeLegivelDaCategoria(resumo.categoria), total: totalClassificado(resumo) });
  }
  for (const item of pendencias) {
    if (item.tipo !== "DESPESA" || !item.vencimento.startsWith(competencia) || !item.categoria?.nome) continue;
    const id = chave(item.categoria.nome);
    const atual = totais.get(id);
    totais.set(id, { nome: nomeLegivelDaCategoria(item.categoria.nome), total: somarDinheiro([atual?.total ?? "0.00", item.valor]) });
  }
  return [...totais.values()].filter((item) => Number(item.total) > 0).sort((a, b) => Number(b.total) - Number(a.total));
}
