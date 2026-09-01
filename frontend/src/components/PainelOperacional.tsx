import { formatarDespesa, formatarDinheiro } from "../format";
import type { ContaManualResponse, LancamentoPlanejadoResponse } from "../types";
import { CalendarClock, Landmark, TrendingUp } from "lucide-react";
import { IconeCategoria } from "./IconeCategoria";

export function PainelOperacional({ contas, pendencias }: { contas: ContaManualResponse[]; pendencias: LancamentoPlanejadoResponse[] }) {
  const pagar = pendencias.filter((item) => item.tipo === "DESPESA");
  const receber = pendencias.filter((item) => item.tipo === "RECEITA");
  return <section className="visao-grid visao-grid--operacional" aria-label="Contas e vencimentos">
    <article className="painel"><div className="painel__cabecalho"><div><p className="eyebrow">Minhas contas</p><h2>Saldo disponível</h2></div></div>
      {contas.length === 0 ? <p className="vazio">Cadastre sua primeira conta para ver o saldo consolidado.</p> : <ul className="lista-cartoes-home">{contas.map((conta) => <li key={conta.id}><span className="cartao-marca cartao-marca--conta"><Landmark size={16} /></span><div><strong>{conta.nome}</strong><small>{conta.tipo === "CORRENTE" ? "Conta manual" : "Poupança manual"}</small></div><b>{formatarDinheiro(conta.saldo)}</b></li>)}</ul>}
    </article>
    <article className="painel"><div className="painel__cabecalho"><div><p className="eyebrow">Contas a pagar</p><h2>Próximas pendências</h2></div><span>{pagar.length}</span></div>
      {pagar.length === 0 ? <p className="vazio">Nenhuma conta a pagar no período.</p> : <ul className="lista-cartoes-home">{pagar.slice(0, 4).map((item) => <li key={item.id}><IconeCategoria nome={item.categoria?.nome} tamanho={16} /><div><strong>{item.descricao}</strong><small>Venc. {new Date(`${item.vencimento}T12:00:00`).toLocaleDateString("pt-BR")}</small></div><b className="valor--despesa">{formatarDespesa(item.valor)}</b></li>)}</ul>}
    </article>
    <article className="painel"><div className="painel__cabecalho"><div><p className="eyebrow">Contas a receber</p><h2>Entradas previstas</h2></div><span>{receber.length}</span></div>
      {receber.length === 0 ? <p className="vazio"><TrendingUp size={18} /> Nenhuma entrada pendente no período.</p> : <ul className="lista-cartoes-home">{receber.slice(0, 4).map((item) => <li key={item.id}><span className="cartao-marca cartao-marca--receita"><CalendarClock size={16} /></span><div><strong>{item.descricao}</strong><small>Venc. {new Date(`${item.vencimento}T12:00:00`).toLocaleDateString("pt-BR")}</small></div><b>{formatarDinheiro(item.valor)}</b></li>)}</ul>}
    </article>
  </section>;
}
