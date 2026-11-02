package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Só existe com o provedor fake: simula a resposta do Mercado Pago (local e BDD). */
@RestController
@ConditionalOnProperty(
    name = "oficina.pagamento.provedor",
    havingValue = "fake",
    matchIfMissing = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pagamento")
public class SimulacaoPagamentoController {

  private final OrcamentoService servico;

  public SimulacaoPagamentoController(OrcamentoService servico) {
    this.servico = servico;
  }

  @PostMapping("/pagamentos/{numeroOs}/simular")
  @Operation(
      summary = "Simula pagamento aprovado ou recusado (provedor fake)",
      description =
          "Só existe com PAYMENT_PROVIDER=fake (local, BDD e E2E). aprovado=true publica"
              + " PAGAMENTO_CONFIRMADO; false publica PAGAMENTO_RECUSADO. aplicado=false: a"
              + " cobrança não estava pendente (repetição).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Resultado aplicado ou ignorado",
        content = @Content(examples = @ExampleObject(value = "{\"aplicado\": true}"))),
    @ApiResponse(
        responseCode = "401",
        description = "Sem token ou token inválido/expirado",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "OS sem orçamento ou sem cobrança",
        content =
            @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = Exemplos.ERRO_404)))
  })
  public Map<String, Object> simular(
      @PathVariable String numeroOs, @RequestParam(defaultValue = "true") boolean aprovado) {
    return Map.of("aplicado", servico.simular(numeroOs, aprovado));
  }
}
