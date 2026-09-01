package br.com.felipe.termometro.naogasto.application.repository;

import br.com.felipe.termometro.naogasto.domain.LancamentoParaConciliar;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Porta de saída da conciliação (RN-03). Lê e escreve na mesma tabela {@code transacao}. */
public interface NaoGastoRepository {

    /** Lançamentos não ignorados no intervalo, com identidade e conta de origem. */
    List<LancamentoParaConciliar> buscaLancamentos(LocalDate desde, LocalDate ate);

    /** Marca os ids como {@code ignorada = true}. Devolve quantos de fato foram encontrados. */
    int marcaIgnoradas(Set<UUID> ids);
}
