package br.com.felipe.termometro.classificacao.domain;

import static br.com.felipe.termometro.classificacao.domain.GrupoDeCategoria.*;
import static br.com.felipe.termometro.classificacao.domain.Natureza.*;
import static br.com.felipe.termometro.classificacao.domain.TipoDeRegra.DESCRICAO;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo padrão, calibrado nas faturas reais de junho a agosto de 2026 (Itaú, Nubank, PicPay).
 *
 * <p>Não é uma lista genérica de categorias: cada padrão daqui casou com transação de verdade, e a
 * ordem importa. As regras mais específicas vêm primeiro porque a primeira que casar decide —
 * {@code IFD\*} tem que ser avaliado antes de "restaurante", senão todo delivery vira refeição
 * presencial e o padrão temporal (RN-13) mede a coisa errada.
 */
public final class CatalogoDeRegrasPadrao {

    // ---------------------------------------------------------------- categorias
    public static final Categoria ALUGUEL = new Categoria("ALUGUEL", MORADIA, FIXO);
    public static final Categoria CONTAS_DE_CASA = new Categoria("CONTAS_DE_CASA", MORADIA, FIXO);
    public static final Categoria LAVANDERIA = new Categoria("LAVANDERIA", SERVICOS, VARIAVEL);
    public static final Categoria IMPOSTO_PJ = new Categoria("IMPOSTO_PJ", IMPOSTOS, FIXO);
    public static final Categoria CONTADOR = new Categoria("CONTADOR", SERVICOS, FIXO);
    public static final Categoria INTERNET = new Categoria("INTERNET", ASSINATURAS, FIXO);
    public static final Categoria ASSINATURA = new Categoria("ASSINATURA", ASSINATURAS, FIXO);
    public static final Categoria MERCADO = new Categoria("MERCADO", ALIMENTACAO, VARIAVEL);
    public static final Categoria PADARIA = new Categoria("PADARIA", ALIMENTACAO, VARIAVEL);
    public static final Categoria RESTAURANTE = new Categoria("RESTAURANTE", ALIMENTACAO, VARIAVEL);
    public static final Categoria DELIVERY = new Categoria("DELIVERY", ALIMENTACAO, VARIAVEL);
    public static final Categoria TRANSPORTE_APP = new Categoria("TRANSPORTE_APP", TRANSPORTE, VARIAVEL);
    public static final Categoria FARMACIA = new Categoria("FARMACIA", SAUDE, VARIAVEL);
    public static final Categoria ACADEMIA = new Categoria("ACADEMIA", SAUDE, VARIAVEL);
    public static final Categoria LAZER_CAT = new Categoria("LAZER", GrupoDeCategoria.LAZER, VARIAVEL);
    public static final Categoria VESTUARIO_CAT = new Categoria("VESTUARIO", VESTUARIO, VARIAVEL);
    public static final Categoria COMPRAS_ONLINE = new Categoria("COMPRAS_ONLINE", COMPRAS, VARIAVEL);
    public static final Categoria PAGAMENTO_DE_FATURA =
            new Categoria("PAGAMENTO_DE_FATURA", DIVIDA, NAO_E_GASTO);
    public static final Categoria ROLAGEM_DE_DIVIDA =
            new Categoria("ROLAGEM_DE_DIVIDA", DIVIDA, NAO_E_GASTO);
    public static final Categoria EMPRESTIMO = new Categoria("EMPRESTIMO", DIVIDA, NAO_E_GASTO);
    public static final Categoria TRANSFERENCIA_PESSOAL =
            new Categoria("TRANSFERENCIA_PESSOAL", TRANSFERENCIA, VARIAVEL);

    private CatalogoDeRegrasPadrao() {
    }

    public static List<RegraDeCategorizacao> regras() {
        List<RegraDeCategorizacao> regras = new ArrayList<>();
        int prioridade = 0;

        // --- o que NÃO é consumo. Precisa vir antes de tudo: "PIX Nu Pagamentos SA" contém
        // "Pagamentos" e casaria com meia dúzia de padrões mais abaixo.
        regras.add(regra(prioridade++, "^PAGAMENTO( RECEBIDO| DE FATURA| VIA CONTA)?$", PAGAMENTO_DE_FATURA));
        regras.add(regra(prioridade++, "NU PAGAMENTOS|PICPAY\\s*\\*?\\s*PAGAMENTO", ROLAGEM_DE_DIVIDA));
        regras.add(regra(prioridade++, "EMPRESTIMO|CREDITO PESSOAL", EMPRESTIMO));

        // --- fixos identificáveis
        regras.add(regra(prioridade++, "MINISTERIO DA FAZEN|RECEITA FEDERAL|DARF|SIMPLES NACIONAL", IMPOSTO_PJ));
        regras.add(regra(prioridade++, "ENERGISA|CAGEPA|SANEAMENTO", CONTAS_DE_CASA));
        regras.add(regra(prioridade++, "SITECNET|TELY|CLARO|VIVO|TIM ", INTERNET));
        regras.add(regra(prioridade++,
                "APPLE\\.COM|GOOGLE ONE|ANTHROPIC|CLAUDE|AMAZON PRIME|RAILWAY|IFOOD CLUB|SPOTIFY|NETFLIX",
                ASSINATURA));

        // --- Smartblue é lavanderia por máquina, não assinatura. Cada cobrança de ~R$ 15 é uma
        // máquina; 4 num dia são duas de lavar e duas de secar. Variável com piso, não fixo.
        regras.add(regra(prioridade++, "SMARTBLUE", LAVANDERIA));

        // --- alimentação. Delivery antes de restaurante, senão IFD*RESTAURANTE vira presencial.
        regras.add(regra(prioridade++, "^IFD\\*|IFOOD|RAPPI|UBER ?EATS", DELIVERY));
        regras.add(regra(prioridade++,
                "SUPERMERCADO|MERCADINHO|MERCADO|CARREFOUR|BEM MAIS|FRUTOS DE GOIAS|ATACAD", MERCADO));
        regras.add(regra(prioridade++, "PANIFICADORA|PADARIA|BESSA PAO|EMPADINHA|PASTEL", PADARIA));
        regras.add(regra(prioridade++,
                "RESTAURANTE|KFC|MINERIM|CANTINHO|LAMPIAO|POKE|BURGER|PIZZA|CAFE|LANCHONETE", RESTAURANTE));

        regras.add(regra(prioridade++, "^UBER|^99\\*|99POP|CABIFY", TRANSPORTE_APP));
        regras.add(regra(prioridade++, "PAGUE MENOS|DROGA|FARMACIA|DROGASIL", FARMACIA));
        regras.add(regra(prioridade++, "AQUAZUL|ACADEMIA|SMART ?FIT|GYM", ACADEMIA));
        regras.add(regra(prioridade++,
                "CINEPOLIS|CINEMA|INGRESSO\\.COM|SUPERCELL|STEAM|AIRBNB|JIM\\.COM", LAZER_CAT));
        regras.add(regra(prioridade++, "NOHA SHOES|ADIDAS|NIKE|FISIA|RENNER|RIACHUELO|PAYU", VESTUARIO_CAT));
        regras.add(regra(prioridade++,
                "AMAZON|MERCADOLIVRE|PICHAU|SHOPEE|ALIEXPRESS|ORANGE SHOPPING|MAGAZINE", COMPRAS_ONLINE));

        // --- Pix para pessoa física: sem outra pista, é transferência pessoal e conta como
        // variável. O contador (RN-17.1) só é reconhecido depois que o usuário confirma quem é.
        regras.add(regra(prioridade, "^PIX ", TRANSFERENCIA_PESSOAL));
        return List.copyOf(regras);
    }

    private static RegraDeCategorizacao regra(int prioridade, String padrao, Categoria categoria) {
        return RegraDeCategorizacao.doSistema(prioridade, DESCRICAO, padrao, categoria);
    }
}
