package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.adapter.controller.Respostas.PagamentoResponse;
import br.com.oficina.orcamentopagamento.adapter.security.AuthenticatedPrincipal;
import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamentos")
@Tag(name = "Pagamento", description = "Link de pagamento e nova tentativa")
public class PagamentoController {

  private final OrcamentoService servico;

  public PagamentoController(OrcamentoService servico) {
    this.servico = servico;
  }

  @GetMapping("/{numeroOs}")
  @Operation(summary = "Última cobrança da OS, com o link do Mercado Pago")
  public PagamentoResponse consultar(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return PagamentoResponse.de(servico.consultarPagamento(numeroOs, Clientes.idDo(principal)));
  }

  @PostMapping("/{numeroOs}/nova-tentativa")
  @Operation(summary = "Gera nova cobrança depois de um pagamento recusado")
  public PagamentoResponse novaTentativa(
      @PathVariable String numeroOs, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
    return PagamentoResponse.de(servico.novaTentativa(numeroOs, Clientes.idDo(principal)));
  }
}
