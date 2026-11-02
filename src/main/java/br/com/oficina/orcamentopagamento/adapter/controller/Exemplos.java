package br.com.oficina.orcamentopagamento.adapter.controller;

/** Exemplos de resposta exibidos no Swagger (constantes para uso em anotações). */
final class Exemplos {
  private Exemplos() {}

  private static final String OS = "\"numeroOs\": \"OS-102026-000003\", ";
  private static final String ITENS =
      "\"valorTotal\": 150.00, \"itens\": [{\"tipo\": \"SERVICO\", \"idServicoSku\": \"7\","
          + " \"descricao\": \"Troca de óleo\", \"quantidade\": 1, \"precoUnitario\": 120.00},"
          + " {\"tipo\": \"PECA\", \"idServicoSku\": \"FILTRO-01\", \"descricao\": \"Filtro de"
          + " óleo\", \"quantidade\": 1, \"precoUnitario\": 30.00}], ";
  private static final String DATA = "\"atualizadoEm\": \"2026-10-30T21:39:00Z\"}";

  static final String ORCAMENTO =
      "{" + OS + "\"status\": \"AGUARDANDO_APROVACAO\", " + ITENS + DATA;
  static final String ORCAMENTO_APROVADO = "{" + OS + "\"status\": \"APROVADO\", " + ITENS + DATA;
  static final String ORCAMENTO_REJEITADO =
      "{"
          + OS
          + "\"status\": \"REJEITADO\", "
          + ITENS
          + "\"motivo\": \"Valor acima do esperado\", "
          + DATA;

  private static final String LINK =
      "\"linkPagamento\":"
          + " \"https://sandbox.mercadopago.com.br/checkout/v1/redirect?pref_id=3796230935-f8cb\"";
  static final String PAGAMENTO =
      "{"
          + OS
          + "\"status\": \"APROVADO\", \"provedor\": \"mercadopago\", \"valor\": 150.00, "
          + LINK
          + ", \"comprovante\": \"185889123735\"}";
  static final String PAGAMENTO_PENDENTE =
      "{"
          + OS
          + "\"status\": \"PENDENTE\", \"provedor\": \"mercadopago\", \"valor\": 150.00, "
          + LINK
          + "}";

  private static final String INSTANCIA = ", \"instance\": \"/orcamentos/OS-102026-000003\"}";
  static final String ERRO_403 =
      "{\"type\": \"about:blank\", \"title\": \"Forbidden\", \"status\": 403, \"detail\": \"A OS"
          + " OS-102026-000003 não pertence ao cliente autenticado\""
          + INSTANCIA;
  static final String ERRO_404 =
      "{\"type\": \"about:blank\", \"title\": \"Not Found\", \"status\": 404, \"detail\":"
          + " \"Orçamento da OS OS-102026-000003 não encontrado\""
          + INSTANCIA;
  static final String ERRO_409 =
      "{\"type\": \"about:blank\", \"title\": \"Conflict\", \"status\": 409, \"detail\": \"Não é"
          + " possível ir de PAGO para PAGAMENTO_PENDENTE\""
          + INSTANCIA;
}
