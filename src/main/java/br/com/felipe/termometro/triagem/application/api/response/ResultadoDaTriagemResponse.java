package br.com.felipe.termometro.triagem.application.api.response;

import java.util.Map;

/**
 * @param porEtiqueta quantas transações receberam cada etiqueta nesta execução
 */
public record ResultadoDaTriagemResponse(String competencia, int analisadas, int triadas,
                                         Map<String, Integer> porEtiqueta) {
}
