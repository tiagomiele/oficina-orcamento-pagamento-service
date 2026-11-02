package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.domain.model.AcaoRejeicao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Rejeição do orçamento pelo cliente")
public record RejeicaoRequest(
    @Schema(
            description = "REFAZER volta ao diagnóstico; CANCELAR cancela a OS",
            example = "REFAZER",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        AcaoRejeicao acao,
    @Schema(
            description = "Motivo informado pelo cliente",
            example = "Valor acima do esperado",
            maxLength = 500)
        @Size(max = 500)
        String motivo) {}
