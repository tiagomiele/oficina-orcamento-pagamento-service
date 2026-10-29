package br.com.oficina.orcamentopagamento.adapter.persistence;

import br.com.oficina.orcamentopagamento.usecase.gateway.MensagemProcessadaRepository;
import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Idempotência por messageId; roda na mesma transação do comando. */
@Component
public class JpaMensagemProcessadaRepository implements MensagemProcessadaRepository {

  @PersistenceContext private EntityManager em;

  @Override
  public boolean registrarSeNova(MensagemSaga mensagem) {
    if (em.find(MensagemProcessadaJpaEntity.class, mensagem.messageId()) != null) {
      return false;
    }
    MensagemProcessadaJpaEntity e = new MensagemProcessadaJpaEntity();
    e.messageId = mensagem.messageId();
    e.tipo = mensagem.tipo().name();
    e.numeroOs = mensagem.numeroOs();
    e.correlationId = mensagem.correlationId();
    e.processadaEm = Instant.now();
    em.persist(e);
    em.flush();
    return true;
  }
}
