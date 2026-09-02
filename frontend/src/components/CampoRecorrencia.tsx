import { Repeat2 } from "lucide-react";

/**
 * Marca um lançamento como recorrente (mesmo valor, todo mês, no dia fixo escolhido) — usado nas
 * duas telas de lançamento (rápido e planilha). Independente das tags Custo fixo/Piso
 * humano/Receita recorrente: essas classificam o orçamento, isto controla se o lançamento se
 * repete sozinho nos meses seguintes.
 *
 * Uma vez que o lançamento já pertence a uma série (`jaEhSerie`), não dá pra "desmarcar" por
 * aqui — só editar o dia; tirar da série é decisão grande demais pra um checkbox.
 */
export function CampoRecorrencia({
  recorrente,
  dia,
  jaEhSerie,
  onChangeRecorrente,
  onChangeDia,
}: {
  recorrente: boolean;
  dia: number;
  jaEhSerie: boolean;
  onChangeRecorrente: (valor: boolean) => void;
  onChangeDia: (valor: number) => void;
}) {
  return (
    <div className="campo-recorrencia">
      <label className="form__checkbox">
        <input
          type="checkbox"
          checked={recorrente}
          disabled={jaEhSerie}
          onChange={(evento) => onChangeRecorrente(evento.target.checked)}
        />
        <Repeat2 size={15} />
        {jaEhSerie ? "Recorrente todo mês" : "Recorrente, todo mês"}
      </label>
      {recorrente && (
        <div className="form__campo-inline campo-recorrencia__dia">
          <label htmlFor="campo-recorrencia-dia">Todo dia</label>
          <input
            id="campo-recorrencia-dia"
            type="number"
            min={1}
            max={31}
            value={dia}
            onChange={(evento) => {
              const numero = Number(evento.target.value);
              if (Number.isNaN(numero)) return;
              onChangeDia(Math.min(31, Math.max(1, Math.trunc(numero))));
            }}
          />
        </div>
      )}
    </div>
  );
}
