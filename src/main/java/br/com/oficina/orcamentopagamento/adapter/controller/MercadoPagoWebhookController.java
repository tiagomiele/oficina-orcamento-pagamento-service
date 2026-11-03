package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.adapter.payment.MercadoPagoAssinatura;
import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @Operation(
      summary = "Notificação de pagamento do Mercado Pago (assinatura x-signature)",
      description =
          "Chamado pelo Mercado Pago via API Gateway (rota pública, sem JWT). Valida x-signature ="
              + " HMAC-SHA256 de 'id:<data.id>;request-id:<x-request-id>;ts:<ts>;' com"
              + " MERCADOPAGO_WEBHOOK_SECRET, consulta GET /v1/payments/{id} e aplica: approved ->"
              + " PAGAMENTO_CONFIRMADO; rejected/cancelled/refunded/charged_back ->"
              + " PAGAMENTO_RECUSADO. Notificações repetidas são ignoradas.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Recebida (aplicada ou ignorada por repetição/tipo diferente de payment)",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "x-signature ausente ou inválida",
        content = @Content)
  })
  public ResponseEntity<Void> receber(
      @Parameter(description = "Id do pagamento no Mercado Pago", example = "185889123735")
          @RequestParam(name = "data.id", required = false)
          String dataId,
      @Parameter(description = "Tipo da notificação; só payment é processado", example = "payment")
          @RequestParam(name = "type", required = false)
          String tipo,
      @Parameter(description = "ts=<epoch>,v1=<hmac hex>", example = "ts=1700000000,v1=3f2a...")
          @RequestHeader(name = "x-signature", required = false)
          String xSignature,
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
