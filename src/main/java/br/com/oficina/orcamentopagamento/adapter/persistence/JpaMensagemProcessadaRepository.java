package br.com.oficina.orcamentopagamento.adapter.persistence;

import br.com.oficina.orcamentopagamento.usecase.gateway.MensagemProcessadaRepository;
import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Idempotência por messageId; roda na mesma transação do comando. */
@Component
public class JpaMensagemProcessadaRepository implements MensagemProcessadaRepository {

  private final SpringDataMensagemProcessadaRepository jpa;

  JpaMensagemProcessadaRepository(SpringDataMensagemProcessadaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public boolean registrarSeNova(MensagemSaga mensagem) {
    if (jpa.existsById(mensagem.messageId())) {
      return false;
    }
    MensagemProcessadaJpaEntity e = new MensagemProcessadaJpaEntity();
    e.messageId = mensagem.messageId();
    e.tipo = mensagem.tipo().name();
    e.numeroOs = mensagem.numeroOs();
    e.correlationId = mensagem.correlationId();
    e.processadaEm = Instant.now();
    jpa.saveAndFlush(e);
    return true;
  }
}
