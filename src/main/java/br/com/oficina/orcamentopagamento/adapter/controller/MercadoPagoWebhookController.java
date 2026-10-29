package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.adapter.payment.MercadoPagoAssinatura;
import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Webhook do Mercado Pago: substitui o antigo POST /ordens-servico/{n}/confirmar-pagamento. */
@RestController
@Tag(name = "Webhook")
public class MercadoPagoWebhookController {

  private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

  private final OrcamentoService servico;
  private final MercadoPagoAssinatura assinatura;

  public MercadoPagoWebhookController(OrcamentoService servico, MercadoPagoAssinatura assinatura) {
    this.servico = servico;
    this.assinatura = assinatura;
  }

  @PostMapping("/webhooks/mercadopago")
  @Operation(summary = "Notificação de pagamento do Mercado Pago (assinatura x-signature)")
  public ResponseEntity<Void> receber(
      @RequestParam(name = "data.id", required = false) String dataId,
      @RequestParam(name = "type", required = false) String tipo,
      @RequestHeader(name = "x-signature", required = false) String xSignature,
      @RequestHeader(name = "x-request-id", required = false) String xRequestId) {
    if (!assinatura.valida(xSignature, xRequestId, dataId)) {
      log.warn("Webhook do Mercado Pago com assinatura inválida");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if ("payment".equals(tipo)) {
      servico.notificacaoDoProvedor(dataId);
    }
    return ResponseEntity.ok().build();
  }
}
