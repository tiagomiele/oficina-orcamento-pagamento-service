package br.com.oficina.orcamentopagamento.adapter.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway.Situacao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoTest {

  private static MercadoPagoProperties props(
      String token, String segredo, String notificacao, boolean sandbox) {
    return props(token, segredo, notificacao, null, null, sandbox);
  }

  private static MercadoPagoProperties props(
      String token,
      String segredo,
      String notificacao,
      String retorno,
      String descritor,
      boolean sandbox) {
    return new MercadoPagoProperties(
        "https://api.mp.test", token, "PUB", segredo, notificacao, retorno, descritor, sandbox);
  }

  @Test
  void criaPreferenciaEConsultaPagamento() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var gateway =
        new MercadoPagoPagamentoGateway(builder, props("TEST-TOKEN", "s", "https://hook", true));
    server
        .expect(requestTo("https://api.mp.test/checkout/preferences"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer TEST-TOKEN"))
        .andExpect(jsonPath("$.external_reference").value("OS-1"))
        .andExpect(jsonPath("$.notification_url").value("https://hook"))
        .andExpect(jsonPath("$.items[0].id").value("OS-1"))
        .andExpect(jsonPath("$.items[0].title").value("OS OS-1"))
        .andExpect(jsonPath("$.items[0].description").value("OS OS-1"))
        .andExpect(jsonPath("$.items[0].currency_id").value("BRL"))
        .andExpect(jsonPath("$.back_urls").doesNotExist())
        .andExpect(jsonPath("$.auto_return").doesNotExist())
        .andExpect(jsonPath("$.statement_descriptor").doesNotExist())
        .andRespond(
            withSuccess(
                "{\"id\":\"PREF-1\",\"init_point\":\"https://mp/real\",\"sandbox_init_point\":\"https://mp/sandbox\"}",
                MediaType.APPLICATION_JSON));
    for (String[] caso :
        new String[][] {{"1", "approved"}, {"2", "rejected"}, {"3", "in_process"}}) {
      server
          .expect(requestTo("https://api.mp.test/v1/payments/" + caso[0]))
          .andRespond(
              withSuccess(
                  "{\"status\":\""
                      + caso[1]
                      + "\",\"external_reference\":\"OS-1\",\"status_detail\":\"d\"}",
                  MediaType.APPLICATION_JSON));
    }

    var cobranca = gateway.criarCobranca("OS-1", new BigDecimal("99.90"), "OS OS-1");
    assertThat(cobranca.referencia()).isEqualTo("PREF-1");
    assertThat(cobranca.link()).isEqualTo("https://mp/sandbox");
    assertThat(gateway.consultar("1").situacao()).isEqualTo(Situacao.APROVADO);
    assertThat(gateway.consultar("2").situacao()).isEqualTo(Situacao.RECUSADO);
    var pendente = gateway.consultar("3");
    assertThat(pendente.situacao()).isEqualTo(Situacao.PENDENTE);
    assertThat(pendente.numeroOs()).isEqualTo("OS-1");
    assertThat(gateway.provedor()).isEqualTo("mercadopago");
    server.verify();
  }

  @Test
  void producaoSemNotificacaoUsaInitPoint() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var gateway = new MercadoPagoPagamentoGateway(builder, props("T", "s", " ", false));
    server
        .expect(requestTo("https://api.mp.test/checkout/preferences"))
        .andExpect(jsonPath("$.notification_url").doesNotExist())
        .andRespond(
            withSuccess(
                "{\"id\":\"P\",\"init_point\":\"https://mp/real\"}", MediaType.APPLICATION_JSON));
    assertThat(gateway.criarCobranca("OS-2", BigDecimal.ONE, "x").link())
        .isEqualTo("https://mp/real");
    assertThat(new MercadoPagoPagamentoGateway(RestClient.builder(), props("T", "s", null, false)))
        .isNotNull();
  }

  @Test
  void enviaBackUrlsAutoReturnEDescritor() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var gateway =
        new MercadoPagoPagamentoGateway(
            builder, props("T", "s", null, "https://oficina.test/retorno", "OFICINA", true));
    server
        .expect(requestTo("https://api.mp.test/checkout/preferences"))
        .andExpect(jsonPath("$.back_urls.success").value("https://oficina.test/retorno"))
        .andExpect(jsonPath("$.back_urls.failure").value("https://oficina.test/retorno"))
        .andExpect(jsonPath("$.back_urls.pending").value("https://oficina.test/retorno"))
        .andExpect(jsonPath("$.auto_return").value("approved"))
        .andExpect(jsonPath("$.statement_descriptor").value("OFICINA"))
        .andRespond(
            withSuccess(
                "{\"id\":\"P\",\"sandbox_init_point\":\"https://mp/sandbox\"}",
                MediaType.APPLICATION_JSON));
    assertThat(gateway.criarCobranca("OS-3", BigDecimal.TEN, "x").link())
        .isEqualTo("https://mp/sandbox");
    server.verify();
  }

  @Test
  void erroDoMercadoPagoViraMensagemClaraSemToken() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var gateway = new MercadoPagoPagamentoGateway(builder, props("SEGREDO", "s", null, true));
    server
        .expect(requestTo("https://api.mp.test/checkout/preferences"))
        .andRespond(
            withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"invalid_items\",\"message\":\"unit_price invalid\"}"));
    server
        .expect(requestTo("https://api.mp.test/v1/payments/9"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("não é json"));
    assertThatThrownBy(() -> gateway.criarCobranca("OS-4", BigDecimal.ONE, "x"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("HTTP 400 invalid_items unit_price invalid")
        .hasMessageNotContaining("SEGREDO");
    assertThatThrownBy(() -> gateway.consultar("9"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("HTTP 401 sem detalhe");
    server.verify();
  }

  @Test
  void extraiCodigoEMensagemDoErro() {
    assertThat(MercadoPagoPagamentoGateway.erroDoMercadoPago(null)).isEqualTo("sem detalhe");
    assertThat(MercadoPagoPagamentoGateway.erroDoMercadoPago("{}")).isEqualTo("sem detalhe");
    assertThat(
            MercadoPagoPagamentoGateway.erroDoMercadoPago("{\"message\":\"invalid access token\"}"))
        .isEqualTo("invalid access token");
  }

  @Test
  void exigeAccessToken() {
    assertThatThrownBy(
            () ->
                new MercadoPagoPagamentoGateway(RestClient.builder(), props(" ", "s", null, true)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new MercadoPagoPagamentoGateway(RestClient.builder(), props(null, "s", null, true)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validaAssinaturaDoWebhook() {
    MercadoPagoAssinatura assinatura = new MercadoPagoAssinatura(props("T", "segredo", null, true));
    String v1 = assinatura.hmac("id:abc123;request-id:req-1;ts:1700;");
    assertThat(assinatura.valida("ts=1700,v1=" + v1, "req-1", "ABC123")).isTrue();
    String semRequest = assinatura.hmac("id:pay-1;ts:1700;");
    assertThat(assinatura.valida("ts=1700, v1=" + semRequest, null, "pay-1")).isTrue();
    assertThat(assinatura.valida("ts=1700, v1=" + semRequest, " ", "pay-1")).isTrue();
    assertThat(assinatura.valida("ts=1700,v1=errado", "req-1", "abc123")).isFalse();
    assertThat(assinatura.valida("v1=" + v1, "req-1", "abc123")).isFalse();
    assertThat(assinatura.valida("ts=1700,lixo", "req-1", "abc123")).isFalse();
    assertThat(assinatura.valida(null, "req-1", "abc123")).isFalse();
    assertThat(assinatura.valida("ts=1700,v1=" + v1, "req-1", null)).isFalse();
    assertThat(new MercadoPagoAssinatura(props("T", "", null, true)).valida("ts=1,v1=x", "r", "1"))
        .isFalse();
    assertThat(
            new MercadoPagoAssinatura(props("T", null, null, true)).valida("ts=1,v1=x", "r", "1"))
        .isFalse();
  }

  @Test
  void fakeCriaCobrancaFicticia() {
    FakePagamentoGateway fake = new FakePagamentoGateway();
    assertThat(fake.provedor()).isEqualTo("fake");
    assertThat(fake.criarCobranca("OS-1", BigDecimal.ONE, "x").referencia()).startsWith("FAKE-");
    assertThat(fake.consultar("X").situacao()).isEqualTo(Situacao.PENDENTE);
  }
}
