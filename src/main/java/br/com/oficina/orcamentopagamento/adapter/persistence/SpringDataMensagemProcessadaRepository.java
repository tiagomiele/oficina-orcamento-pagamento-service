package br.com.oficina.orcamentopagamento.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMensagemProcessadaRepository
    extends JpaRepository<MensagemProcessadaJpaEntity, String> {}
