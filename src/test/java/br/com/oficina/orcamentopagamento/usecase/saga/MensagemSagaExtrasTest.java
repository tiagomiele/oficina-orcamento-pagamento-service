package br.com.oficina.orcamentopagamento.usecase.saga;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MensagemSagaExtrasTest {

  @Test
  void leDecimalELista() {
    MensagemSaga m =
        MensagemSaga.evento(
            TipoMensagemSaga.GERAR_ORCAMENTO,
            "OS-1",
            "c",
            MensagemSaga.dados(
                "valorTotal", 10.5, "itens", List.of(Map.of("descricao", "x")), "texto", "a"));
    assertThat(m.decimal("valorTotal")).isEqualByComparingTo(new BigDecimal("10.5"));
    assertThat(m.decimal("nada")).isNull();
    assertThat(m.lista("itens")).hasSize(1);
    assertThat(m.lista("texto")).isEmpty();
  }
}
