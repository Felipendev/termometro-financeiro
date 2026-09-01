package br.com.felipe.termometro.classificacao.application.repository;

import br.com.felipe.termometro.classificacao.domain.Categoria;
import br.com.felipe.termometro.classificacao.domain.RegraDeCategorizacao;
import java.util.List;

public interface RegraDeCategorizacaoRepository {

    /** Só as regras do usuário e as aprendidas — o catálogo do sistema é código. */
    List<RegraDeCategorizacao> buscaRegrasDoUsuario();

    RegraDeCategorizacao salva(RegraDeCategorizacao regra);

    /** Usada quando o usuário classifica uma transação e manda aplicar ao grupo (RN-12). */
    RegraDeCategorizacao aprende(String estabelecimento, Categoria categoria);
}
