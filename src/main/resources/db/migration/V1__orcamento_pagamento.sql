CREATE TABLE orcamentos (
  id             BIGSERIAL     PRIMARY KEY,
  numero_os      VARCHAR(30)   NOT NULL,
  id_cliente     BIGINT,
  status         VARCHAR(30)   NOT NULL,
  valor_total    NUMERIC(12,2) NOT NULL,
  correlation_id VARCHAR(100),
  motivo         VARCHAR(500),
  criado_em      TIMESTAMPTZ   NOT NULL,
  atualizado_em  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_orcamentos_numero_os ON orcamentos(numero_os);

CREATE TABLE itens_orcamento (
  orcamento_id   BIGINT        NOT NULL REFERENCES orcamentos(id),
  tipo           VARCHAR(20),
  id_servico_sku VARCHAR(100),
  descricao      VARCHAR(255),
  quantidade     INTEGER       NOT NULL,
  preco_unitario NUMERIC(12,2) NOT NULL
);
CREATE INDEX idx_itens_orcamento_orcamento ON itens_orcamento(orcamento_id);

CREATE TABLE pagamentos (
  id                   BIGSERIAL     PRIMARY KEY,
  orcamento_id         BIGINT        NOT NULL REFERENCES orcamentos(id),
  numero_os            VARCHAR(30)   NOT NULL,
  provedor             VARCHAR(20)   NOT NULL,
  referencia_externa   VARCHAR(120),
  pagamento_externo_id VARCHAR(120),
  status               VARCHAR(20)   NOT NULL,
  valor                NUMERIC(12,2) NOT NULL,
  link_pagamento       VARCHAR(500),
  detalhe              VARCHAR(255),
  criado_em            TIMESTAMPTZ   NOT NULL,
  atualizado_em        TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_pagamentos_numero_os ON pagamentos(numero_os);

CREATE TABLE mensagens_processadas (
  message_id     VARCHAR(100) PRIMARY KEY,
  tipo           VARCHAR(60)  NOT NULL,
  numero_os      VARCHAR(30)  NOT NULL,
  correlation_id VARCHAR(100),
  processada_em  TIMESTAMPTZ  NOT NULL
);
