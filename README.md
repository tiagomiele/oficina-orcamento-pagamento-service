# oficina-orcamento-pagamento-service
oficina-orcamento-pagamento-service

# Serviço de Orçamento e Pagamento

Orçamento, aprovação/rejeição pelo cliente e pagamento via Mercado Pago (Tech Challenge Fase 4 - SOAT).

Esqueleto criado na Fase 0 (Fundação). As regras de negócio, persistência e mensageria entram nas próximas fases.

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

`verify` roda testes, ArchUnit e o gate do JaCoCo (80% de linhas e branches). O relatório fica em `target/site/jacoco/index.html`.

## Endpoints

- `GET /actuator/health`
- `GET /swagger-ui.html`

## CI

`.github/workflows/ci.yml`: validação dos workflows, build com testes e JaCoCo, SonarCloud (quality gate, quando o secret `SONAR_TOKEN` existir), SBOM CycloneDX e Trivy + Gitleaks.
