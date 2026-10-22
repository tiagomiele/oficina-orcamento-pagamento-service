package br.com.oficina.orcamentopagamento.adapter.persistence;

import br.com.oficina.orcamentopagamento.domain.model.Pagamento;
import br.com.oficina.orcamentopagamento.domain.model.StatusPagamento;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaPagamentoRepository implements PagamentoRepository {

  private final SpringDataPagamentoRepository jpa;

  JpaPagamentoRepository(SpringDataPagamentoRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Pagamento salvar(Pagamento p) {
    PagamentoJpaEntity e = new PagamentoJpaEntity();
    e.id = p.getId();
    e.orcamentoId = p.getOrcamentoId();
    e.numeroOs = p.getNumeroOs();
    e.provedor = p.getProvedor();
    e.referenciaExterna = p.getReferenciaExterna();
    e.pagamentoExternoId = p.getPagamentoExternoId();
    e.status = p.getStatus().name();
    e.valor = p.getValor();
    e.linkPagamento = p.getLinkPagamento();
    e.detalhe = p.getDetalhe();
    e.criadoEm = p.getCriadoEm();
    e.atualizadoEm = p.getAtualizadoEm();
    return paraDominio(jpa.save(e));
  }

  @Override
  public Optional<Pagamento> ultimo(String numeroOs) {
    return jpa.findFirstByNumeroOsOrderByIdDesc(numeroOs).map(JpaPagamentoRepository::paraDominio);
  }

  private static Pagamento paraDominio(PagamentoJpaEntity e) {
    return new Pagamento(
        e.id,
        e.orcamentoId,
        e.numeroOs,
        e.provedor,
        e.referenciaExterna,
        e.pagamentoExternoId,
        StatusPagamento.valueOf(e.status),
        e.valor,
        e.linkPagamento,
        e.detalhe,
        e.criadoEm,
        e.atualizadoEm);
  }
}
