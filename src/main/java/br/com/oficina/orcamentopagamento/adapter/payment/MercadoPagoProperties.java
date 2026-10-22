package br.com.oficina.orcamentopagamento.adapter.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oficina.pagamento.mercadopago")
public record MercadoPagoProperties(
    String baseUrl,
    String accessToken,
    String publicKey,
    String webhookSecret,
    String notificationUrl,
    boolean sandbox) {}
