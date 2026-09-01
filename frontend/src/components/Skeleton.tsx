export function Skeleton() {
  return (
    <div className="skeleton" role="status" aria-live="polite" aria-label="Carregando dashboard">
      {[0, 1, 2].map((coluna) => (
        <div key={coluna} className="skeleton__coluna">
          <div className="skeleton__bloco skeleton__bloco--titulo" />
          {[0, 1, 2].map((cartao) => (
            <div key={cartao} className="skeleton__bloco skeleton__bloco--cartao" />
          ))}
        </div>
      ))}
    </div>
  );
}
