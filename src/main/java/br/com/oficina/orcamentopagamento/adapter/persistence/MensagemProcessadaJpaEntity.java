package br.com.oficina.orcamentopagamento.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mensagens_processadas")
public class MensagemProcessadaJpaEntity {

  @Id
  @Column(name = "message_id", length = 100)
  String messageId;

  @Column(nullable = false, length = 60)
  String tipo;

  @Column(name = "numero_os", nullable = false, length = 30)
  String numeroOs;

  @Column(name = "correlation_id", length = 100)
  String correlationId;

  @Column(name = "processada_em", nullable = false)
  Instant processadaEm;
}
