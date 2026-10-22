package br.com.oficina.orcamentopagamento.usecase.gateway;

import br.com.oficina.orcamentopagamento.domain.model.Pagamento;
import java.util.Optional;

public interface PagamentoRepository {

  Pagamento salvar(Pagamento pagamento);

  /** Tentativa de pagamento mais recente da OS. */
  Optional<Pagamento> ultimo(String numeroOs);
}
