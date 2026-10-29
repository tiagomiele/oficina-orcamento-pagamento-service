package br.com.oficina.orcamentopagamento.adapter.persistence;

import br.com.oficina.orcamentopagamento.domain.model.Pagamento;
import br.com.oficina.orcamentopagamento.domain.model.StatusPagamento;
import br.com.oficina.orcamentopagamento.usecase.gateway.PagamentoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaPagamentoRepository implements PagamentoRepository {

  @PersistenceContext private EntityManager em;

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
    if (e.id == null) {
      em.persist(e);
    } else {
      e = em.merge(e);
    }
    em.flush();
    return paraDominio(e);
  }

  @Override
  public Optional<Pagamento> ultimo(String numeroOs) {
    return em
        .createQuery(
            "select p from PagamentoJpaEntity p where p.numeroOs = :numeroOs order by p.id desc",
            PagamentoJpaEntity.class)
        .setParameter("numeroOs", numeroOs)
        .setMaxResults(1)
        .getResultList()
        .stream()
        .findFirst()
        .map(JpaPagamentoRepository::paraDominio);
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
