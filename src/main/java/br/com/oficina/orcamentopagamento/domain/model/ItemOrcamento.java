package br.com.oficina.orcamentopagamento.domain.model;

import java.math.BigDecimal;

public record ItemOrcamento(
    String tipo, String idServicoSku, String descricao, int quantidade, BigDecimal precoUnitario) {}
