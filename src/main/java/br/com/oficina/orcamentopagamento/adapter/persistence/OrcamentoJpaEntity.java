package br.com.oficina.orcamentopagamento.adapter.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamentos")
public class OrcamentoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "numero_os", nullable = false, length = 30)
  String numeroOs;

  @Column(name = "id_cliente")
  Long idCliente;

  @Column(nullable = false, length = 30)
  String status;

  @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
  BigDecimal valorTotal;

  @Column(name = "correlation_id", length = 100)
  String correlationId;

  @Column(length = 500)
  String motivo;

  @Column(name = "criado_em", nullable = false)
  Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  Instant atualizadoEm;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "itens_orcamento", joinColumns = @JoinColumn(name = "orcamento_id"))
  List<ItemOrcamentoJpa> itens = new ArrayList<>();
}
