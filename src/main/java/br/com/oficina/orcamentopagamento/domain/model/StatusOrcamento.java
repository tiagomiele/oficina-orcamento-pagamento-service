package br.com.oficina.orcamentopagamento.domain.model;

/** Ciclo de vida do orçamento de uma OS no Serviço de Orçamento e Pagamento. */
public enum StatusOrcamento {
  AGUARDANDO_APROVACAO,
  APROVADO,
  REJEITADO,
  PAGAMENTO_PENDENTE,
  PAGO,
  PAGAMENTO_RECUSADO,
  CANCELADO;

  public boolean podeAvancarPara(StatusOrcamento destino) {
    if (destino == null) {
      return false;
    }
    return switch (this) {
      case AGUARDANDO_APROVACAO ->
          destino == APROVADO || destino == REJEITADO || destino == CANCELADO;
      case APROVADO -> destino == PAGAMENTO_PENDENTE || destino == CANCELADO;
      case PAGAMENTO_PENDENTE -> destino == PAGO || destino == PAGAMENTO_RECUSADO;
      case PAGAMENTO_RECUSADO -> destino == PAGAMENTO_PENDENTE || destino == CANCELADO;
      default -> false;
    };
  }

  public boolean isFinal() {
    return !podeAvancarParaAlgumaEtapa();
  }

  private boolean podeAvancarParaAlgumaEtapa() {
    for (StatusOrcamento destino : values()) {
      if (podeAvancarPara(destino)) {
        return true;
      }
    }
    return false;
  }
}
