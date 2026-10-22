package br.com.oficina.orcamentopagamento.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Uma tentativa de cobrança do orçamento no provedor (Mercado Pago ou fake). */
public final class Pagamento {

  private final Long id;
  private final Long orcamentoId;
  private final String numeroOs;
  private final String provedor;
  private final String referenciaExterna;
  private final BigDecimal valor;
  private final String linkPagamento;
  private final Instant criadoEm;
  private StatusPagamento status;
  private String pagamentoExternoId;
  private String detalhe;
  private Instant atualizadoEm;

  public Pagamento(
      Long id,
      Long orcamentoId,
      String numeroOs,
      String provedor,
      String referenciaExterna,
      String pagamentoExternoId,
      StatusPagamento status,
      BigDecimal valor,
      String linkPagamento,
      String detalhe,
      Instant criadoEm,
      Instant atualizadoEm) {
    this.id = id;
    this.orcamentoId = orcamentoId;
    this.numeroOs = numeroOs;
    this.provedor = provedor;
    this.referenciaExterna = referenciaExterna;
    this.pagamentoExternoId = pagamentoExternoId;
    this.status = status;
    this.valor = valor;
    this.linkPagamento = linkPagamento;
    this.detalhe = detalhe;
    this.criadoEm = criadoEm;
    this.atualizadoEm = atualizadoEm;
  }

  public static Pagamento pendente(
      Orcamento orcamento, String provedor, String referencia, String link, Instant agora) {
    return new Pagamento(
        null,
        orcamento.getId(),
        orcamento.getNumeroOs(),
        provedor,
        referencia,
        null,
        StatusPagamento.PENDENTE,
        orcamento.getValorTotal(),
        link,
        null,
        agora,
        agora);
  }

  public void aprovar(String idExterno, Instant agora) {
    finalizar(StatusPagamento.APROVADO, idExterno, null, agora);
  }

  public void recusar(String idExterno, String motivo, Instant agora) {
    finalizar(StatusPagamento.RECUSADO, idExterno, motivo, agora);
  }

  private void finalizar(StatusPagamento destino, String idExterno, String motivo, Instant agora) {
    if (status != StatusPagamento.PENDENTE) {
      throw new TransicaoInvalidaException(status, destino);
    }
    status = destino;
    pagamentoExternoId = idExterno;
    detalhe = motivo;
    atualizadoEm = agora;
  }

  public Long getId() {
    return id;
  }

  public Long getOrcamentoId() {
    return orcamentoId;
  }

  public String getNumeroOs() {
    return numeroOs;
  }

  public String getProvedor() {
    return provedor;
  }

  public String getReferenciaExterna() {
    return referenciaExterna;
  }

  public String getPagamentoExternoId() {
    return pagamentoExternoId;
  }

  public StatusPagamento getStatus() {
    return status;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public String getLinkPagamento() {
    return linkPagamento;
  }

  public String getDetalhe() {
    return detalhe;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }
}
