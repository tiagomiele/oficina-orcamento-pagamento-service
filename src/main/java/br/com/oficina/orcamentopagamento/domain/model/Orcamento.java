package br.com.oficina.orcamentopagamento.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Orçamento de uma OS. Cada refazer gera um orçamento novo; o anterior fica rejeitado. */
public final class Orcamento {

  private final Long id;
  private final String numeroOs;
  private final Long idCliente;
  private final BigDecimal valorTotal;
  private final List<ItemOrcamento> itens;
  private final String correlationId;
  private final Instant criadoEm;
  private StatusOrcamento status;
  private String motivo;
  private Instant atualizadoEm;

  public Orcamento(
      Long id,
      String numeroOs,
      Long idCliente,
      StatusOrcamento status,
      BigDecimal valorTotal,
      List<ItemOrcamento> itens,
      String correlationId,
      String motivo,
      Instant criadoEm,
      Instant atualizadoEm) {
    this.id = id;
    this.numeroOs = Objects.requireNonNull(numeroOs, "numeroOs é obrigatório");
    this.idCliente = idCliente;
    this.status = Objects.requireNonNull(status, "status é obrigatório");
    this.valorTotal = valorTotal == null ? BigDecimal.ZERO : valorTotal;
    this.itens = itens == null ? List.of() : List.copyOf(itens);
    this.correlationId = correlationId;
    this.motivo = motivo;
    this.criadoEm = criadoEm;
    this.atualizadoEm = atualizadoEm;
  }

  public static Orcamento novo(
      String numeroOs,
      Long idCliente,
      BigDecimal valorTotal,
      List<ItemOrcamento> itens,
      String correlationId,
      Instant agora) {
    return new Orcamento(
        null,
        numeroOs,
        idCliente,
        StatusOrcamento.AGUARDANDO_APROVACAO,
        valorTotal,
        itens,
        correlationId,
        null,
        agora,
        agora);
  }

  public void aprovar(Instant agora) {
    mudar(StatusOrcamento.APROVADO, null, agora);
  }

  public void rejeitar(String motivoRejeicao, Instant agora) {
    mudar(StatusOrcamento.REJEITADO, motivoRejeicao, agora);
  }

  public void cancelar(String motivoCancelamento, Instant agora) {
    mudar(StatusOrcamento.CANCELADO, motivoCancelamento, agora);
  }

  public void aguardarPagamento(Instant agora) {
    mudar(StatusOrcamento.PAGAMENTO_PENDENTE, null, agora);
  }

  public void confirmarPagamento(Instant agora) {
    mudar(StatusOrcamento.PAGO, null, agora);
  }

  public void recusarPagamento(String motivoRecusa, Instant agora) {
    mudar(StatusOrcamento.PAGAMENTO_RECUSADO, motivoRecusa, agora);
  }

  public boolean pertenceAo(Long cliente) {
    return idCliente != null && idCliente.equals(cliente);
  }

  private void mudar(StatusOrcamento destino, String novoMotivo, Instant agora) {
    if (!status.podeAvancarPara(destino)) {
      throw new TransicaoInvalidaException(status, destino);
    }
    status = destino;
    motivo = novoMotivo;
    atualizadoEm = agora;
  }

  public Long getId() {
    return id;
  }

  public String getNumeroOs() {
    return numeroOs;
  }

  public Long getIdCliente() {
    return idCliente;
  }

  public StatusOrcamento getStatus() {
    return status;
  }

  public BigDecimal getValorTotal() {
    return valorTotal;
  }

  public List<ItemOrcamento> getItens() {
    return itens;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getMotivo() {
    return motivo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }
}
