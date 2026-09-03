import { apiFetch, limpaSessao, salvaSessao, sessaoValida } from "./api";

/**
 * Sessão do login único do app (ver back-end `auth/` + `config/SecurityConfig`). Token e validade
 * ficam no `localStorage` (ver `api.ts`) — sobrevivem a um recarregar de página, mas são só deste
 * navegador.
 */
interface LoginResponse {
  token: string;
  expiraEm: string;
}

export async function login(usuario: string, senha: string): Promise<void> {
  const resposta = await apiFetch<LoginResponse>("/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ usuario, senha }),
  });
  salvaSessao(resposta.token, resposta.expiraEm);
}

export function logout(): void {
  limpaSessao();
}

export { sessaoValida as tokenValido };
