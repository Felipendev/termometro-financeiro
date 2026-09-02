import type { EscopoEdicaoRecorrencia } from "../types";

/** Confirmação exibida ao salvar a edição de um lançamento que já pertence a uma série de
 *  recorrência — pergunta se a mudança vale só pra esta ocorrência ou pra série inteira daqui
 *  pra frente. Usada nas duas telas de edição de lançamento. */
export function EscolhaEscopoRecorrencia({
  onEscolher,
  onCancelar,
}: {
  onEscolher: (escopo: EscopoEdicaoRecorrencia) => void;
  onCancelar: () => void;
}) {
  return (
    <div className="escolha-escopo" role="alertdialog" aria-label="Aplicar edição a">
      <p>Este lançamento é recorrente. Aplicar a alteração a:</p>
      <div className="form__acoes">
        <button type="button" className="botao--secundario" onClick={() => onEscolher("ESTA")}>
          Só este lançamento
        </button>
        <button type="button" onClick={() => onEscolher("ESTA_E_FUTURAS")}>
          Este e todos os futuros
        </button>
        <button type="button" className="botao--texto" onClick={onCancelar}>
          Cancelar
        </button>
      </div>
    </div>
  );
}
