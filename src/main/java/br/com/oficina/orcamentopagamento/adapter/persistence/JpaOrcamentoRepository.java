package br.com.oficina.orcamentopagamento.adapter.persistence;

import br.com.oficina.orcamentopagamento.domain.model.ItemOrcamento;
import br.com.oficina.orcamentopagamento.domain.model.Orcamento;
import br.com.oficina.orcamentopagamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamentopagamento.usecase.gateway.OrcamentoRepository;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaOrcamentoRepository implements OrcamentoRepository {

  private final SpringDataOrcamentoRepository jpa;

  JpaOrcamentoRepository(SpringDataOrcamentoRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Orcamento salvar(Orcamento orcamento) {
    return paraDominio(jpa.save(paraEntidade(orcamento)));
  }

  @Override
  public Optional<Orcamento> atual(String numeroOs) {
    return jpa.findFirstByNumeroOsOrderByIdDesc(numeroOs).map(JpaOrcamentoRepository::paraDominio);
  }

  private static OrcamentoJpaEntity paraEntidade(Orcamento o) {
    OrcamentoJpaEntity e = new OrcamentoJpaEntity();
    e.id = o.getId();
    e.numeroOs = o.getNumeroOs();
    e.idCliente = o.getIdCliente();
    e.status = o.getStatus().name();
    e.valorTotal = o.getValorTotal();
    e.correlationId = o.getCorrelationId();
    e.motivo = o.getMotivo();
    e.criadoEm = o.getCriadoEm();
    e.atualizadoEm = o.getAtualizadoEm();
    e.itens = new ArrayList<>();
    for (ItemOrcamento item : o.getItens()) {
      ItemOrcamentoJpa i = new ItemOrcamentoJpa();
      i.tipo = item.tipo();
      i.idServicoSku = item.idServicoSku();
      i.descricao = item.descricao();
      i.quantidade = item.quantidade();
      i.precoUnitario = item.precoUnitario();
      e.itens.add(i);
    }
    return e;
  }

  private static Orcamento paraDominio(OrcamentoJpaEntity e) {
    return new Orcamento(
        e.id,
        e.numeroOs,
        e.idCliente,
        StatusOrcamento.valueOf(e.status),
        e.valorTotal,
        e.itens.stream()
            .map(
                i ->
                    new ItemOrcamento(
                        i.tipo, i.idServicoSku, i.descricao, i.quantidade, i.precoUnitario))
            .toList(),
        e.correlationId,
        e.motivo,
        e.criadoEm,
        e.atualizadoEm);
  }
}
