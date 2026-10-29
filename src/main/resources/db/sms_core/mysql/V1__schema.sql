/* =====================================================================
   V1 - SmsCore: ESQUEMA (MySQL 8 - perfil dev, base "notificacion")
   ---------------------------------------------------------------------
   Tablas Canal* (configuración compartida con notificacion, filas
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

CREATE TABLE IF NOT EXISTS CanalProveedor (
  id_canal_proveedor  BIGINT       NOT NULL AUTO_INCREMENT,
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
  CONSTRAINT pk_canal_proveedor PRIMARY KEY (id_canal_proveedor),
  CONSTRAINT uq_canal_proveedor UNIQUE (canal, codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS CanalCuenta (
  id_canal_cuenta     BIGINT       NOT NULL AUTO_INCREMENT,
  canal               VARCHAR(10)  NOT NULL DEFAULT 'SMS',
  canal_proveedor_id  BIGINT       NOT NULL,
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
  CONSTRAINT pk_canal_cuenta PRIMARY KEY (id_canal_cuenta),
  CONSTRAINT uq_canal_cuenta UNIQUE (canal, codigo),
  CONSTRAINT fk_canal_cuenta_proveedor FOREIGN KEY (canal_proveedor_id) REFERENCES CanalProveedor (id_canal_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS CanalTipoMensaje (
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
  CONSTRAINT pk_canal_tipo_mensaje PRIMARY KEY (id_tipo_mensaje),
  CONSTRAINT uq_canal_tipo_mensaje UNIQUE (canal, codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS CanalRuta (
  id_ruta            BIGINT      NOT NULL AUTO_INCREMENT,
  canal              VARCHAR(10) NOT NULL DEFAULT 'SMS',
  canal_proveedor_id BIGINT      NULL,          -- NULL = endpoint CENTRAL
  tipo_mensaje_id    BIGINT      NOT NULL,      -- FK -> CanalTipoMensaje
  eliminado          BIT(1)      NOT NULL DEFAULT b'0',
  usr_crea           INT         NOT NULL DEFAULT 1,
  usr_mod            INT         NULL,
  created_at         DATETIME(6) NULL,
  updated_at         DATETIME(6) NULL,
  CONSTRAINT pk_canal_ruta PRIMARY KEY (id_ruta),
  CONSTRAINT fk_canal_ruta_proveedor    FOREIGN KEY (canal_proveedor_id) REFERENCES CanalProveedor (id_canal_proveedor),
  CONSTRAINT fk_canal_ruta_tipo_mensaje FOREIGN KEY (tipo_mensaje_id)    REFERENCES CanalTipoMensaje (id_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS CanalRutaPaso (
  id_ruta_paso     BIGINT      NOT NULL AUTO_INCREMENT,
  canal            VARCHAR(10) NOT NULL DEFAULT 'SMS',
  ruta_id          BIGINT      NOT NULL,
  orden            INT         NOT NULL,        -- 1, 2, 3... orden de fallback
  canal_cuenta_id  BIGINT      NOT NULL,
  max_intentos     INT         NOT NULL DEFAULT 3,
  espera_ms        INT         NOT NULL DEFAULT 500,
  eliminado        BIT(1)      NOT NULL DEFAULT b'0',
  usr_crea         INT         NOT NULL DEFAULT 1,
  usr_mod          INT         NULL,
  created_at       DATETIME(6) NULL,
  updated_at       DATETIME(6) NULL,
  CONSTRAINT pk_canal_ruta_paso PRIMARY KEY (id_ruta_paso),
  CONSTRAINT uq_canal_ruta_paso UNIQUE (ruta_id, orden),
  CONSTRAINT fk_canal_ruta_paso_ruta   FOREIGN KEY (ruta_id)         REFERENCES CanalRuta (id_ruta),
  CONSTRAINT fk_canal_ruta_paso_cuenta FOREIGN KEY (canal_cuenta_id) REFERENCES CanalCuenta (id_canal_cuenta)
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
  CONSTRAINT fk_sms_envio_proveedor FOREIGN KEY (proveedor_final_id) REFERENCES CanalProveedor (id_canal_proveedor),
  CONSTRAINT fk_sms_envio_cuenta    FOREIGN KEY (cuenta_final_id)    REFERENCES CanalCuenta (id_canal_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsEnvioIntento (
  id_sms_envio_intento BIGINT        NOT NULL AUTO_INCREMENT,
  sms_envio_id             BIGINT        NOT NULL,
  paso_orden           INT           NOT NULL,
  nro_intento          INT           NOT NULL,
  canal_proveedor_id   BIGINT        NOT NULL,
  canal_cuenta_id      BIGINT        NOT NULL,
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
  CONSTRAINT fk_sms_intento_proveedor FOREIGN KEY (canal_proveedor_id) REFERENCES CanalProveedor (id_canal_proveedor),
  CONSTRAINT fk_sms_intento_cuenta    FOREIGN KEY (canal_cuenta_id)    REFERENCES CanalCuenta (id_canal_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* ---------------------- ÍNDICES Y COLUMNA AUXILIAR ----------------------
   Separados para que se creen también si Hibernate (ddl-auto=update) creó
   las tablas antes. MySQL no tiene CREATE INDEX IF NOT EXISTS: se consulta
   information_schema y se ejecuta con PREPARE.                            */

-- MySQL permite varios NULL en un UNIQUE: para que solo exista UNA ruta central
-- por tipo se indexa una columna calculada (NULL -> 0). JPA no la conoce ni la escribe.
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'canalruta' AND column_name = 'canal_proveedor_key') = 0,
  'ALTER TABLE CanalRuta ADD COLUMN canal_proveedor_key BIGINT AS (IFNULL(canal_proveedor_id, 0)) STORED',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'canalruta' AND index_name = 'ux_canal_ruta') = 0,
  'CREATE UNIQUE INDEX ux_canal_ruta ON CanalRuta (canal, canal_proveedor_key, tipo_mensaje_id)',
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
