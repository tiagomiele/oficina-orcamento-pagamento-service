package br.com.oficina.orcamentopagamento.infrastructure.config;

import br.com.oficina.orcamentopagamento.adapter.payment.MercadoPagoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class PagamentoConfig {}
