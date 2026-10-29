package br.com.oficina.orcamentopagamento.usecase.gateway;

import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;

/** Publica os eventos da Saga na fila os-eventos do OS Service. */
public interface MensageriaGateway {
  void enviar(MensagemSaga mensagem);
}
