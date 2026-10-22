package br.com.oficina.orcamentopagamento.usecase;

import static br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga.dados;

import br.com.oficina.orcamentopagamento.domain.model.AcaoRejeicao;
import br.com.oficina.orcamentopagamento.domain.model.ItemOrcamento;
import br.com.oficina.orcamentopagamento.domain.model.Orcamento;
import br.com.oficina.orcamentopagamento.domain.model.Pagamento;
import br.com.oficina.orcamentopagamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamentopagamento.domain.model.StatusPagamento;
import br.com.oficina.orcamentopagamento.domain.model.TransicaoInvalidaException;
import br.com.oficina.orcamentopagamento.usecase.gateway.MensagemProcessadaRepository;
import br.com.oficina.orcamentopagamento.usecase.gateway.MensageriaGateway;
import br.com.oficina.orcamentopagamento.usecase.gateway.OrcamentoRepository;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway.ResultadoPagamento;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoGateway.Situacao;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoRepository;
import br.com.oficina.orcamentopagamento.usecase.saga.DestinoMensagem;
import br.com.oficina.orcamentopagamento.usecase.saga.MensagemSaga;
import br.com.oficina.orcamentopagamento.usecase.saga.TipoMensagemSaga;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orçamento e pagamento da OS. Recebe os comandos da Saga e publica para o OS Service os eventos de
 * aprovação, rejeição e pagamento.
 */
@Service
public class OrcamentoService {

  private static final Logger log = LoggerFactory.getLogger(OrcamentoService.class);

  private final OrcamentoRepository orcamentos;
  private final PagamentoRepository pagamentos;
  private final MensagemProcessadaRepository processadas;
  private final MensageriaGateway mensageria;
  private final PagamentoGateway provedor;
  private final Clock clock = Clock.systemUTC();

  public OrcamentoService(
      OrcamentoRepository orcamentos,
      PagamentoRepository pagamentos,
      MensagemProcessadaRepository processadas,
      MensageriaGateway mensageria,
      PagamentoGateway provedor) {
    this.orcamentos = orcamentos;
    this.pagamentos = pagamentos;
    this.processadas = processadas;
    this.mensageria = mensageria;
    this.provedor = provedor;
  }

  /**
   * Aplica um comando da fila; retorna false se for duplicado, fora de ordem ou de outro serviço.
   */
  @Transactional
  public boolean processar(MensagemSaga comando) {
    if (comando.tipo().destino() != DestinoMensagem.ORCAMENTO_PAGAMENTO) {
      log.warn(
          "Mensagem {} da OS {} não é comando deste serviço", comando.tipo(), comando.numeroOs());
      return false;
    }
    if (!processadas.registrarSeNova(comando)) {
      log.info("Comando {} já processado (messageId {})", comando.tipo(), comando.messageId());
      return false;
    }
    try {
      switch (comando.tipo()) {
        case GERAR_ORCAMENTO -> gerar(comando);
        case CANCELAR_ORCAMENTO -> cancelar(comando);
        default -> solicitarPagamento(comando);
      }
      return true;
    } catch (TransicaoInvalidaException | OrcamentoNaoEncontradoException e) {
      log.warn(
          "Comando {} da OS {} ignorado: {}", comando.tipo(), comando.numeroOs(), e.getMessage());
      return false;
    }
  }

  private void gerar(MensagemSaga comando) {
    List<ItemOrcamento> itens =
        comando.lista("itens").stream().map(OrcamentoService::item).toList();
    Orcamento orcamento =
        orcamentos.salvar(
            Orcamento.novo(
                comando.numeroOs(),
                comando.numero("idCliente"),
                comando.decimal("valorTotal"),
                itens,
                comando.correlationId(),
                agora()));
    log.info(
        "Orçamento {} da OS {} aguardando aprovação", orcamento.getId(), orcamento.getNumeroOs());
  }

  private static ItemOrcamento item(Map<String, Object> item) {
    Object quantidade = item.get("quantidade");
    Object preco = item.get("precoUnitario");
    return new ItemOrcamento(
        texto(item.get("tipo")),
        texto(item.get("idServicoSku")),
        texto(item.get("descricao")),
        quantidade == null ? 1 : Integer.parseInt(quantidade.toString()),
        preco == null ? BigDecimal.ZERO : new BigDecimal(preco.toString()));
  }

  private static String texto(Object valor) {
    return valor == null ? null : valor.toString();
  }

  private void cancelar(MensagemSaga comando) {
    Orcamento orcamento = obter(comando.numeroOs());
    orcamento.cancelar(comando.texto("motivo"), agora());
    orcamentos.salvar(orcamento);
    log.warn("Compensação: orçamento da OS {} cancelado", comando.numeroOs());
  }

  private void solicitarPagamento(MensagemSaga comando) {
    Orcamento orcamento = obter(comando.numeroOs());
    orcamento.aguardarPagamento(agora());
    cobrar(orcamentos.salvar(orcamento));
  }

  private Pagamento cobrar(Orcamento orcamento) {
    PagamentoGateway.Cobranca cobranca =
        provedor.criarCobranca(
            orcamento.getNumeroOs(),
            orcamento.getValorTotal(),
            "Oficina - OS " + orcamento.getNumeroOs());
    Pagamento pagamento =
        pagamentos.salvar(
            Pagamento.pendente(
                orcamento, provedor.provedor(), cobranca.referencia(), cobranca.link(), agora()));
    log.info(
        "Cobrança {} criada para a OS {} ({})",
        cobranca.referencia(),
        orcamento.getNumeroOs(),
        provedor.provedor());
    return pagamento;
  }

  @Transactional(readOnly = true)
  public Orcamento consultar(String numeroOs, Long idCliente) {
    return doCliente(numeroOs, idCliente);
  }

  @Transactional
  public Orcamento aprovar(String numeroOs, Long idCliente) {
    Orcamento orcamento = doCliente(numeroOs, idCliente);
    orcamento.aprovar(agora());
    Orcamento salvo = orcamentos.salvar(orcamento);
    publicar(
        TipoMensagemSaga.ORCAMENTO_APROVADO, salvo, dados("valorTotal", salvo.getValorTotal()));
    return salvo;
  }

  @Transactional
  public Orcamento rejeitar(String numeroOs, Long idCliente, AcaoRejeicao acao, String motivo) {
    Orcamento orcamento = doCliente(numeroOs, idCliente);
    orcamento.rejeitar(motivo, agora());
    Orcamento salvo = orcamentos.salvar(orcamento);
    publicar(
        TipoMensagemSaga.ORCAMENTO_REJEITADO, salvo, dados("acao", acao.name(), "motivo", motivo));
    return salvo;
  }

  @Transactional(readOnly = true)
  public Pagamento consultarPagamento(String numeroOs, Long idCliente) {
    doCliente(numeroOs, idCliente);
    return pagamentos
        .ultimo(numeroOs)
        .orElseThrow(() -> new OrcamentoNaoEncontradoException(numeroOs));
  }

  /** Pagamento recusado: o cliente pode tentar de novo com uma nova cobrança. */
  @Transactional
  public Pagamento novaTentativa(String numeroOs, Long idCliente) {
    Orcamento orcamento = doCliente(numeroOs, idCliente);
    orcamento.aguardarPagamento(agora());
    return cobrar(orcamentos.salvar(orcamento));
  }

  /** Notificação do Mercado Pago: consulta o pagamento no provedor e aplica o resultado. */
  @Transactional
  public boolean notificacaoDoProvedor(String pagamentoExternoId) {
    return aplicar(provedor.consultar(pagamentoExternoId));
  }

  /** Simulação do provedor fake (local e BDD). */
  @Transactional
  public boolean simular(String numeroOs, boolean aprovado) {
    Pagamento pagamento =
        pagamentos
            .ultimo(numeroOs)
            .orElseThrow(() -> new OrcamentoNaoEncontradoException(numeroOs));
    return aplicar(
        new ResultadoPagamento(
            pagamento.getReferenciaExterna(),
            numeroOs,
            aprovado ? Situacao.APROVADO : Situacao.RECUSADO,
            aprovado ? null : "Pagamento recusado (simulação)"));
  }

  private boolean aplicar(ResultadoPagamento resultado) {
    if (resultado.situacao() == Situacao.PENDENTE || resultado.numeroOs() == null) {
      log.info("Pagamento {} ainda pendente", resultado.pagamentoExternoId());
      return false;
    }
    Optional<Orcamento> atual = orcamentos.atual(resultado.numeroOs());
    Optional<Pagamento> ultimo = pagamentos.ultimo(resultado.numeroOs());
    if (atual.isEmpty()
        || atual.get().getStatus() != StatusOrcamento.PAGAMENTO_PENDENTE
        || ultimo.isEmpty()
        || ultimo.get().getStatus() != StatusPagamento.PENDENTE) {
      log.info(
          "Notificação do pagamento {} já aplicada ou fora de ordem",
          resultado.pagamentoExternoId());
      return false;
    }
    Orcamento orcamento = atual.get();
    Pagamento pagamento = ultimo.get();
    if (resultado.situacao() == Situacao.APROVADO) {
      orcamento.confirmarPagamento(agora());
      pagamento.aprovar(resultado.pagamentoExternoId(), agora());
      Pagamento salvo = pagamentos.salvar(pagamento);
      publicar(
          TipoMensagemSaga.PAGAMENTO_CONFIRMADO,
          orcamentos.salvar(orcamento),
          dados("comprovante", resultado.pagamentoExternoId(), "pagamentoId", salvo.getId()));
      return true;
    }
    orcamento.recusarPagamento(resultado.detalhe(), agora());
    pagamento.recusar(resultado.pagamentoExternoId(), resultado.detalhe(), agora());
    pagamentos.salvar(pagamento);
    publicar(
        TipoMensagemSaga.PAGAMENTO_RECUSADO,
        orcamentos.salvar(orcamento),
        dados("motivo", resultado.detalhe()));
    return true;
  }

  private Orcamento doCliente(String numeroOs, Long idCliente) {
    Orcamento orcamento = obter(numeroOs);
    if (idCliente != null && !orcamento.pertenceAo(idCliente)) {
      throw new AcessoNegadoException(numeroOs);
    }
    return orcamento;
  }

  private Orcamento obter(String numeroOs) {
    return orcamentos
        .atual(numeroOs)
        .orElseThrow(() -> new OrcamentoNaoEncontradoException(numeroOs));
  }

  private void publicar(TipoMensagemSaga tipo, Orcamento orcamento, Map<String, Object> payload) {
    mensageria.enviar(
        MensagemSaga.evento(tipo, orcamento.getNumeroOs(), orcamento.getCorrelationId(), payload));
  }

  private Instant agora() {
    return clock.instant();
  }
}
