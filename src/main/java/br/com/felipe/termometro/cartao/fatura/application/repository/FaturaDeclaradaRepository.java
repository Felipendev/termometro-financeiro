package br.com.felipe.termometro.cartao.fatura.application.repository;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Map;

public interface FaturaDeclaradaRepository {
    Map<String, Dinheiro> buscaPorCompetencia(Competencia competencia);
    void salva(String referencia, String nome, Competencia competencia, Dinheiro valor);
}
