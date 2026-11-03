package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.domain.model.ItemOrcamento;
import br.com.oficina.orcamentopagamento.domain.model.Orcamento;
import br.com.oficina.orcamentopagamento.domain.model.Pagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

final class Respostas {

  private Respostas() {}

  @Schema(name = "OrcamentoResponse", description = "Orçamento atual da OS")
  record OrcamentoResponse(
      @Schema(example = "OS-102026-000003") String numeroOs,
      @Schema(
              description =
                  "AGUARDANDO_APROVACAO, APROVADO, REJEITADO, PAGAMENTO_PENDENTE, PAGO,"
                      + " PAGAMENTO_RECUSADO ou CANCELADO",
              example = "AGUARDANDO_APROVACAO")
          String status,
      @Schema(example = "150.00") BigDecimal valorTotal,
      @Schema(description = "Serviços e peças vindos do diagnóstico (tipo SERVICO ou PECA)")
          List<ItemOrcamento> itens,
      @Schema(
              description = "Motivo da rejeição, do cancelamento ou da recusa do pagamento",
              example = "Valor acima do esperado")
          String motivo,
      @Schema(example = "2026-10-30T21:39:00Z") Instant atualizadoEm) {

    static OrcamentoResponse de(Orcamento o) {
      return new OrcamentoResponse(
          o.getNumeroOs(),
          o.getStatus().name(),
          o.getValorTotal(),
          o.getItens(),
          o.getMotivo(),
          o.getAtualizadoEm());
    }
  }

  @Schema(name = "PagamentoResponse", description = "Última cobrança da OS")
  record PagamentoResponse(
      @Schema(example = "OS-102026-000003") String numeroOs,
      @Schema(description = "PENDENTE, APROVADO ou RECUSADO", example = "PENDENTE") String status,
      @Schema(description = "fake ou mercadopago", example = "mercadopago") String provedor,
      @Schema(example = "150.00") BigDecimal valor,
      @Schema(
              description = "Link do Checkout Pro para o cliente pagar",
              example =
                  "https://sandbox.mercadopago.com.br/checkout/v1/redirect?pref_id=3796230935-f8cb")
          String linkPagamento,
      @Schema(
              description = "Id do pagamento no Mercado Pago (depois de aprovado ou recusado)",
              example = "185889123735")
          String comprovante,
      @Schema(
              description = "status_detail do Mercado Pago na recusa",
              example = "cc_rejected_other_reason")
          String detalhe) {

    static PagamentoResponse de(Pagamento p) {
      return new PagamentoResponse(
          p.getNumeroOs(),
          p.getStatus().name(),
          p.getProvedor(),
          p.getValor(),
          p.getLinkPagamento(),
          p.getPagamentoExternoId(),
          p.getDetalhe());
    }
  }
}
