/* =====================================================================
   V1 - SmsCore: ESQUEMA (MySQL 8 - perfil dev, base "notificacion")
   ---------------------------------------------------------------------
   Tablas de AdminCanales (configuración compartida con notificacion, filas
   canal = 'SMS') + log propio del gateway (SmsEnvio, SmsEnvioIntento).
   Idempotente. En DBeaver ejecutar como SCRIPT (Alt+X), no sentencia.

   Tipos como los genera Hibernate 6 en MySQL:
     Boolean -> BIT(1) | LocalDateTime -> DATETIME(6) | UUID -> BINARY(16)
     @Lob String -> LONGTEXT

   La columna `canal` (SMS | EMAIL) la administra notificacion; el gateway
   no la escribe (insertable=false). DEFAULT 'SMS' para que los INSERT de
   semilla de este archivo queden en el canal SMS.
   ===================================================================== */

SET NAMES utf8mb4;

/* ---------------------------- CONFIGURACIÓN ---------------------------- */

CREATE TABLE IF NOT EXISTS Proveedor (
  id_proveedor  BIGINT       NOT NULL AUTO_INCREMENT,
  canal               VARCHAR(10)  NOT NULL DEFAULT 'SMS',  -- SMS | EMAIL (lo escribe notificacion)
  codigo              VARCHAR(30)  NOT NULL,                -- INALAMBRIA, GO4CLIENTS, MOCK (= SmsSenderPort.providerCode())
  nombre              VARCHAR(100) NOT NULL,
  base_url            VARCHAR(255) NOT NULL,
  connect_timeout_ms  INT          NOT NULL DEFAULT 2000,
  read_timeout_ms     INT          NOT NULL DEFAULT 5000,
  eliminado           BIT(1)       NOT NULL DEFAULT b'0',
  usr_crea            INT          NOT NULL DEFAULT 1,
  usr_mod             INT          NULL,
  created_at          DATETIME(6)  NULL,
  updated_at          DATETIME(6)  NULL,
  CONSTRAINT pk_proveedor PRIMARY KEY (id_proveedor),
  CONSTRAINT uq_proveedor UNIQUE (canal, codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS Cuenta (
  id_cuenta     BIGINT       NOT NULL AUTO_INCREMENT,
  canal               VARCHAR(10)  NOT NULL DEFAULT 'SMS',
  proveedor_id  BIGINT       NOT NULL,
  codigo              VARCHAR(50)  NOT NULL,
  usuario             VARCHAR(320) NULL,      -- valor literal, o 'env:NOMBRE_VARIABLE'
  secret_ref          VARCHAR(100) NULL,      -- alternativa: NOMBRE de la variable de entorno con la contraseña
  clave_cifrada       VARCHAR(4000) NULL,     -- contraseña cifrada AES-256-GCM ('v1:...'); llave en SMS_CRYPTO_KEY
  remitente           VARCHAR(320) NULL,
  parametros_json     LONGTEXT     NULL,      -- parámetros propios de cada proveedor
  eliminado           BIT(1)       NOT NULL DEFAULT b'0',
  usr_crea            INT          NOT NULL DEFAULT 1,
  usr_mod             INT          NULL,
  created_at          DATETIME(6)  NULL,
  updated_at          DATETIME(6)  NULL,
  CONSTRAINT pk_cuenta PRIMARY KEY (id_cuenta),
  CONSTRAINT uq_cuenta UNIQUE (canal, codigo),
  CONSTRAINT fk_cuenta_proveedor FOREIGN KEY (proveedor_id) REFERENCES Proveedor (id_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS TipoMensaje (
  id_tipo_mensaje BIGINT       NOT NULL AUTO_INCREMENT,
  canal           VARCHAR(10)  NOT NULL DEFAULT 'SMS',  -- canal de configuración (SMS | EMAIL)
  codigo          VARCHAR(30)  NOT NULL,                -- valor de tipo_mensaje en las APIs (OTP, NORMAL, ...)
  descripcion     VARCHAR(255) NULL,
  canal_mensaje   VARCHAR(20)  NOT NULL,                -- canal del mensaje: SMS | WHATSAPP
  otp             BIT(1)       NULL,
  masivo          BIT(1)       NOT NULL DEFAULT b'0',
  eliminado       BIT(1)       NOT NULL DEFAULT b'0',
  usr_crea        INT          NOT NULL DEFAULT 1,
  usr_mod         INT          NULL,
  created_at      DATETIME(6)  NULL,
  updated_at      DATETIME(6)  NULL,
  CONSTRAINT pk_tipo_mensaje PRIMARY KEY (id_tipo_mensaje),
  CONSTRAINT uq_tipo_mensaje UNIQUE (canal, codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS Ruta (
  id_ruta            BIGINT      NOT NULL AUTO_INCREMENT,
  canal              VARCHAR(10) NOT NULL DEFAULT 'SMS',
  proveedor_id BIGINT      NULL,          -- NULL = endpoint CENTRAL
  tipo_mensaje_id    BIGINT      NOT NULL,      -- FK -> TipoMensaje
  eliminado          BIT(1)      NOT NULL DEFAULT b'0',
  usr_crea           INT         NOT NULL DEFAULT 1,
  usr_mod            INT         NULL,
  created_at         DATETIME(6) NULL,
  updated_at         DATETIME(6) NULL,
  CONSTRAINT pk_ruta PRIMARY KEY (id_ruta),
  CONSTRAINT fk_ruta_proveedor    FOREIGN KEY (proveedor_id) REFERENCES Proveedor (id_proveedor),
  CONSTRAINT fk_ruta_tipo FOREIGN KEY (tipo_mensaje_id)    REFERENCES TipoMensaje (id_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS RutaPaso (
  id_ruta_paso     BIGINT      NOT NULL AUTO_INCREMENT,
  canal            VARCHAR(10) NOT NULL DEFAULT 'SMS',
  ruta_id          BIGINT      NOT NULL,
  orden            INT         NOT NULL,        -- 1, 2, 3... orden de fallback
  cuenta_id  BIGINT      NOT NULL,
  max_intentos     INT         NOT NULL DEFAULT 3,
  espera_ms        INT         NOT NULL DEFAULT 500,
  eliminado        BIT(1)      NOT NULL DEFAULT b'0',
  usr_crea         INT         NOT NULL DEFAULT 1,
  usr_mod          INT         NULL,
  created_at       DATETIME(6) NULL,
  updated_at       DATETIME(6) NULL,
  CONSTRAINT pk_ruta_paso PRIMARY KEY (id_ruta_paso),
  CONSTRAINT uq_ruta_paso UNIQUE (ruta_id, orden),
  CONSTRAINT fk_ruta_paso_ruta   FOREIGN KEY (ruta_id)         REFERENCES Ruta (id_ruta),
  CONSTRAINT fk_ruta_paso_cuenta FOREIGN KEY (cuenta_id) REFERENCES Cuenta (id_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* ------------------------------ LOG DE ENVÍOS ------------------------------ */

CREATE TABLE IF NOT EXISTS SmsEnvio (
  id_sms_envio         BIGINT        NOT NULL AUTO_INCREMENT,
  uuid                 BINARY(16)    NOT NULL,   -- id público (sms_envio_id en la API). Leer con BIN_TO_UUID(uuid)
  idempotency_key      VARCHAR(100)  NULL,
  canal                VARCHAR(20)   NOT NULL,   -- CENTRAL | PROVEEDOR
  proveedor_solicitado VARCHAR(30)   NULL,
  tipo_mensaje         VARCHAR(30)   NOT NULL,
  destino              VARCHAR(20)   NOT NULL,
  mensaje              VARCHAR(1600) NOT NULL,
  plantilla_codigo     VARCHAR(50)   NULL,
  estado               VARCHAR(20)   NOT NULL,   -- EN_PROCESO | ENVIADO | FALLIDO
  proveedor_final_id   BIGINT        NULL,
  cuenta_final_id      BIGINT        NULL,
  http_status_final    INT           NULL,
  total_intentos       INT           NOT NULL DEFAULT 0,
  codigo_error         VARCHAR(50)   NULL,
  detalle_error        VARCHAR(1000) NULL,
  fecha_finalizacion   DATETIME(6)   NULL,
  eliminado            BIT(1)        NOT NULL DEFAULT b'0',
  usr_crea             INT           NOT NULL DEFAULT 1,
  usr_mod              INT           NULL,
  created_at           DATETIME(6)   NULL,
  updated_at           DATETIME(6)   NULL,
  CONSTRAINT pk_sms_envio PRIMARY KEY (id_sms_envio),
  CONSTRAINT uq_sms_envio_uuid UNIQUE (uuid),
  CONSTRAINT fk_sms_envio_proveedor FOREIGN KEY (proveedor_final_id) REFERENCES Proveedor (id_proveedor),
  CONSTRAINT fk_sms_envio_cuenta    FOREIGN KEY (cuenta_final_id)    REFERENCES Cuenta (id_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsEnvioIntento (
  id_sms_envio_intento BIGINT        NOT NULL AUTO_INCREMENT,
  sms_envio_id             BIGINT        NOT NULL,
  paso_orden           INT           NOT NULL,
  nro_intento          INT           NOT NULL,
  proveedor_id   BIGINT        NOT NULL,
  cuenta_id      BIGINT        NOT NULL,
  exitoso              BIT(1)        NOT NULL,
  http_status          INT           NULL,
  detalle              VARCHAR(1000) NULL,
  duracion_ms          INT           NOT NULL,
  fecha                DATETIME(6)   NOT NULL,
  eliminado            BIT(1)        NOT NULL DEFAULT b'0',
  usr_crea             INT           NOT NULL DEFAULT 1,
  usr_mod              INT           NULL,
  created_at           DATETIME(6)   NULL,
  updated_at           DATETIME(6)   NULL,
  CONSTRAINT pk_sms_envio_intento PRIMARY KEY (id_sms_envio_intento),
  CONSTRAINT fk_sms_intento_envio     FOREIGN KEY (sms_envio_id)           REFERENCES SmsEnvio (id_sms_envio),
  CONSTRAINT fk_sms_intento_proveedor FOREIGN KEY (proveedor_id) REFERENCES Proveedor (id_proveedor),
  CONSTRAINT fk_sms_intento_cuenta    FOREIGN KEY (cuenta_id)    REFERENCES Cuenta (id_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* ---------------------- ÍNDICES Y COLUMNA AUXILIAR ----------------------
   Separados para que se creen también si Hibernate (ddl-auto=update) creó
   las tablas antes. MySQL no tiene CREATE INDEX IF NOT EXISTS: se consulta
   information_schema y se ejecuta con PREPARE.                            */

-- MySQL permite varios NULL en un UNIQUE: para que solo exista UNA ruta central
-- por tipo se indexa una columna calculada (NULL -> 0). JPA no la conoce ni la escribe.
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'ruta' AND column_name = 'proveedor_key') = 0,
  'ALTER TABLE Ruta ADD COLUMN proveedor_key BIGINT AS (IFNULL(proveedor_id, 0)) STORED',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'ruta' AND index_name = 'ux_ruta') = 0,
  'CREATE UNIQUE INDEX ux_ruta ON Ruta (canal, proveedor_key, tipo_mensaje_id)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- En MySQL un UNIQUE normal ya ignora los NULL (equivale al índice filtrado de SQL Server)
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND index_name = 'ux_sms_envio_idem') = 0,
  'CREATE UNIQUE INDEX ux_sms_envio_idem ON SmsEnvio (idempotency_key)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND index_name = 'ix_sms_envio_destino_created') = 0,
  'CREATE INDEX ix_sms_envio_destino_created ON SmsEnvio (destino, created_at)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND index_name = 'ix_sms_envio_estado_created') = 0,
  'CREATE INDEX ix_sms_envio_estado_created ON SmsEnvio (estado, created_at)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenviointento' AND index_name = 'ix_sms_intento_envio') = 0,
  'CREATE INDEX ix_sms_intento_envio ON SmsEnvioIntento (sms_envio_id)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
