package br.com.felipe.termometro.comparativo.domain;

import java.util.Map;

/**
 * RN-30 — liga o nome do item declarado no catálogo (custo fixo ou piso humano) ao grupo do
 * gráfico. Mapeamento nominal, não um campo no catálogo: são poucos itens, todos declarados pelo
 * próprio Felipe, e uma tabela nova no catálogo só para isto seria mais cerimônia do que o
 * problema pede agora. Se um nome não constar aqui, cai em {@link GrupoDoComparativo#OUTROS} —
 * nunca é descartado silenciosamente.
 *
 * <p><b>Fragilidade conhecida:</b> renomear um item no catálogo (ex. "Aluguel" → "Aluguel do
 * apto") faz esse item cair em OUTROS até este mapa ser atualizado. Aceitável para um catálogo de
 * poucas dezenas de itens revisado por uma pessoa só; se isso incomodar na prática, o próximo passo
 * natural é migrar para um campo {@code grupo} de verdade no catálogo.
 */
public final class MapeadorDeGrupo {

    private static final Map<String, GrupoDoComparativo> GRUPO_POR_NOME = Map.ofEntries(
            Map.entry("Aluguel", GrupoDoComparativo.MORADIA),
            Map.entry("Energia (Energisa)", GrupoDoComparativo.MORADIA),
            Map.entry("Água", GrupoDoComparativo.MORADIA),
            Map.entry("Internet (Tely)", GrupoDoComparativo.MORADIA),
            Map.entry("Imposto PJ (DARF)", GrupoDoComparativo.IMPOSTOS),
            Map.entry("Contador", GrupoDoComparativo.SERVICOS),
            Map.entry("Anthropic / Claude", GrupoDoComparativo.ASSINATURAS),
            Map.entry("Google One", GrupoDoComparativo.ASSINATURAS),
            Map.entry("Apple", GrupoDoComparativo.ASSINATURAS),
            Map.entry("Amazon Prime", GrupoDoComparativo.ASSINATURAS),
            Map.entry("iFood Club", GrupoDoComparativo.ASSINATURAS),
            Map.entry("Claro Flex", GrupoDoComparativo.ASSINATURAS),
            Map.entry("Mercado", GrupoDoComparativo.ALIMENTACAO),
            Map.entry("Comer fora", GrupoDoComparativo.ALIMENTACAO),
            Map.entry("Transporte por app", GrupoDoComparativo.TRANSPORTE),
            Map.entry("Lavanderia", GrupoDoComparativo.LAVANDERIA),
            Map.entry("Saúde/farmácia", GrupoDoComparativo.SAUDE),
            Map.entry("Lazer", GrupoDoComparativo.LAZER));

    private MapeadorDeGrupo() {
    }

    public static GrupoDoComparativo grupoDe(String nomeDoItem) {
        return GRUPO_POR_NOME.getOrDefault(nomeDoItem, GrupoDoComparativo.OUTROS);
    }

    public static GrupoDoComparativo grupoDe(String grupoDaCategoria, String nomeDaCategoria) {
        if (grupoDaCategoria != null) {
            try {
                return GrupoDoComparativo.valueOf(grupoDaCategoria);
            } catch (IllegalArgumentException ignorada) {
                // Grupos ainda não calibrados (compras, dívida, vestuário...) aparecem em Outros.
            }
        }
        return grupoDe(nomeDaCategoria);
    }
}
