package br.com.oficina.orcamentopagamento.adapter.security;

public record AuthenticatedPrincipal(String subject, String role, Long clientId) {

  public boolean isClient() {
    return "CLIENTE".equals(role) && clientId != null;
  }
}
