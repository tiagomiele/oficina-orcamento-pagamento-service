package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.adapter.controller.Respostas.OrcamentoResponse;
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
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orcamentos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orçamento", description = "Consulta, aprovação e rejeição do orçamento pelo cliente")
public class OrcamentoController {

  private final OrcamentoService servico;

  public OrcamentoController(OrcamentoService servico) {
    this.servico = servico;
  }

  @GetMapping("/{numeroOs}")
  @Operation(
      summary = "Orçamento atual da OS",
      description =
          "Cliente (token do Auth por CPF) vê só as próprias OS; funcionário e técnico (token do OS"
              + " Service) veem todas.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Orçamento atual",
        content = @Content(examples = @ExampleObject(value = Exemplos.ORCAMENTO))),
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
  public OrcamentoResponse consultar(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return OrcamentoResponse.de(servico.consultar(numeroOs, Clientes.idDo(principal)));
  }

  @PostMapping("/{numeroOs}/aprovar")
  @Operation(
      summary = "Aprova o orçamento (evento ORCAMENTO_APROVADO)",
      description =
          "Só aceito em AGUARDANDO_APROVACAO. Publica ORCAMENTO_APROVADO na fila os-eventos; o OS"
              + " Service segue a Saga para o reparo.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Orçamento APROVADO",
        content = @Content(examples = @ExampleObject(value = Exemplos.ORCAMENTO_APROVADO))),
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
  public OrcamentoResponse aprovar(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return OrcamentoResponse.de(servico.aprovar(numeroOs, Clientes.idDo(principal)));
  }

  @PostMapping("/{numeroOs}/rejeitar")
  @Operation(
      summary = "Rejeita o orçamento: REFAZER volta ao diagnóstico, CANCELAR cancela a OS",
      description =
          "Só aceito em AGUARDANDO_APROVACAO. Publica ORCAMENTO_REJEITADO com a ação escolhida.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Orçamento REJEITADO",
        content = @Content(examples = @ExampleObject(value = Exemplos.ORCAMENTO_REJEITADO))),
    @ApiResponse(
        responseCode = "400",
        description = "acao ausente/inválida ou motivo com mais de 500 caracteres",
        content = @Content),
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
  public OrcamentoResponse rejeitar(
      @PathVariable String numeroOs,
      @Valid @RequestBody RejeicaoRequest request,
      @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return OrcamentoResponse.de(
        servico.rejeitar(numeroOs, Clientes.idDo(principal), request.acao(), request.motivo()));
  }
}
