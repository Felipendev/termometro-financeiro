import { CreditCard } from "lucide-react";

const LOGOS: { termos: string[]; arquivo: string }[] = [
  { termos: ["nubank", "nu bank"], arquivo: "nubank.svg" },
  { termos: ["itau", "itaú"], arquivo: "itau.svg" },
  { termos: ["picpay", "pic pay"], arquivo: "picpay.svg" },
  { termos: ["inter"], arquivo: "inter.svg" },
  { termos: ["santander"], arquivo: "santander.svg" },
  { termos: ["mercado pago", "mercadopago"], arquivo: "mercado-pago.svg" },
  { termos: ["c6"], arquivo: "c6.svg" },
];

export function LogoCartao({ nome, tamanho = 22 }: { nome: string; tamanho?: number }) {
  const normalizado = nome.toLocaleLowerCase("pt-BR");
  const logo = LOGOS.find((item) => item.termos.some((termo) => normalizado.includes(termo)));
  if (!logo) return <CreditCard size={Math.min(tamanho, 20)} aria-hidden="true" />;
  return <img className="logo-cartao" src={`/logos-cartoes/${logo.arquivo}`} alt="" width={tamanho} height={tamanho} />;
}
