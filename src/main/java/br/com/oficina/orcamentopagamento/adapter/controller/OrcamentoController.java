package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.adapter.controller.Respostas.OrcamentoResponse;
import br.com.oficina.orcamentopagamento.adapter.security.AuthenticatedPrincipal;
import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orcamentos")
@Tag(name = "Orçamento", description = "Consulta, aprovação e rejeição do orçamento pelo cliente")
public class OrcamentoController {

  private final OrcamentoService servico;

  public OrcamentoController(OrcamentoService servico) {
    this.servico = servico;
  }

  @GetMapping("/{numeroOs}")
  @Operation(summary = "Orçamento atual da OS")
  public OrcamentoResponse consultar(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return OrcamentoResponse.de(servico.consultar(numeroOs, Clientes.idDo(principal)));
  }

  @PostMapping("/{numeroOs}/aprovar")
  @Operation(summary = "Aprova o orçamento (evento ORCAMENTO_APROVADO)")
  public OrcamentoResponse aprovar(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return OrcamentoResponse.de(servico.aprovar(numeroOs, Clientes.idDo(principal)));
  }

  @PostMapping("/{numeroOs}/rejeitar")
  @Operation(summary = "Rejeita o orçamento: REFAZER volta ao diagnóstico, CANCELAR cancela a OS")
  public OrcamentoResponse rejeitar(
      @PathVariable String numeroOs,
      @Valid @RequestBody RejeicaoRequest request,
      @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return OrcamentoResponse.de(
        servico.rejeitar(numeroOs, Clientes.idDo(principal), request.acao(), request.motivo()));
  }
}
