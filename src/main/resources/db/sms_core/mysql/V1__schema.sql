/* =====================================================================
   V1 - SmsCore: ESQUEMA (MySQL 8 - perfil dev, base "notificacion")
   ---------------------------------------------------------------------
   Equivalente a sqlserver/V1__schema.sql. Idempotente.
   En DBeaver ejecutar como SCRIPT (Alt+X), no como sentencia (Ctrl+Enter).

   Tipos como los genera Hibernate 6 en MySQL:
     Boolean -> BIT(1) | LocalDateTime -> DATETIME(6) | UUID -> BINARY(16)
     @Lob String -> LONGTEXT
   ===================================================================== */

SET NAMES utf8mb4;

/* ---------------------------- CONFIGURACIÓN ---------------------------- */

CREATE TABLE IF NOT EXISTS SmsProveedor (
  id_sms_proveedor   BIGINT       NOT NULL AUTO_INCREMENT,
  codigo             VARCHAR(30)  NOT NULL,              -- INALAMBRIA, GO4CLIENTS, MOCK (= SmsSenderPort.providerCode())
  nombre             VARCHAR(100) NOT NULL,
  base_url           VARCHAR(255) NOT NULL,
  connect_timeout_ms INT          NOT NULL DEFAULT 2000,
  read_timeout_ms    INT          NOT NULL DEFAULT 5000,
  activo             BIT(1)       NOT NULL DEFAULT b'1',
  fecha_creacion     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_sms_proveedor PRIMARY KEY (id_sms_proveedor),
  CONSTRAINT uq_sms_proveedor_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsCuenta (
  id_sms_cuenta   BIGINT       NOT NULL AUTO_INCREMENT,
  proveedor_id    BIGINT       NOT NULL,
  codigo          VARCHAR(50)  NOT NULL,
  usuario         VARCHAR(100) NULL,      -- valor literal, o 'env:NOMBRE_VARIABLE'
  secret_ref      VARCHAR(100) NOT NULL,  -- NOMBRE de la variable de entorno con la contraseña (nunca la contraseña)
  remitente       VARCHAR(20)  NULL,
  parametros_json LONGTEXT     NULL,      -- parámetros propios de cada proveedor
  activo          BIT(1)       NOT NULL DEFAULT b'1',
  CONSTRAINT pk_sms_cuenta PRIMARY KEY (id_sms_cuenta),
  CONSTRAINT uq_sms_cuenta_codigo UNIQUE (codigo),
  CONSTRAINT fk_sms_cuenta_proveedor FOREIGN KEY (proveedor_id) REFERENCES SmsProveedor (id_sms_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsRuta (
  id_sms_ruta   BIGINT      NOT NULL AUTO_INCREMENT,
  proveedor_id  BIGINT      NULL,          -- NULL = endpoint CENTRAL
  tipo_mensaje  VARCHAR(30) NOT NULL,      -- OTP, HABEAS_DATA, TEMPLATE, PERSONALIZED, NORMAL, MASIVO
  activo        BIT(1)      NOT NULL DEFAULT b'1',
  CONSTRAINT pk_sms_ruta PRIMARY KEY (id_sms_ruta),
  CONSTRAINT fk_sms_ruta_proveedor FOREIGN KEY (proveedor_id) REFERENCES SmsProveedor (id_sms_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsRutaPaso (
  id_sms_ruta_paso BIGINT NOT NULL AUTO_INCREMENT,
  ruta_id          BIGINT NOT NULL,
  orden            INT    NOT NULL,        -- 1, 2, 3... orden de fallback
  cuenta_id        BIGINT NOT NULL,
  max_intentos     INT    NOT NULL DEFAULT 3,
  espera_ms        INT    NOT NULL DEFAULT 500,
  activo           BIT(1) NOT NULL DEFAULT b'1',
  CONSTRAINT pk_sms_ruta_paso PRIMARY KEY (id_sms_ruta_paso),
  CONSTRAINT uq_sms_ruta_paso UNIQUE (ruta_id, orden),
  CONSTRAINT fk_sms_ruta_paso_ruta   FOREIGN KEY (ruta_id)   REFERENCES SmsRuta (id_sms_ruta),
  CONSTRAINT fk_sms_ruta_paso_cuenta FOREIGN KEY (cuenta_id) REFERENCES SmsCuenta (id_sms_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* ------------------------------ LOG DE ENVÍOS ------------------------------ */

CREATE TABLE IF NOT EXISTS SmsEnvio (
  id_sms_envio         BIGINT        NOT NULL AUTO_INCREMENT,
  uuid                 BINARY(16)    NOT NULL,   -- id público (envio_id en la API). Leer con BIN_TO_UUID(uuid)
  idempotency_key      VARCHAR(100)  NULL,
  canal                VARCHAR(20)   NOT NULL,   -- CENTRAL | PROVEEDOR
  proveedor_solicitado VARCHAR(30)   NULL,
  tipo_mensaje         VARCHAR(30)   NOT NULL,
  destino              VARCHAR(20)   NOT NULL,
  mensaje              VARCHAR(1600) NOT NULL,
  estado               VARCHAR(20)   NOT NULL,   -- EN_PROCESO | ENVIADO | FALLIDO
  proveedor_final_id   BIGINT        NULL,
  cuenta_final_id      BIGINT        NULL,
  http_status_final    INT           NULL,
  total_intentos       INT           NOT NULL DEFAULT 0,
  codigo_error         VARCHAR(50)   NULL,
  detalle_error        VARCHAR(1000) NULL,
  fecha_creacion       DATETIME(6)   NOT NULL,
  fecha_finalizacion   DATETIME(6)   NULL,
  CONSTRAINT pk_sms_envio PRIMARY KEY (id_sms_envio),
  CONSTRAINT uq_sms_envio_uuid UNIQUE (uuid),
  CONSTRAINT fk_sms_envio_proveedor FOREIGN KEY (proveedor_final_id) REFERENCES SmsProveedor (id_sms_proveedor),
  CONSTRAINT fk_sms_envio_cuenta    FOREIGN KEY (cuenta_final_id)    REFERENCES SmsCuenta (id_sms_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsEnvioIntento (
  id_sms_envio_intento BIGINT        NOT NULL AUTO_INCREMENT,
  envio_id             BIGINT        NOT NULL,
  paso_orden           INT           NOT NULL,
  nro_intento          INT           NOT NULL,
  proveedor_id         BIGINT        NOT NULL,
  cuenta_id            BIGINT        NOT NULL,
  exitoso              BIT(1)        NOT NULL,
  http_status          INT           NULL,
  detalle              VARCHAR(1000) NULL,
  duracion_ms          INT           NOT NULL,
  fecha                DATETIME(6)   NOT NULL,
  CONSTRAINT pk_sms_envio_intento PRIMARY KEY (id_sms_envio_intento),
  CONSTRAINT fk_sms_intento_envio     FOREIGN KEY (envio_id)     REFERENCES SmsEnvio (id_sms_envio),
  CONSTRAINT fk_sms_intento_proveedor FOREIGN KEY (proveedor_id) REFERENCES SmsProveedor (id_sms_proveedor),
  CONSTRAINT fk_sms_intento_cuenta    FOREIGN KEY (cuenta_id)    REFERENCES SmsCuenta (id_sms_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* ---------------------- ÍNDICES Y COLUMNA AUXILIAR ----------------------
   Separados para que se creen también si Hibernate (ddl-auto=update) creó
   las tablas antes. MySQL no tiene CREATE INDEX IF NOT EXISTS: se consulta
   information_schema y se ejecuta con PREPARE.                            */

-- MySQL permite varios NULL en un UNIQUE: para que solo exista UNA ruta central
-- por tipo se indexa una columna calculada (NULL -> 0). JPA no la conoce ni la escribe.
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND column_name = 'proveedor_key') = 0,
  'ALTER TABLE SmsRuta ADD COLUMN proveedor_key BIGINT AS (IFNULL(proveedor_id, 0)) STORED',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND index_name = 'ux_sms_ruta') = 0,
  'CREATE UNIQUE INDEX ux_sms_ruta ON SmsRuta (proveedor_key, tipo_mensaje)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- En MySQL un UNIQUE normal ya ignora los NULL (equivale al índice filtrado de SQL Server)
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND index_name = 'ux_sms_envio_idem') = 0,
  'CREATE UNIQUE INDEX ux_sms_envio_idem ON SmsEnvio (idempotency_key)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND index_name = 'ix_sms_envio_destino') = 0,
  'CREATE INDEX ix_sms_envio_destino ON SmsEnvio (destino, fecha_creacion)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND index_name = 'ix_sms_envio_estado') = 0,
  'CREATE INDEX ix_sms_envio_estado ON SmsEnvio (estado, fecha_creacion)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenviointento' AND index_name = 'ix_sms_intento_envio') = 0,
  'CREATE INDEX ix_sms_intento_envio ON SmsEnvioIntento (envio_id)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
