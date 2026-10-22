package br.com.oficina.orcamentopagamento.adapter.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPagamentoRepository extends JpaRepository<PagamentoJpaEntity, Long> {
  Optional<PagamentoJpaEntity> findFirstByNumeroOsOrderByIdDesc(String numeroOs);
}
