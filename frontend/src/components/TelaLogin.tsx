import { useState } from "react";
import { ApiError } from "../api";
import { login } from "../auth";

/** Gate de login único do app — ver `App.tsx` (renderizado no lugar de `.shell` até autenticar). */
export function TelaLogin({ aoEntrar }: { aoEntrar: () => void }) {
  const [usuario, setUsuario] = useState("");
  const [senha, setSenha] = useState("");
  const [entrando, setEntrando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function entrar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro(null);
    setEntrando(true);
    try {
      await login(usuario, senha);
      aoEntrar();
    } catch (erroLogin: unknown) {
      setErro(erroLogin instanceof ApiError ? erroLogin.message : "Não foi possível falar com o servidor.");
    } finally {
      setEntrando(false);
    }
  }

  return (
    <div className="tela-login">
      <section className="cartao tela-login__cartao">
        <div className="marca tela-login__marca">
          <span className="marca__simbolo">T</span>
          <span>termômetro</span>
        </div>
        <h1>Entrar</h1>
        <form className="form" onSubmit={entrar}>
          <div className="form__campo">
            <label htmlFor="login-usuario">Usuário</label>
            <input
              id="login-usuario"
              type="text"
              autoComplete="username"
              autoFocus
              value={usuario}
              onChange={(e) => setUsuario(e.target.value)}
            />
          </div>
          <div className="form__campo">
            <label htmlFor="login-senha">Senha</label>
            <input
              id="login-senha"
              type="password"
              autoComplete="current-password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
            />
          </div>
          {erro && <p className="form__erro">{erro}</p>}
          <div className="form__acoes">
            <button type="submit" disabled={entrando || !usuario || !senha}>
              {entrando ? "Entrando…" : "Entrar"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
