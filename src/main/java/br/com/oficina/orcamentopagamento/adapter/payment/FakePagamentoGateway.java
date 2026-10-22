package br.com.oficina.orcamentopagamento.adapter.payment;

import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Provedor local: cria cobranças fictícias; o resultado vem de POST /pagamentos/{n}/simular. */
@Component
@ConditionalOnProperty(
    name = "oficina.pagamento.provedor",
    havingValue = "fake",
    matchIfMissing = true)
public class FakePagamentoGateway implements PagamentoGateway {

  @Override
  public String provedor() {
    return "fake";
  }

  @Override
  public Cobranca criarCobranca(String numeroOs, BigDecimal valor, String descricao) {
    return new Cobranca("FAKE-" + UUID.randomUUID(), "https://pagamento.fake/checkout/" + numeroOs);
  }

  @Override
  public ResultadoPagamento consultar(String pagamentoExternoId) {
    return new ResultadoPagamento(pagamentoExternoId, null, Situacao.PENDENTE, null);
  }
}
