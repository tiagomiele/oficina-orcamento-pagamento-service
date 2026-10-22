package br.com.oficina.orcamentopagamento.domain.model;

public class TransicaoInvalidaException extends RuntimeException {

  public TransicaoInvalidaException(Object atual, Object destino) {
    super("Não é possível ir de " + atual + " para " + destino);
  }
}
