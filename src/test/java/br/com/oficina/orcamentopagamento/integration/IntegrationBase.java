package br.com.oficina.orcamentopagamento.integration;

import br.com.oficina.orcamentopagamento.adapter.messaging.MemoriaMensageriaGateway;
import br.com.oficina.orcamentopagamento.support.Tokens;
import br.com.oficina.orcamentopagamento.usecase.OrcamentoService;
import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;
import br.com.oficina.orcamentopagamento.usecase.saga.TipoMensagemSaga;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationBase {

  @Autowired protected MockMvc mvc;
  @Autowired protected OrcamentoService servico;

  @Autowired(required = false)
  protected MemoriaMensageriaGateway eventos;

  protected final String funcionario = "Bearer " + Tokens.interno("FUNCIONARIO_DA_OFICINA");

  @DynamicPropertySource
  static void chaveCliente(DynamicPropertyRegistry registry) {
    registry.add("oficina.security.jwt.cliente-public-key", Tokens::pemPublico);
  }

  protected static String cliente(long id) {
    return "Bearer " + Tokens.cliente(id);
  }

  protected static MensagemSaga comando(
      TipoMensagemSaga tipo, String os, Map<String, Object> payload) {
    return new MensagemSaga(
        UUID.randomUUID().toString(),
        tipo,
        "corr-" + os,
        os,
        "oficina-os-service",
        Instant.now(),
        payload);
  }

  protected void gerarOrcamento(String os, long idCliente) {
    servico.processar(
        comando(
            TipoMensagemSaga.GERAR_ORCAMENTO,
            os,
            MensagemSaga.dados(
                "idCliente",
                idCliente,
                "valorTotal",
                250.50,
                "itens",
                List.of(
                    Map.of(
                        "tipo",
                        "SERVICO",
                        "idServicoSku",
                        "S-1",
                        "descricao",
                        "Troca de óleo",
                        "quantidade",
                        1,
                        "precoUnitario",
                        100.50),
                    Map.of("tipo", "PECA", "descricao", "Filtro")))));
  }

  protected TipoMensagemSaga ultimoEvento(String os) {
    var lista = eventos.enviadasPara(os);
    return lista.get(lista.size() - 1).tipo();
  }
}
