package br.com.oficina.orcamentopagamento.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;
import br.com.oficina.orcamentopagamento.usecase.saga.TipoMensagemSaga;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Postgres (Testcontainers) + provedor fake + mensageria em memória. */
class OrcamentoPagamentoIntegrationTest extends IntegrationBase {

  private static String os() {
    return "OS-" + UUID.randomUUID().toString().substring(0, 8);
  }

  @Test
  void fluxoFelizComPagamentoAprovado() throws Exception {
    String os = os();
    gerarOrcamento(os, 10L);
    mvc.perform(get("/orcamentos/" + os).header("Authorization", cliente(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
        .andExpect(jsonPath("$.valorTotal").value(250.5))
        .andExpect(jsonPath("$.itens.length()").value(2));
    mvc.perform(post("/orcamentos/" + os + "/aprovar").header("Authorization", cliente(10)))
        .andExpect(jsonPath("$.status").value("APROVADO"));
    MensagemSaga aprovado = eventos.enviadasPara(os).get(0);
    assertThat(aprovado.tipo()).isEqualTo(TipoMensagemSaga.ORCAMENTO_APROVADO);
    assertThat(aprovado.correlationId()).isEqualTo("corr-" + os);

    assertThat(servico.processar(comando(TipoMensagemSaga.SOLICITAR_PAGAMENTO, os, null))).isTrue();
    mvc.perform(get("/pagamentos/" + os).header("Authorization", cliente(10)))
        .andExpect(jsonPath("$.status").value("PENDENTE"))
        .andExpect(jsonPath("$.provedor").value("fake"));
    mvc.perform(post("/pagamentos/" + os + "/simular").header("Authorization", funcionario))
        .andExpect(jsonPath("$.aplicado").value(true));
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_CONFIRMADO);
    assertThat(eventos.enviadasPara(os).get(1).texto("comprovante")).startsWith("FAKE-");
    mvc.perform(post("/pagamentos/" + os + "/simular").header("Authorization", funcionario))
        .andExpect(jsonPath("$.aplicado").value(false));
    mvc.perform(get("/orcamentos/" + os).header("Authorization", funcionario))
        .andExpect(jsonPath("$.status").value("PAGO"));
  }

  @Test
  void pagamentoRecusadoPermiteNovaTentativa() throws Exception {
    String os = os();
    gerarOrcamento(os, 11L);
    servico.aprovar(os, 11L);
    servico.processar(comando(TipoMensagemSaga.SOLICITAR_PAGAMENTO, os, null));
    mvc.perform(
            post("/pagamentos/" + os + "/simular?aprovado=false")
                .header("Authorization", funcionario))
        .andExpect(jsonPath("$.aplicado").value(true));
    assertThat(ultimoEvento(os)).isEqualTo(TipoMensagemSaga.PAGAMENTO_RECUSADO);
    mvc.perform(get("/pagamentos/" + os).header("Authorization", cliente(11)))
        .andExpect(jsonPath("$.status").value("RECUSADO"));
    mvc.perform(post("/pagamentos/" + os + "/nova-tentativa").header("Authorization", cliente(11)))
        .andExpect(jsonPath("$.status").value("PENDENTE"));
    mvc.perform(post("/pagamentos/" + os + "/nova-tentativa").header("Authorization", cliente(11)))
        .andExpect(status().isConflict());
  }

  @Test
  void rejeicaoComRefazerECancelar() throws Exception {
    String os = os();
    gerarOrcamento(os, 12L);
    mvc.perform(
            post("/orcamentos/" + os + "/rejeitar")
                .header("Authorization", cliente(12))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"REFAZER\",\"motivo\":\"caro\"}"))
        .andExpect(jsonPath("$.status").value("REJEITADO"));
    MensagemSaga rejeitado = eventos.enviadasPara(os).get(0);
    assertThat(rejeitado.texto("acao")).isEqualTo("REFAZER");
    gerarOrcamento(os, 12L);
    mvc.perform(get("/orcamentos/" + os).header("Authorization", cliente(12)))
        .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));
    mvc.perform(
            post("/orcamentos/" + os + "/rejeitar")
                .header("Authorization", cliente(12))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"CANCELAR\"}"))
        .andExpect(status().isOk());
    assertThat(eventos.enviadasPara(os).get(1).texto("acao")).isEqualTo("CANCELAR");
    mvc.perform(
            post("/orcamentos/" + os + "/rejeitar")
                .header("Authorization", cliente(12))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void compensacaoCancelaOrcamentoEIdempotencia() {
    String os = os();
    gerarOrcamento(os, 13L);
    servico.aprovar(os, null);
    MensagemSaga cancelar =
        comando(TipoMensagemSaga.CANCELAR_ORCAMENTO, os, MensagemSaga.dados("motivo", "estoque"));
    assertThat(servico.processar(cancelar)).isTrue();
    assertThat(servico.processar(cancelar)).isFalse();
    assertThat(servico.consultar(os, 13L).getMotivo()).isEqualTo("estoque");
    assertThat(servico.processar(comando(TipoMensagemSaga.SOLICITAR_PAGAMENTO, os, null)))
        .isFalse();
    assertThat(servico.processar(comando(TipoMensagemSaga.CANCELAR_ORCAMENTO, "OS-NADA", null)))
        .isFalse();
    assertThat(servico.processar(comando(TipoMensagemSaga.ORCAMENTO_APROVADO, os, null))).isFalse();
  }

  @Test
  void segurancaEErros() throws Exception {
    String os = os();
    gerarOrcamento(os, 14L);
    mvc.perform(get("/orcamentos/" + os)).andExpect(status().isUnauthorized());
    mvc.perform(get("/orcamentos/" + os).header("Authorization", cliente(99)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/orcamentos/OS-NADA").header("Authorization", funcionario))
        .andExpect(status().isNotFound());
    mvc.perform(get("/pagamentos/" + os).header("Authorization", funcionario))
        .andExpect(status().isNotFound());
    mvc.perform(post("/pagamentos/OS-NADA/simular").header("Authorization", funcionario))
        .andExpect(status().isNotFound());
    mvc.perform(post("/webhooks/mercadopago?type=payment&data.id=1"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
