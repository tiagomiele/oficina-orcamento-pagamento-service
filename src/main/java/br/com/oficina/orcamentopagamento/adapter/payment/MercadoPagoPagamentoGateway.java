package br.com.oficina.orcamentopagamento.adapter.payment;

import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Mercado Pago Checkout Pro: cria a preferência com {@code external_reference} = número da OS e
 * consulta o pagamento informado pelo webhook em {@code /v1/payments/{id}}.
 */
@Component
@ConditionalOnProperty(name = "oficina.pagamento.provedor", havingValue = "mercadopago")
public class MercadoPagoPagamentoGateway implements PagamentoGateway {

  private static final Logger log = LoggerFactory.getLogger(MercadoPagoPagamentoGateway.class);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Set<String> RECUSADOS =
      Set.of("rejected", "cancelled", "refunded", "charged_back");

  private final RestClient rest;
  private final MercadoPagoProperties properties;

  public MercadoPagoPagamentoGateway(RestClient.Builder builder, MercadoPagoProperties properties) {
    if (vazio(properties.accessToken())) {
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
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", numeroOs);
    item.put("title", descricao);
    item.put("description", descricao);
    item.put("quantity", 1);
    item.put("unit_price", valor);
    item.put("currency_id", "BRL");
    Map<String, Object> corpo = new LinkedHashMap<>();
    corpo.put("items", List.of(item));
    corpo.put("external_reference", numeroOs);
    if (!vazio(properties.notificationUrl())) {
      corpo.put("notification_url", properties.notificationUrl());
    }
    if (!vazio(properties.backUrl())) {
      String retorno = properties.backUrl();
      corpo.put("back_urls", Map.of("success", retorno, "failure", retorno, "pending", retorno));
      corpo.put("auto_return", "approved");
    }
    if (!vazio(properties.statementDescriptor())) {
      corpo.put("statement_descriptor", properties.statementDescriptor());
    }
    JsonNode resposta =
        chamar(
            "criar a preferência da OS " + numeroOs,
            () ->
                rest.post()
                    .uri("/checkout/preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class));
    String link =
        resposta.path(properties.sandbox() ? "sandbox_init_point" : "init_point").asText();
    return new Cobranca(resposta.path("id").asText(), link);
  }

  @Override
  public ResultadoPagamento consultar(String pagamentoExternoId) {
    JsonNode pagamento =
        chamar(
            "consultar o pagamento " + pagamentoExternoId,
            () ->
                rest.get()
                    .uri("/v1/payments/{id}", pagamentoExternoId)
                    .retrieve()
                    .body(JsonNode.class));
    String status = pagamento.path("status").asText();
    Situacao situacao =
        "approved".equals(status)
            ? Situacao.APROVADO
            : RECUSADOS.contains(status) ? Situacao.RECUSADO : Situacao.PENDENTE;
    String referencia = pagamento.path("external_reference").asText(null);
    return new ResultadoPagamento(
        pagamentoExternoId, referencia, situacao, pagamento.path("status_detail").asText(status));
  }

  private static JsonNode chamar(String operacao, Supplier<JsonNode> chamada) {
    try {
      return chamada.get();
    } catch (RestClientResponseException e) {
      String erro = erroDoMercadoPago(e.getResponseBodyAsString());
      log.error("Mercado Pago recusou {}: HTTP {} {}", operacao, e.getStatusCode().value(), erro);
      throw new IllegalStateException(
          "Mercado Pago recusou " + operacao + ": HTTP " + e.getStatusCode().value() + " " + erro,
          e);
    }
  }

  /** Só o código e a mensagem do erro (ex.: invalid_access_token, invalid_items). */
  static String erroDoMercadoPago(String corpo) {
    try {
      JsonNode erro = JSON.readTree(corpo == null ? "" : corpo);
      String codigo = erro.path("error").asText("");
      String mensagem = erro.path("message").asText("");
      String texto = (codigo + " " + mensagem).trim();
      return texto.isEmpty() ? "sem detalhe" : texto;
    } catch (Exception e) {
      return "sem detalhe";
    }
  }

  private static boolean vazio(String valor) {
    return valor == null || valor.isBlank();
  }
}
