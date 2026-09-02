package br.com.felipe.termometro.lancamentoplanejado.domain;

/** Ao editar uma ocorrência que pertence a uma série de recorrência: só esta, ou esta e toda
 *  ocorrência PENDENTE futura da mesma série. Ignorado quando o lançamento não tem série. */
public enum EscopoEdicaoRecorrencia {
    ESTA,
    ESTA_E_FUTURAS
}
