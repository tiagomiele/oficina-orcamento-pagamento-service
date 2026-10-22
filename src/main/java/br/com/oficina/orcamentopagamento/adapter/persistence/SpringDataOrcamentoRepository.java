package br.com.oficina.orcamentopagamento.adapter.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrcamentoRepository extends JpaRepository<OrcamentoJpaEntity, Long> {
  Optional<OrcamentoJpaEntity> findFirstByNumeroOsOrderByIdDesc(String numeroOs);
}
