package br.com.oficina.orcamentopagamento.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import br.com.oficina.orcamentopagamento.usecase.gateway.OrcamentoRepository;
import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;
import br.com.oficina.orcamentopagamento.usecase.saga.TipoMensagemSaga;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

/** SQS do LocalStack: GERAR_ORCAMENTO consumido e ORCAMENTO_APROVADO publicado após o commit. */
@SpringBootTest
class SqsLocalStackIntegrationTest {

  private static final LocalStackContainer LOCALSTACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
          .withServices(LocalStackContainer.Service.SQS);
  private static final SqsClient SQS;
  private static final String OS_EVENTOS;
  private static final String COMANDOS;

  static {
    LOCALSTACK.start();
    SQS =
        SqsClient.builder()
            .endpointOverride(LOCALSTACK.getEndpoint())
            .region(Region.of(LOCALSTACK.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();
    OS_EVENTOS = SQS.createQueue(b -> b.queueName("os-eventos")).queueUrl();
    COMANDOS = SQS.createQueue(b -> b.queueName("orcamento-pagamento-comandos")).queueUrl();
  }

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add("oficina.mensageria.tipo", () -> "sqs");
    registry.add("oficina.mensageria.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
    registry.add("oficina.mensageria.sqs.region", LOCALSTACK::getRegion);
    registry.add("oficina.mensageria.sqs.os-eventos-url", () -> OS_EVENTOS);
    registry.add("oficina.mensageria.sqs.comandos-url", () -> COMANDOS);
    registry.add("oficina.mensageria.sqs.wait-time-seconds", () -> "1");
    registry.add("oficina.mensageria.sqs.poll-delay-ms", () -> "200");
  }

  @Autowired private OrcamentoService servico;
  @Autowired private OrcamentoRepository orcamentos;
  @Autowired private ObjectMapper mapper;

  @Test
  void comandoConsumidoEEventoPublicado() throws Exception {
    String os = "OS-SQS-" + UUID.randomUUID().toString().substring(0, 8);
    MensagemSaga comando =
        new MensagemSaga(
            UUID.randomUUID().toString(),
            TipoMensagemSaga.GERAR_ORCAMENTO,
            "corr-sqs",
            os,
            "oficina-os-service",
            Instant.now(),
            MensagemSaga.dados(
                "idCliente",
                30,
                "valorTotal",
                80,
                "itens",
                List.of(Map.of("descricao", "Freio", "quantidade", 2, "precoUnitario", 40))));
    String corpo = mapper.writeValueAsString(comando);
    SQS.sendMessage(b -> b.queueUrl(COMANDOS).messageBody(corpo));

    await().atMost(Duration.ofSeconds(30)).until(() -> orcamentos.atual(os).isPresent());
    servico.aprovar(os, 30L);
    List<Message> eventos =
        SQS.receiveMessage(b -> b.queueUrl(OS_EVENTOS).maxNumberOfMessages(10).waitTimeSeconds(5))
            .messages();
    MensagemSaga evento = mapper.readValue(eventos.get(0).body(), MensagemSaga.class);
    assertThat(evento.tipo()).isEqualTo(TipoMensagemSaga.ORCAMENTO_APROVADO);
    assertThat(evento.correlationId()).isEqualTo("corr-sqs");
    assertThat(evento.origem()).isEqualTo(MensagemSaga.ORIGEM);
  }
}
