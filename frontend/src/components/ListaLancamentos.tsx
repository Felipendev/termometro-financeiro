import { Fragment, type ReactNode } from "react";
import { HeartHandshake, Pencil, Repeat2, Trash2 } from "lucide-react";
import { formatarDinheiro } from "../format";
import type { LancamentoPlanejadoResponse } from "../types";
import { IconeCategoria } from "./IconeCategoria";

const ROTULOS_ORIGEM_RECEITA = {
  SALARIO: "Salário",
  INVESTIMENTO: "Investimento",
  EMPRESTIMO: "Empréstimo",
} as const;

const ROTULOS_MARCACAO = {
  CUSTO_FIXO: "Custo fixo",
  PISO_HUMANO: "Piso humano",
  RECEITA_RECORRENTE: "Receita recorrente",
} as const;

export function dataLegivel(data: string) {
  const [ano, mes, dia] = data.split("-");
  return `${dia}/${mes}/${ano}`;
}

export function statusLegivel(item: LancamentoPlanejadoResponse, hojeIso: string) {
  if (item.status === "PENDENTE" && item.vencimento < hojeIso) return "Atrasado";
  if (item.status === "LIQUIDADO") return item.tipo === "RECEITA" ? "Recebido" : "Pago";
  if (item.status === "CANCELADO") return "Cancelado";
  return "Pendente";
}

/**
 * Lista de lançamentos compartilhada entre Lançamentos (edição completa) e Relatórios > Entradas
 * e saídas (só visualização). `origemDo`/`renderAcoes` ausentes = modo leitura: sem coluna de
 * conta/status nem de ações: editar/excluir só aparecem se os callbacks forem passados.
 */
export function ListaLancamentos({
  itens,
  origemDo,
  aoEditar,
  aoExcluir,
  renderAcoes,
}: {
  itens: LancamentoPlanejadoResponse[];
  origemDo?: (item: LancamentoPlanejadoResponse) => string;
  aoEditar?: (item: LancamentoPlanejadoResponse) => void;
  aoExcluir?: (id: string, descricao: string) => void;
  renderAcoes?: (item: LancamentoPlanejadoResponse) => ReactNode;
}) {
  const hojeIso = new Date().toLocaleDateString("en-CA");
  return (
    <ul className="lancamentos__lista">
      {itens.map((item, indice) => (
        <Fragment key={item.id}>
          {(indice === 0 || itens[indice - 1].vencimento !== item.vencimento) && (
            <li className="lancamentos__data">{dataLegivel(item.vencimento)}</li>
          )}
          <li className={item.status === "PENDENTE" && item.vencimento < hojeIso ? "lancamento--atrasado" : undefined}>
            <div className="lancamento__identidade">
              <IconeCategoria nome={item.categoria?.nome} />
              <div>
                <span className="lancamento__titulo">
                  <strong>{item.descricao}</strong>
                  {(aoEditar || aoExcluir) && item.editavel && item.status === "PENDENTE" && (
                    <>
                      {aoEditar && (
                        <button className="botao--edicao-inline" aria-label={`Editar ${item.descricao}`} title="Editar" onClick={() => aoEditar(item)}>
                          <Pencil size={14} />
                        </button>
                      )}
                      {aoExcluir && (
                        <button className="botao--edicao-inline botao--edicao-inline-perigo" aria-label={`Excluir ${item.descricao}`} title="Excluir definitivamente" onClick={() => aoExcluir(item.id, item.descricao)}>
                          <Trash2 size={14} />
                        </button>
                      )}
                    </>
                  )}
                </span>
                {item.marcacaoPlanejamento && item.marcacaoPlanejamento !== "NENHUMA" && (
                  <span className="marcacao-badge">
                    {item.marcacaoPlanejamento === "PISO_HUMANO" ? <HeartHandshake size={12} /> : <Repeat2 size={12} />}
                    {ROTULOS_MARCACAO[item.marcacaoPlanejamento]}
                  </span>
                )}
                <small>
                  {item.tipo === "RECEITA"
                    ? (item.origemReceita ? ROTULOS_ORIGEM_RECEITA[item.origemReceita] : "Origem não informada")
                    : item.categoria?.nome ?? "Sem categoria"} · {item.vencimento} · {item.status}
                </small>
              </div>
            </div>
            <b className={item.tipo === "DESPESA" ? "valor--despesa" : item.tipo === "RECEITA" ? "valor--receita" : ""}>
              {item.tipo === "DESPESA" ? "-" : item.tipo === "RECEITA" ? "+" : ""}{formatarDinheiro(item.valor)}
            </b>
            {origemDo && (
              <span className="lancamento__origem">
                {origemDo(item)}
                <small>{statusLegivel(item, hojeIso)}</small>
                {item.origem !== "MANUAL" && <small>Importado via {item.origem}</small>}
              </span>
            )}
            {renderAcoes && <div className="lancamentos__acoes">{renderAcoes(item)}</div>}
          </li>
        </Fragment>
      ))}
    </ul>
  );
}
