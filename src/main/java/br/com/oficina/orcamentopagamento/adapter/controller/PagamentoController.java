package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.adapter.controller.Respostas.PagamentoResponse;
import br.com.oficina.orcamentopagamento.adapter.security.AuthenticatedPrincipal;
import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamentos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pagamento", description = "Link de pagamento e nova tentativa")
public class PagamentoController {

  private final OrcamentoService servico;

  public PagamentoController(OrcamentoService servico) {
    this.servico = servico;
  }

  @GetMapping("/{numeroOs}")
  @Operation(
      summary = "Última cobrança da OS, com o link do Mercado Pago",
      description =
          "linkPagamento é o Checkout Pro (sandbox_init_point no sandbox). comprovante é o id do"
              + " pagamento no Mercado Pago depois da aprovação.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Cobrança atual",
        content = @Content(examples = @ExampleObject(value = Exemplos.PAGAMENTO))),
    @ApiResponse(
        responseCode = "401",
        description = "Sem token ou token inválido/expirado",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Cliente tentando acessar OS de outro cliente",
        content =
            @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = Exemplos.ERRO_403))),
    @ApiResponse(
        responseCode = "404",
        description = "OS sem orçamento ou sem cobrança",
        content =
            @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = Exemplos.ERRO_404)))
  })
  public PagamentoResponse consultar(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return PagamentoResponse.de(servico.consultarPagamento(numeroOs, Clientes.idDo(principal)));
  }

  @PostMapping("/{numeroOs}/nova-tentativa")
  @Operation(
      summary = "Gera nova cobrança depois de um pagamento recusado",
      description =
          "Só aceito com o orçamento em PAGAMENTO_RECUSADO; cria um novo link. Com a cobrança ainda"
              + " PENDENTE ou já PAGA responde 409.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Nova cobrança PENDENTE",
        content = @Content(examples = @ExampleObject(value = Exemplos.PAGAMENTO_PENDENTE))),
    @ApiResponse(
        responseCode = "401",
        description = "Sem token ou token inválido/expirado",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Cliente tentando acessar OS de outro cliente",
        content =
            @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = Exemplos.ERRO_403))),
    @ApiResponse(
        responseCode = "404",
        description = "OS sem orçamento ou sem cobrança",
        content =
            @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = Exemplos.ERRO_404))),
    @ApiResponse(
        responseCode = "409",
        description = "Transição de status inválida para o estado atual",
        content =
            @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = Exemplos.ERRO_409)))
  })
  public PagamentoResponse novaTentativa(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return PagamentoResponse.de(servico.novaTentativa(numeroOs, Clientes.idDo(principal)));
  }
}
