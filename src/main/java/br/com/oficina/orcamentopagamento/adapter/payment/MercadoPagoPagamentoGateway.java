package br.com.oficina.orcamentopagamento.adapter.payment;

import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Mercado Pago Checkout Pro: cria a preferência com {@code external_reference} = número da OS e
 * consulta o pagamento informado pelo webhook em {@code /v1/payments/{id}}.
 */
@Component
@ConditionalOnProperty(name = "oficina.pagamento.provedor", havingValue = "mercadopago")
public class MercadoPagoPagamentoGateway implements PagamentoGateway {

  private static final Set<String> RECUSADOS =
      Set.of("rejected", "cancelled", "refunded", "charged_back");

  private final RestClient rest;
  private final MercadoPagoProperties properties;

  public MercadoPagoPagamentoGateway(RestClient.Builder builder, MercadoPagoProperties properties) {
    if (properties.accessToken() == null || properties.accessToken().isBlank()) {
      throw new IllegalStateException("MERCADOPAGO_ACCESS_TOKEN não configurado");
    }
    this.properties = properties;
    this.rest =
        builder
            .baseUrl(properties.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
            .build();
  }

  @Override
  public String provedor() {
    return "mercadopago";
  }

  @Override
  public Cobranca criarCobranca(String numeroOs, BigDecimal valor, String descricao) {
    Map<String, Object> corpo = new LinkedHashMap<>();
    corpo.put(
        "items",
        List.of(
            Map.of("title", descricao, "quantity", 1, "unit_price", valor, "currency_id", "BRL")));
    corpo.put("external_reference", numeroOs);
    if (properties.notificationUrl() != null && !properties.notificationUrl().isBlank()) {
      corpo.put("notification_url", properties.notificationUrl());
    }
    JsonNode resposta =
        rest.post()
            .uri("/checkout/preferences")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Idempotency-Key", UUID.randomUUID().toString())
            .body(corpo)
            .retrieve()
            .body(JsonNode.class);
    String link =
        resposta.path(properties.sandbox() ? "sandbox_init_point" : "init_point").asText();
    return new Cobranca(resposta.path("id").asText(), link);
  }

  @Override
  public ResultadoPagamento consultar(String pagamentoExternoId) {
    JsonNode pagamento =
        rest.get().uri("/v1/payments/{id}", pagamentoExternoId).retrieve().body(JsonNode.class);
    String status = pagamento.path("status").asText();
    Situacao situacao =
        "approved".equals(status)
            ? Situacao.APROVADO
            : RECUSADOS.contains(status) ? Situacao.RECUSADO : Situacao.PENDENTE;
    String referencia = pagamento.path("external_reference").asText(null);
    return new ResultadoPagamento(
        pagamentoExternoId, referencia, situacao, pagamento.path("status_detail").asText(status));
  }
}
