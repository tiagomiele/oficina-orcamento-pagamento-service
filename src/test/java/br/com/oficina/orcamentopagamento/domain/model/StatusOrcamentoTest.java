package br.com.oficina.orcamentopagamento.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StatusOrcamentoTest {

  private static final Map<StatusOrcamento, Set<StatusOrcamento>> TRANSICOES =
      Map.of(
          StatusOrcamento.AGUARDANDO_APROVACAO,
          EnumSet.of(
              StatusOrcamento.APROVADO, StatusOrcamento.REJEITADO, StatusOrcamento.CANCELADO),
          StatusOrcamento.APROVADO,
          EnumSet.of(StatusOrcamento.PAGAMENTO_PENDENTE, StatusOrcamento.CANCELADO),
          StatusOrcamento.PAGAMENTO_PENDENTE,
          EnumSet.of(StatusOrcamento.PAGO, StatusOrcamento.PAGAMENTO_RECUSADO),
          StatusOrcamento.PAGAMENTO_RECUSADO,
          EnumSet.of(StatusOrcamento.PAGAMENTO_PENDENTE, StatusOrcamento.CANCELADO));

  @ParameterizedTest
  @EnumSource(StatusOrcamento.class)
  void respeitaAsTransicoesPermitidas(StatusOrcamento origem) {
    Set<StatusOrcamento> permitidas = TRANSICOES.getOrDefault(origem, Set.of());
    for (StatusOrcamento destino : StatusOrcamento.values()) {
      assertThat(origem.podeAvancarPara(destino)).isEqualTo(permitidas.contains(destino));
    }
    assertThat(origem.isFinal()).isEqualTo(permitidas.isEmpty());
  }

  @Test
  void naoAvancaParaDestinoNulo() {
    assertThat(StatusOrcamento.AGUARDANDO_APROVACAO.podeAvancarPara(null)).isFalse();
    assertThat(StatusOrcamento.AGUARDANDO_APROVACAO.podeAvancarPara(StatusOrcamento.APROVADO))
        .isTrue();
  }
}
