import type { DinheiroStr, PercentualStr } from "./types";

const FORMATO_BRL = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

/** Máscara de caixa registradora: cada novo dígito desloca os centavos para a esquerda. */
export function formatarEntradaDeDinheiro(bruto: string): string {
  const digitos = bruto.replace(/\D/g, "").replace(/^0+(?=\d)/, "");
  const centavos = BigInt(digitos || "0");
  return FORMATO_BRL.format(Number(centavos) / 100);
}

/** Converte o valor mascarado para número decimal aceito pela API; vazio/zero é inválido. */
export function valorDaEntradaDeDinheiro(mascarado: string): number | null {
  const digitos = mascarado.replace(/\D/g, "");
  if (!digitos) return null;
  const centavos = BigInt(digitos);
  return centavos > 0n ? Number(centavos) / 100 : null;
}

/** "1234.56" -> "R$ 1.234,56". Nunca usa Number() pra decisão de negócio, só pra exibição. */
export function formatarDinheiro(valor: DinheiroStr): string {
  return FORMATO_BRL.format(Number(valor));
}

/** Exibe uma saída sempre com o sinal contábil negativo, mesmo quando a API envia o módulo. */
export function formatarDespesa(valor: DinheiroStr): string {
  return formatarDinheiro(valor.startsWith("-") ? valor : `-${valor}`);
}

/** "0.285714" (fração) -> "28,6%" */
export function formatarPercentual(fracao: PercentualStr, casas = 1): string {
  const pontos = Number(fracao) * 100;
  return (
    pontos.toLocaleString("pt-BR", {
      minimumFractionDigits: casas,
      maximumFractionDigits: casas,
    }) + "%"
  );
}

/** "2026-09" -> "setembro/2026" */
export function formatarCompetencia(competencia: string | null): string {
  if (!competencia) return "—";
  const [ano, mes] = competencia.split("-").map(Number);
  const data = new Date(Date.UTC(ano, mes - 1, 1));
  const nome = data.toLocaleDateString("pt-BR", { month: "long", timeZone: "UTC" });
  return `${nome}/${ano}`;
}

export function competenciaAtual(): string {
  const hoje = new Date();
  const mes = String(hoje.getMonth() + 1).padStart(2, "0");
  return `${hoje.getFullYear()}-${mes}`;
}

/** "2026-08" -> "2026-07" (mês anterior, vira o ano em janeiro). Usado no fallback de pré-preenchimento da renda. */
export function competenciaAnterior(competencia: string): string {
  const [ano, mes] = competencia.split("-").map(Number);
  const data = new Date(Date.UTC(ano, mes - 1, 1));
  data.setUTCMonth(data.getUTCMonth() - 1);
  const mesAnterior = String(data.getUTCMonth() + 1).padStart(2, "0");
  return `${data.getUTCFullYear()}-${mesAnterior}`;
}

function paraCentavos(dinheiro: DinheiroStr): bigint {
  const negativo = dinheiro.startsWith("-");
  const semSinal = negativo ? dinheiro.slice(1) : dinheiro;
  const [reais, centavosBrutos = ""] = semSinal.split(".");
  const centavos = (centavosBrutos + "00").slice(0, 2);
  const total = BigInt(reais || "0") * 100n + BigInt(centavos);
  return negativo ? -total : total;
}

/**
 * Soma valores de Dinheiro sem passar por `number`/float — `0.1 + 0.2 !== 0.3` em ponto
 * flutuante é exatamente o tipo de erro que o `Dinheiro` do backend (BigDecimal, HALF_EVEN)
 * existe pra evitar; somar aqui com `Number()` reintroduziria o mesmo risco só que no front.
 */
export function somarDinheiro(valores: DinheiroStr[]): DinheiroStr {
  const totalCentavos = valores.reduce((soma, v) => soma + paraCentavos(v), 0n);
  const negativo = totalCentavos < 0n;
  const absoluto = negativo ? -totalCentavos : totalCentavos;
  const reais = absoluto / 100n;
  const centavos = absoluto % 100n;
  return `${negativo ? "-" : ""}${reais}.${centavos.toString().padStart(2, "0")}`;
}

const REGEX_DECIMAL = /^-?\d+([.,]\d+)?$/;

/**
 * Normaliza um valor decimal digitado em formulário (Dinheiro ou fração de Percentual) para a
 * string canônica que o backend espera (separador decimal `.`, sem separador de milhar).
 * Aceita vírgula OU ponto como separador decimal — não aceita separador de milhar (ex.:
 * "1.234,56"); para um app pessoal de uso único isso é aceitável, não vale a complexidade de
 * detectar o formato. Retorna `null` se a entrada não for um decimal válido, pra quem chama
 * decidir como sinalizar o erro no formulário. O valor retornado é a própria string tratada —
 * nunca passa por `Number()`, então não há risco de perda de precisão aqui.
 */
export function normalizarDecimal(bruto: string): string | null {
  const valor = bruto.trim();
  if (!REGEX_DECIMAL.test(valor)) return null;
  return valor.replace(",", ".");
}
