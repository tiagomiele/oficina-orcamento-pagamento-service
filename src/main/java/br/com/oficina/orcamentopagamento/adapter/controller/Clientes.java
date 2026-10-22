package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.adapter.security.AuthenticatedPrincipal;

final class Clientes {

  private Clientes() {}

  /** Cliente só enxerga a própria OS; funcionário e técnico enxergam todas (null). */
  static Long idDo(AuthenticatedPrincipal principal) {
    return principal != null && principal.isClient() ? principal.clientId() : null;
  }
}
