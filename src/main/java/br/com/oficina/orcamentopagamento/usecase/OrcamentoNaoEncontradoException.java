package br.com.oficina.orcamentopagamento.usecase;

public class OrcamentoNaoEncontradoException extends RuntimeException {

  public OrcamentoNaoEncontradoException(String numeroOs) {
    super("Orçamento da OS " + numeroOs + " não encontrado");
  }
}
