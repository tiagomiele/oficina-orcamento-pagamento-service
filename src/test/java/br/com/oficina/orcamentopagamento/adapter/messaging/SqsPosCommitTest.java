package br.com.oficina.orcamentopagamento.adapter.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;
import br.com.oficina.orcamentopagamento.usecase.saga.TipoMensagemSaga;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class SqsPosCommitTest {

  @Test
  void dentroDeTransacaoSoEnviaDepoisDoCommit() {
    SqsClient sqs = mock(SqsClient.class);
    SqsMensageriaGateway gateway =
        new SqsMensageriaGateway(
            sqs, new ObjectMapper().registerModule(new JavaTimeModule()), "http://fila");
    TransactionSynchronizationManager.initSynchronization();
    try {
      gateway.enviar(MensagemSaga.evento(TipoMensagemSaga.ORCAMENTO_APROVADO, "OS-1", "c", null));
      verify(sqs, never()).sendMessage(any(SendMessageRequest.class));
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(sqs).sendMessage(any(SendMessageRequest.class));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
