# oficina-orcamento-pagamento-service
oficina-orcamento-pagamento-service

# Serviço de Orçamento e Pagamento

Orçamento, aprovação/rejeição pelo cliente e pagamento via Mercado Pago (Tech Challenge Fase 4 - SOAT).

Participa da Saga orquestrada pelo OS Service (contrato em [`oficina-os-service/docs/saga/contrato-mensagens.md`](https://github.com/tiagomiele/oficina-os-service/blob/homolog/docs/saga/contrato-mensagens.md)).

## Saga

| Recebe (fila `orcamento-pagamento-comandos`) | Efeito |
|---|---|
| `GERAR_ORCAMENTO` | novo orçamento `AGUARDANDO_APROVACAO` com itens e valor |
| `CANCELAR_ORCAMENTO` | compensação (estoque insuficiente): orçamento `CANCELADO` |
| `SOLICITAR_PAGAMENTO` | `PAGAMENTO_PENDENTE` + cobrança no provedor |

| Ação | Publica (fila `os-eventos`) |
|---|---|
| `POST /orcamentos/{numeroOs}/aprovar` | `ORCAMENTO_APROVADO` |
| `POST /orcamentos/{numeroOs}/rejeitar` `{"acao": "REFAZER" ou "CANCELAR", "motivo": "..."}` | `ORCAMENTO_REJEITADO` |
| `POST /webhooks/mercadopago` (pagamento aprovado) | `PAGAMENTO_CONFIRMADO` (`comprovante` = id do pagamento) |
| `POST /webhooks/mercadopago` (pagamento recusado) | `PAGAMENTO_RECUSADO` |
| `POST /pagamentos/{numeroOs}/nova-tentativa` | — (nova cobrança após recusa) |

Também: `GET /orcamentos/{numeroOs}` e `GET /pagamentos/{numeroOs}` (link de pagamento). O cliente (token do auth-serverless) só acessa as próprias OS; funcionário e técnico (token do OS Service) acessam todas.

- **Postgres + Flyway:** `orcamentos`, `itens_orcamento`, `pagamentos`, `mensagens_processadas` (idempotência por `messageId`).
- **Eventos:** saem só depois do commit. Mensagem com falha vai para a DLQ após 5 tentativas.

## Pagamento

- `PAYMENT_PROVIDER=fake` (padrão): cobrança fictícia; `POST /pagamentos/{numeroOs}/simular?aprovado=true|false` simula a resposta (local e BDD).
- `PAYMENT_PROVIDER=mercadopago`: Checkout Pro (API REST, sem SDK). Cria a preferência com item `id` = número da OS, `external_reference` = número da OS, `notification_url` = `MERCADOPAGO_NOTIFICATION_URL`, `statement_descriptor` = `MERCADOPAGO_STATEMENT_DESCRIPTOR` e, se `MERCADOPAGO_BACK_URL` (HTTPS) estiver preenchida, `back_urls` com `auto_return=approved`. Não envia `payer`: o comprador preenche os dados no checkout. Erros da API (ex.: `invalid_access_token`, `invalid_items`, `invalid_back_urls`) aparecem no log com o código e a mensagem do Mercado Pago, sem o token. O webhook valida o header `x-signature` (HMAC-SHA256 com `MERCADOPAGO_WEBHOOK_SECRET`) e consulta `GET /v1/payments/{id}` antes de aplicar o resultado.
- `MERCADOPAGO_SANDBOX=true` usa o `sandbox_init_point`; `false`, o `init_point`.
- `-ConfigureMercadoPago` do `configure-environment.ps1` cadastra `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET`, `MERCADOPAGO_PUBLIC_KEY` e troca `PAYMENT_PROVIDER`.

## Configuração

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres local | RDS do Orçamento e Pagamento |
| `MENSAGERIA_TIPO` | `memoria` | `sqs` na AWS |
| `SQS_ORCAMENTO_PAGAMENTO_COMANDOS_URL` / `SQS_OS_EVENTOS_URL` | — | filas |
| `AWS_SQS_ENDPOINT` | — | só LocalStack |
| `JWT_SECRET`, `SERVERLESS_JWT_PUBLIC_KEY` | — | mesmos valores do OS Service |
| `PAYMENT_PROVIDER` | `fake` | `mercadopago` liga o Checkout Pro |
| `MERCADOPAGO_ACCESS_TOKEN` / `MERCADOPAGO_WEBHOOK_SECRET` | — | credencial de teste e assinatura do webhook (Secrets do GitHub) |
| `MERCADOPAGO_BACK_URL` | — | HTTPS de retorno do comprador; vazio não envia `back_urls` |
| `MERCADOPAGO_STATEMENT_DESCRIPTOR` | `OFICINA` | nome na fatura do cartão |
| `MERCADOPAGO_SANDBOX` | `true` | `sandbox_init_point` (`true`) ou `init_point` (`false`) |

## Arquitetura

Clean Architecture com 4 camadas, validada por ArchUnit (`ArchitectureTest`):

| Pacote | Responsabilidade |
|---|---|
| `br.com.oficina.orcamentopagamento.domain` | Regras de negócio puras (sem Spring) |
| `br.com.oficina.orcamentopagamento.usecase` | Casos de uso e portas (gateways) |
| `br.com.oficina.orcamentopagamento.adapter` | Controllers, persistência, mensageria e integrações |
| `br.com.oficina.orcamentopagamento.infrastructure` | Configuração e composição |

## Build local

```powershell
./mvnw spotless:apply
./mvnw verify
```

`verify` roda testes (Postgres e SQS do LocalStack via Testcontainers, precisa de Docker), ArchUnit e o gate do JaCoCo (80% de linhas e branches). O relatório fica em `target/site/jacoco/index.html`.

## Endpoints

- `GET /actuator/health`
- `GET /swagger-ui.html`

## CI

`.github/workflows/ci.yml`: validação dos workflows, build com testes e JaCoCo, SonarCloud (quality gate, quando o secret `SONAR_TOKEN` existir), SBOM CycloneDX e Trivy + Gitleaks.

## Deploy (EKS)

`cd.yml` publica a imagem no GHCR e chama `deploy.yml`, que aplica `k8s/` no namespace `oficina-orcamento-pagamento-<env>` do cluster compartilhado, com Service LoadBalancer próprio. O deploy só roda com `DEPLOY_ENABLED=true`, que o `configure-environment.ps1` liga depois do apply do EKS e do banco.
- O deploy do Orçamento e Pagamento envia a URL do LoadBalancer para `orcamento_pagamento_base_url` no workspace do Auth (`scripts/sync-service-url.py`); depois é preciso rodar o apply do Auth para publicar as rotas no API Gateway.
