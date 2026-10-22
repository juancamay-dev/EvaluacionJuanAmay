CREATE TABLE cuenta (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    numero_cuenta   CHAR(10)       NOT NULL,
    titular         VARCHAR(120)   NOT NULL,
    tipo            VARCHAR(10)    NOT NULL,
    saldo           DECIMAL(15,2)  NOT NULL DEFAULT 0,
    estado          VARCHAR(10)    NOT NULL,
    fecha_creacion  DATETIME(6)    NOT NULL,
    CONSTRAINT pk_cuenta PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_numero UNIQUE (numero_cuenta),
    CONSTRAINT ck_cuenta_saldo  CHECK (saldo >= 0),
    CONSTRAINT ck_cuenta_tipo   CHECK (tipo IN ('AHORRO', 'CORRIENTE')),
    CONSTRAINT ck_cuenta_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
) ENGINE = InnoDB;

CREATE TABLE transferencia (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    cuenta_origen_id   BIGINT         NOT NULL,
    cuenta_destino_id  BIGINT         NOT NULL,
    monto              DECIMAL(15,2)  NOT NULL,
    estado             VARCHAR(15)    NOT NULL,
    motivo             VARCHAR(255),
    fecha              DATETIME(6)    NOT NULL,
    CONSTRAINT pk_transferencia PRIMARY KEY (id),
    CONSTRAINT fk_transf_origen  FOREIGN KEY (cuenta_origen_id)  REFERENCES cuenta (id),
    CONSTRAINT fk_transf_destino FOREIGN KEY (cuenta_destino_id) REFERENCES cuenta (id),
    CONSTRAINT ck_transf_monto   CHECK (monto > 0),
    CONSTRAINT ck_transf_estado  CHECK (estado IN ('COMPLETADA', 'RECHAZADA', 'EN_REVISION'))
) ENGINE = InnoDB;

CREATE INDEX idx_transf_origen_fecha  ON transferencia (cuenta_origen_id, fecha);
CREATE INDEX idx_transf_destino_fecha ON transferencia (cuenta_destino_id, fecha);
CREATE INDEX idx_transf_estado_fecha  ON transferencia (estado, fecha);
