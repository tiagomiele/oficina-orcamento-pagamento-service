package br.com.oficina.orcamentopagamento.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class ItemOrcamentoJpa {

  @Column(length = 20)
  String tipo;

  @Column(name = "id_servico_sku", length = 100)
  String idServicoSku;

  @Column(length = 255)
  String descricao;

  @Column(nullable = false)
  Integer quantidade;

  @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
  BigDecimal precoUnitario;
}
