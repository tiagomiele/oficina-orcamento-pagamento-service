package br.com.oficina.orcamentopagamento.usecase.gateway;

import java.math.BigDecimal;

/** Provedor de pagamento: Mercado Pago (Checkout Pro) ou fake. */
public interface PagamentoGateway {

  String provedor();

  Cobranca criarCobranca(String numeroOs, BigDecimal valor, String descricao);

  ResultadoPagamento consultar(String pagamentoExternoId);

  record Cobranca(String referencia, String link) {}

  record ResultadoPagamento(
      String pagamentoExternoId, String numeroOs, Situacao situacao, String detalhe) {}

  enum Situacao {
    APROVADO,
    RECUSADO,
    PENDENTE
  }
}
