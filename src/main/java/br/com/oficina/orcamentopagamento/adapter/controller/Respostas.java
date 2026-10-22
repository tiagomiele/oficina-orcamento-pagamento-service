package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.domain.model.ItemOrcamento;
import br.com.oficina.orcamentopagamento.domain.model.Orcamento;
import br.com.oficina.orcamentopagamento.domain.model.Pagamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

final class Respostas {

  private Respostas() {}

  record OrcamentoResponse(
      String numeroOs,
      String status,
      BigDecimal valorTotal,
      List<ItemOrcamento> itens,
      String motivo,
      Instant atualizadoEm) {

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

  record PagamentoResponse(
      String numeroOs,
      String status,
      String provedor,
      BigDecimal valor,
      String linkPagamento,
      String comprovante,
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
