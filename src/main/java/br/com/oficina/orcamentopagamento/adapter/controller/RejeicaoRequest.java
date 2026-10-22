package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.domain.model.AcaoRejeicao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejeicaoRequest(@NotNull AcaoRejeicao acao, @Size(max = 500) String motivo) {}
