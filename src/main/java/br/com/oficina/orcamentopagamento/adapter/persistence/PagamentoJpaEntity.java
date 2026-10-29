package br.com.oficina.orcamentopagamento.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pagamentos")
public class PagamentoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "orcamento_id", nullable = false)
  Long orcamentoId;

  @Column(name = "numero_os", nullable = false, length = 30)
  String numeroOs;

  @Column(nullable = false, length = 20)
  String provedor;

  @Column(name = "referencia_externa", length = 120)
  String referenciaExterna;

  @Column(name = "pagamento_externo_id", length = 120)
  String pagamentoExternoId;

  @Column(nullable = false, length = 20)
  String status;

  @Column(nullable = false, precision = 12, scale = 2)
  BigDecimal valor;

  @Column(name = "link_pagamento", length = 500)
  String linkPagamento;

  @Column(length = 255)
  String detalhe;

  @Column(name = "criado_em", nullable = false)
  Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  Instant atualizadoEm;
}
