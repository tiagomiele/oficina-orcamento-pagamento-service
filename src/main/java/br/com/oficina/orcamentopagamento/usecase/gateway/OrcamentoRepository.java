package br.com.oficina.orcamentopagamento.usecase.gateway;

import br.com.oficina.orcamentopagamento.domain.model.Orcamento;
import java.util.Optional;

public interface OrcamentoRepository {

  Orcamento salvar(Orcamento orcamento);

  /** Orçamento mais recente da OS. */
  Optional<Orcamento> atual(String numeroOs);
}
