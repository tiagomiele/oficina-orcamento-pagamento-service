package br.com.oficina.orcamentopagamento.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrcamentoPagamentoDominioTest {

  private static final Instant T = Instant.parse("2026-11-01T10:00:00Z");

  private Orcamento novo() {
    return Orcamento.novo(
        "OS-1",
        3L,
        new BigDecimal("150.00"),
        List.of(new ItemOrcamento("SERVICO", "1", "Troca", 1, BigDecimal.TEN)),
        "c",
        T);
  }

  @Test
  void cicloDoOrcamento() {
    Orcamento o = novo();
    assertThat(o.pertenceAo(3L)).isTrue();
    assertThat(o.pertenceAo(4L)).isFalse();
    o.aprovar(T);
    o.aguardarPagamento(T);
    o.recusarPagamento("sem saldo", T);
    assertThat(o.getMotivo()).isEqualTo("sem saldo");
    o.aguardarPagamento(T);
    o.confirmarPagamento(T);
    assertThat(o.getStatus()).isEqualTo(StatusOrcamento.PAGO);
    assertThat(o.getItens()).hasSize(1);
    assertThat(o.getCriadoEm()).isEqualTo(T);
    assertThatThrownBy(() -> o.cancelar("x", T)).isInstanceOf(TransicaoInvalidaException.class);
  }

  @Test
  void rejeicaoECancelamento() {
    Orcamento rejeitado = novo();
    rejeitado.rejeitar("caro", T);
    assertThat(rejeitado.getStatus()).isEqualTo(StatusOrcamento.REJEITADO);
    Orcamento cancelado = novo();
    cancelado.aprovar(T);
    cancelado.cancelar("estoque", T);
    assertThat(cancelado.getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
    Orcamento semCliente =
        new Orcamento(1L, "OS-2", null, StatusOrcamento.APROVADO, null, null, null, null, T, T);
    assertThat(semCliente.pertenceAo(1L)).isFalse();
    assertThat(semCliente.getValorTotal()).isZero();
    assertThat(semCliente.getItens()).isEmpty();
  }

  @Test
  void pagamentoSoFinalizaUmaVez() {
    Orcamento o =
        new Orcamento(
            9L, "OS-1", 3L, StatusOrcamento.APROVADO, BigDecimal.TEN, null, "c", null, T, T);
    Pagamento p = Pagamento.pendente(o, "fake", "REF", "http://link", T);
    assertThat(p.getOrcamentoId()).isEqualTo(9L);
    assertThat(p.getValor()).isEqualTo(BigDecimal.TEN);
    p.aprovar("MP-1", T);
    assertThat(p.getStatus()).isEqualTo(StatusPagamento.APROVADO);
    assertThatThrownBy(() -> p.recusar("MP-1", "x", T))
        .isInstanceOf(TransicaoInvalidaException.class);
    Pagamento r = Pagamento.pendente(o, "fake", "REF", "http://link", T);
    r.recusar("MP-2", "cartão recusado", T);
    assertThat(r.getDetalhe()).isEqualTo("cartão recusado");
    assertThat(r.getPagamentoExternoId()).isEqualTo("MP-2");
    assertThat(AcaoRejeicao.valueOf("REFAZER")).isEqualTo(AcaoRejeicao.REFAZER);
  }
}
