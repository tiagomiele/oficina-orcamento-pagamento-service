package br.com.oficina.orcamentopagamento.usecase;

public class AcessoNegadoException extends RuntimeException {

  public AcessoNegadoException(String numeroOs) {
    super("A OS " + numeroOs + " não pertence ao cliente autenticado");
  }
}
