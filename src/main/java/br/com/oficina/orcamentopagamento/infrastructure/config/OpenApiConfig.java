package br.com.oficina.orcamentopagamento.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  private static final String DESCRICAO =
      "Orçamento e pagamento da OS na Saga orquestrada pelo OS Service.\n\n"
          + "**Token (botão Authorize):** cliente = `accessToken` do `POST /auth/cpf` do Auth"
          + " Serverless (vê só as próprias OS); funcionário/técnico = token do `POST /auth/login`"
          + " do OS Service (vê todas). O webhook do Mercado Pago é público e validado pelo"
          + " header `x-signature`.\n\n"
          + "**Erros:** 400 corpo inválido, 401 sem token ou token expirado (15 min), 403 OS de"
          + " outro cliente, 404 OS sem orçamento/cobrança, 409 transição de status inválida."
          + " 403/404/409 seguem o formato Problem Details (RFC 7807).";

  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Serviço de Orçamento e Pagamento")
                .version("v1")
                .description(DESCRICAO))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
