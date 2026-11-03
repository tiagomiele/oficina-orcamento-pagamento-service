package br.com.oficina.orcamentopagamento.usecase.gateway;

import java.math.BigDecimal;

/** Provedor de pagamento: Mercado Pago (Checkout Pro) ou fake. */
public interface PagamentoGateway {

  String provedor();

  Cobranca criarCobranca(String numeroOs, BigDecimal valor, String descricao);

  ResultadoPagamento consultar(String pagamentoExternoId);

  record Cobranca(String referencia, String link) {}

  /**
   * {@code referenciaCobranca} identifica a cobrança (tentativa) a que o pagamento pertence; nulo
   * quando o provedor não informa.
   */
  record ResultadoPagamento(
      String pagamentoExternoId,
      String numeroOs,
      String referenciaCobranca,
      Situacao situacao,
      String detalhe) {}

  enum Situacao {
    APROVADO,
    RECUSADO,
    PENDENTE
  }
}
