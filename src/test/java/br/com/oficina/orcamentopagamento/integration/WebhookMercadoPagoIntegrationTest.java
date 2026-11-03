package br.com.oficina.orcamentopagamento.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.orcamentopagamento.adapter.payment.MercadoPagoAssinatura;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway.Cobranca;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway.ResultadoPagamento;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway.Situacao;
import br.com.oficina.orcamentopagamento.usecase.saga.TipoMensagemSaga;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/** Webhook com assinatura válida: consulta o provedor e publica PAGAMENTO_CONFIRMADO. */
@TestPropertySource(properties = "oficina.pagamento.mercadopago.webhook-secret=segredo-teste")
class WebhookMercadoPagoIntegrationTest extends IntegrationBase {

  @MockBean private PagamentoGateway provedor;
  @Autowired private MercadoPagoAssinatura assinatura;

  @Test
  void webhookAssinadoConfirmaPagamento() throws Exception {
    String os = "OS-WH-1";
    when(provedor.provedor()).thenReturn("mercadopago");
    when(provedor.criarCobranca(any(), any(), any()))
        .thenReturn(new Cobranca("PREF-1", "https://mp/checkout"));
    when(provedor.consultar("777"))
        .thenReturn(new ResultadoPagamento("777", os, "PREF-1", Situacao.APROVADO, "accredited"));
    when(provedor.consultar("888"))
        .thenReturn(new ResultadoPagamento("888", null, null, Situacao.PENDENTE, null));
    gerarOrcamento(os, 20L);
    servico.aprovar(os, 20L);
    servico.processar(comando(TipoMensagemSaga.SOLICITAR_PAGAMENTO, os, null));

    String v1 = assinatura.hmac("id:777;request-id:r1;ts:1700;");
    mvc.perform(
            post("/webhooks/mercadopago?type=payment&data.id=777")
                .header("x-signature", "ts=1700,v1=" + v1)
                .header("x-request-id", "r1"))
        .andExpect(status().isOk());
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_CONFIRMADO);
    assertThat(eventos.enviadasPara(os).get(1).texto("comprovante")).isEqualTo("777");

    String outro = assinatura.hmac("id:888;ts:1700;");
    mvc.perform(
            post("/webhooks/mercadopago?type=payment&data.id=888")
                .header("x-signature", "ts=1700,v1=" + outro))
        .andExpect(status().isOk());
    mvc.perform(
            post("/webhooks/mercadopago?type=merchant_order&data.id=888")
                .header("x-signature", "ts=1700,v1=" + outro))
        .andExpect(status().isOk());
    verify(provedor, never()).consultar("999");
  }

  @Test
  void aprovacaoDepoisDeRecusaNoMesmoLinkConfirmaPagamento() throws Exception {
    String os = "OS-WH-2";
    when(provedor.provedor()).thenReturn("mercadopago");
    when(provedor.criarCobranca(any(), any(), any()))
        .thenReturn(new Cobranca("PREF-2", "https://mp/checkout"));
    when(provedor.consultar("101"))
        .thenReturn(new ResultadoPagamento("101", os, "PREF-2", Situacao.RECUSADO, "cc_rejected"));
    when(provedor.consultar("102"))
        .thenReturn(new ResultadoPagamento("102", os, "PREF-2", Situacao.APROVADO, "accredited"));
    gerarOrcamento(os, 21L);
    servico.aprovar(os, 21L);
    servico.processar(comando(TipoMensagemSaga.SOLICITAR_PAGAMENTO, os, null));

    notificar("101");
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_RECUSADO);
    notificar("102");
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_CONFIRMADO);
    notificar("101");
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_CONFIRMADO);
    assertThat(eventos.enviadasPara(os)).hasSize(3);
  }

  @Test
  void aprovacaoDeCobrancaAntigaNaoConfirmaATentativaAtual() throws Exception {
    String os = "OS-WH-3";
    when(provedor.provedor()).thenReturn("mercadopago");
    when(provedor.criarCobranca(any(), any(), any()))
        .thenReturn(new Cobranca("PREF-A", "https://mp/a"), new Cobranca("PREF-B", "https://mp/b"));
    when(provedor.consultar("201"))
        .thenReturn(new ResultadoPagamento("201", os, "PREF-A", Situacao.RECUSADO, "cc_rejected"));
    when(provedor.consultar("202"))
        .thenReturn(new ResultadoPagamento("202", os, "PREF-B", Situacao.RECUSADO, "cc_rejected"));
    when(provedor.consultar("203"))
        .thenReturn(new ResultadoPagamento("203", os, "PREF-A", Situacao.APROVADO, "accredited"));
    when(provedor.consultar("204"))
        .thenReturn(new ResultadoPagamento("204", os, "PREF-B", Situacao.APROVADO, "accredited"));
    gerarOrcamento(os, 22L);
    servico.aprovar(os, 22L);
    servico.processar(comando(TipoMensagemSaga.SOLICITAR_PAGAMENTO, os, null));

    notificar("201");
    servico.novaTentativa(os, 22L);
    notificar("202");
    notificar("203");
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_RECUSADO);
    assertThat(servico.consultarPagamento(os, 22L).getReferenciaExterna()).isEqualTo("PREF-B");
    notificar("204");
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_CONFIRMADO);
    assertThat(eventos.enviadasPara(os)).hasSize(4);
    assertThat(eventos.enviadasPara(os).get(3).texto("comprovante")).isEqualTo("204");
  }

  private void notificar(String id) throws Exception {
    String v1 = assinatura.hmac("id:" + id + ";ts:1700;");
    mvc.perform(
            post("/webhooks/mercadopago?type=payment&data.id=" + id)
                .header("x-signature", "ts=1700,v1=" + v1))
        .andExpect(status().isOk());
  }
}
