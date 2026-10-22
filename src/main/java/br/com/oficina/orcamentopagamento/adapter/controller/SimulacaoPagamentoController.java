package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@Tag(name = "Pagamento")
public class SimulacaoPagamentoController {

  private final OrcamentoService servico;

  public SimulacaoPagamentoController(OrcamentoService servico) {
    this.servico = servico;
  }

  @PostMapping("/pagamentos/{numeroOs}/simular")
  @Operation(summary = "Simula pagamento aprovado ou recusado (provedor fake)")
  public Map<String, Object> simular(
      @PathVariable String numeroOs, @RequestParam(defaultValue = "true") boolean aprovado) {
    return Map.of("aplicado", servico.simular(numeroOs, aprovado));
  }
}
