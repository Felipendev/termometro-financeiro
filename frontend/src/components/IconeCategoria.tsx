import { ArrowLeftRight, Bike, Car, CircleDollarSign, CircleHelp, Croissant, Gamepad2, GraduationCap, HeartPulse, House, MoreHorizontal, Percent, ShoppingBag, ShoppingCart, Utensils, type LucideIcon } from "lucide-react";

const ICONES: Record<string, LucideIcon> = {
  "Alimentação": Utensils, "Assinaturas e serviços": CircleDollarSign, Casa: House,
  Compras: ShoppingBag, Educação: GraduationCap, "Impostos e Taxas": Percent,
  "Lazer e hobbies": Gamepad2, Mercado: ShoppingCart, Saúde: HeartPulse,
  Transporte: Car, Outros: MoreHorizontal,
  Assinatura: CircleDollarSign, Delivery: Bike, Padaria: Croissant,
  "Transporte App": Car, "Transferencia Pessoal": ArrowLeftRight,
  "Nao Identificada": CircleHelp,
};

function chaveLegivel(nome?: string | null) {
  if (!nome) return "";
  const normalizado = nome.toLocaleLowerCase("pt-BR").replaceAll("_", " ");
  return normalizado.replace(/(^|\s)\p{L}/gu, (letra) => letra.toLocaleUpperCase("pt-BR"));
}

export function IconeCategoria({ nome, tamanho = 18, cor }: { nome?: string | null; tamanho?: number; cor?: string }) {
  const Icone = ICONES[chaveLegivel(nome)] ?? MoreHorizontal;
  return <span className="icone-categoria" style={cor ? { color: cor, backgroundColor: `${cor}18` } : undefined} aria-hidden="true"><Icone size={tamanho} strokeWidth={1.9} /></span>;
}
