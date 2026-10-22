package br.com.oficina.orcamentopagamento.usecase.gateway;

import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;

public interface MensagemProcessadaRepository {

  /** Registra o messageId; retorna false se a mensagem já foi processada. */
  boolean registrarSeNova(MensagemSaga mensagem);
}
