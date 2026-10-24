-- SmsCore MySQL: esquema + datos (V1..V14) en orden de ejecución. Base vacía o existente.
-- Idempotente: se puede ejecutar varias veces. En DBeaver: ejecutar como SCRIPT (Alt+X).

-- >>>>>>>>>>>>>>>>>>>> V1__schema.sql
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
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_proveedor PRIMARY KEY (id_sms_proveedor),
  CONSTRAINT uq_sms_proveedor_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsCuenta (
  id_sms_cuenta   BIGINT       NOT NULL AUTO_INCREMENT,
  proveedor_id    BIGINT       NOT NULL,
  codigo          VARCHAR(50)  NOT NULL,
  usuario         VARCHAR(100) NULL,      -- valor literal, o 'env:NOMBRE_VARIABLE'
  secret_ref      VARCHAR(100) NULL,      -- alternativa: NOMBRE de la variable de entorno con la contraseña
  clave_cifrada   VARCHAR(500) NULL,      -- contraseña cifrada AES-256-GCM ('v1:...'); llave en SMS_CRYPTO_KEY
  remitente       VARCHAR(20)  NULL,
  parametros_json LONGTEXT     NULL,      -- parámetros propios de cada proveedor
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_cuenta PRIMARY KEY (id_sms_cuenta),
  CONSTRAINT uq_sms_cuenta_codigo UNIQUE (codigo),
  CONSTRAINT fk_sms_cuenta_proveedor FOREIGN KEY (proveedor_id) REFERENCES SmsProveedor (id_sms_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsTipoMensaje (
  id_sms_tipo_mensaje BIGINT       NOT NULL AUTO_INCREMENT,
  codigo              VARCHAR(30)  NOT NULL,   -- valor de tipo_mensaje en las APIs (OTP, NORMAL, ...)
  descripcion         VARCHAR(255) NULL,
  canal               VARCHAR(20)  NOT NULL,   -- SMS | WHATSAPP
  otp                 BIT(1)       NOT NULL DEFAULT b'0',
  masivo              BIT(1)       NOT NULL DEFAULT b'0',
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_tipo_mensaje PRIMARY KEY (id_sms_tipo_mensaje),
  CONSTRAINT uq_sms_tipo_mensaje_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsRuta (
  id_sms_ruta     BIGINT      NOT NULL AUTO_INCREMENT,
  proveedor_id    BIGINT      NULL,          -- NULL = endpoint CENTRAL
  tipo_mensaje_id BIGINT      NOT NULL,      -- FK -> sms_tipo_mensaje
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_ruta PRIMARY KEY (id_sms_ruta),
  CONSTRAINT fk_sms_ruta_proveedor    FOREIGN KEY (proveedor_id)    REFERENCES SmsProveedor (id_sms_proveedor),
  CONSTRAINT fk_sms_ruta_tipo_mensaje FOREIGN KEY (tipo_mensaje_id) REFERENCES SmsTipoMensaje (id_sms_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsRutaPaso (
  id_sms_ruta_paso BIGINT NOT NULL AUTO_INCREMENT,
  ruta_id          BIGINT NOT NULL,
  orden            INT    NOT NULL,        -- 1, 2, 3... orden de fallback
  cuenta_id        BIGINT NOT NULL,
  max_intentos     INT    NOT NULL DEFAULT 3,
  espera_ms        INT    NOT NULL DEFAULT 500,
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
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
  fecha_finalizacion   DATETIME(6)   NULL,
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
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
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_envio_intento PRIMARY KEY (id_sms_envio_intento),
  CONSTRAINT fk_sms_intento_envio     FOREIGN KEY (envio_id)     REFERENCES SmsEnvio (id_sms_envio),
  CONSTRAINT fk_sms_intento_proveedor FOREIGN KEY (proveedor_id) REFERENCES SmsProveedor (id_sms_proveedor),
  CONSTRAINT fk_sms_intento_cuenta    FOREIGN KEY (cuenta_id)    REFERENCES SmsCuenta (id_sms_cuenta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* ------------- MIGRACIÓN: tablas creadas antes de clave_cifrada ------------- */
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smscuenta' AND column_name = 'clave_cifrada') = 0,
  'ALTER TABLE SmsCuenta ADD COLUMN clave_cifrada VARCHAR(500) NULL AFTER secret_ref',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE SmsCuenta MODIFY secret_ref VARCHAR(100) NULL;

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
  'CREATE UNIQUE INDEX ux_sms_ruta ON SmsRuta (proveedor_key, tipo_mensaje_id)',
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
  'CREATE INDEX ix_sms_intento_envio ON SmsEnvioIntento (envio_id)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V6__tipos_mensaje.sql
/* =====================================================================
   V6 - Catálogo de tipos de mensaje (MySQL). Idempotente.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   sms_tipo_mensaje.codigo es el valor que viaja en sms_ruta.tipo_mensaje
   y en las peticiones de envío. Las capacidades (canal, otp, masivo) se
   administran aquí por CRUD; no hace falta crear un tipo por proveedor.
   ===================================================================== */

CREATE TABLE IF NOT EXISTS SmsTipoMensaje (
  id_sms_tipo_mensaje BIGINT       NOT NULL AUTO_INCREMENT,
  codigo              VARCHAR(30)  NOT NULL,   -- valor de sms_ruta.tipo_mensaje (OTP, NORMAL, ...)
  descripcion         VARCHAR(255) NULL,
  canal               VARCHAR(20)  NOT NULL,   -- SMS | WHATSAPP
  otp                 BIT(1)       NOT NULL DEFAULT b'0',  -- lleva código/PIN de un solo uso
  masivo              BIT(1)       NOT NULL DEFAULT b'0',  -- apto para envíos masivos
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_tipo_mensaje PRIMARY KEY (id_sms_tipo_mensaje),
  CONSTRAINT uq_sms_tipo_mensaje_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semilla: mismos códigos que ya usan sms_ruta y el enum MessageType
INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, eliminado, usr_crea)
SELECT v.codigo, v.descripcion, v.canal, v.otp, v.masivo, 0, 1
FROM (
  SELECT 'OTP'          AS codigo, 'Código de verificación de un solo uso'      AS descripcion, 'SMS' AS canal, b'1' AS otp, b'0' AS masivo
  UNION ALL SELECT 'HABEAS_DATA',  'Notificación judicial / habeas data',               'SMS', b'0', b'0'
  UNION ALL SELECT 'TEMPLATE',     'Mensaje basado en plantilla del proveedor',         'SMS', b'0', b'0'
  UNION ALL SELECT 'PERSONALIZED', 'Mensaje con contenido personalizado por destino',   'SMS', b'0', b'0'
  UNION ALL SELECT 'NORMAL',       'Mensaje estándar / masivo',                         'SMS', b'0', b'1'
  UNION ALL SELECT 'MASIVO',       'Envío masivo',                                      'SMS', b'0', b'1'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje t WHERE t.codigo = v.codigo);

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V7__ruta_tipo_mensaje_fk.sql
/* =====================================================================
   V7 - sms_ruta.tipo_mensaje -> tipo_mensaje_id (FK a sms_tipo_mensaje).
   Idempotente. Requiere V6 (catálogo poblado). Ejecutar como SCRIPT (Alt+X).

   Pasos: crea tipos faltantes -> agrega columna -> backfill por código ->
   reemplaza el índice único -> agrega FK -> elimina la columna vieja.
   ===================================================================== */

-- 1. Garantiza que todos los códigos usados en sms_ruta existan en el catálogo
--    (solo si la columna vieja todavía existe)
SET @existe = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND column_name = 'tipo_mensaje');
SET @sql = IF(@existe > 0,
  'INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, eliminado, usr_crea)
   SELECT DISTINCT r.tipo_mensaje, CONCAT(''Migrado desde sms_ruta: '', r.tipo_mensaje), ''SMS'', b''0'', b''0'', 0, 1
   FROM SmsRuta r
   WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje t WHERE t.codigo = r.tipo_mensaje)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 2. Agrega la columna FK (si no existe)
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND column_name = 'tipo_mensaje_id') = 0,
  'ALTER TABLE SmsRuta ADD COLUMN tipo_mensaje_id BIGINT NULL AFTER proveedor_id',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 3. Backfill por código (solo si la columna vieja sigue existiendo)
SET @sql = IF(@existe > 0,
  'UPDATE SmsRuta r JOIN SmsTipoMensaje t ON t.codigo = r.tipo_mensaje
      SET r.tipo_mensaje_id = t.id_sms_tipo_mensaje WHERE r.tipo_mensaje_id IS NULL',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 4. NOT NULL + FK
SET @sql = IF(@existe > 0,
  'ALTER TABLE SmsRuta MODIFY COLUMN tipo_mensaje_id BIGINT NOT NULL',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta'
                 AND constraint_name = 'fk_sms_ruta_tipo_mensaje') = 0,
  'ALTER TABLE SmsRuta ADD CONSTRAINT fk_sms_ruta_tipo_mensaje
       FOREIGN KEY (tipo_mensaje_id) REFERENCES SmsTipoMensaje (id_sms_tipo_mensaje)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 5. Índice único (proveedor_key, tipo): lo recrea sobre tipo_mensaje_id
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND index_name = 'ux_sms_ruta') > 0,
  'DROP INDEX ux_sms_ruta ON SmsRuta',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = 'CREATE UNIQUE INDEX ux_sms_ruta ON SmsRuta (proveedor_key, tipo_mensaje_id)';
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 6. Elimina la columna vieja (último paso: solo cuando el backfill ya corrió)
SET @sql = IF(@existe > 0,
  'ALTER TABLE SmsRuta DROP COLUMN tipo_mensaje',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V13__auditoria_soft_delete.sql
-- =============================================================================
-- V13 · Auditoría + soft delete (AuditInfo) en las tablas de configuración
--       SmsProveedor, SmsCuenta, SmsTipoMensaje, SmsRuta, SmsRutaPaso, SmsProveedorServicio
-- -----------------------------------------------------------------------------
-- AuditInfo mapea `eliminado` con @SoftDelete e insertable=false: Hibernate NUNCA
-- escribe la columna y filtra con `eliminado = 0`. Si queda NULL (columna creada
-- por ddl-auto=update sin DEFAULT), las filas quedan invisibles (404 / RUTA_NO_CONFIGURADA).
--
-- Deja en cada tabla:
--   eliminado  TINYINT(1)  NOT NULL DEFAULT 0     (NULL -> 0)
--   usr_crea   INT         NOT NULL DEFAULT 1     (NULL/0 -> 1)
--   usr_mod    INT         NULL
--   created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)  (backfill desde fecha_creacion)
--   updated_at DATETIME(6) NULL
-- La aplicación ya no usa `activo` ni `fecha_creacion`; si existen en la base se ignoran.
-- EJECUTAR en una base existente ANTES de los scripts de datos (V2-V5, V8-V12) y otra vez al final.
-- Idempotente. Compatible con MySQL 5.7 y 8.0.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_v13_columna;
DELIMITER $$
-- Ejecuta p_sql solo si la tabla existe y la columna (no) existe según p_debe_existir
CREATE PROCEDURE sp_v13_columna(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64),
                                IN p_debe_existir TINYINT, IN p_sql TEXT)
BEGIN
  DECLARE v_tabla INT DEFAULT 0;
  DECLARE v_col INT DEFAULT 0;
  SELECT COUNT(*) INTO v_tabla FROM information_schema.tables
   WHERE table_schema = DATABASE() AND table_name = p_tabla;
  SELECT COUNT(*) INTO v_col FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_tabla AND column_name = p_columna;
  IF v_tabla > 0 AND ((p_debe_existir = 1 AND v_col > 0) OR (p_debe_existir = 0 AND v_col = 0)) THEN
    SET @s = p_sql;
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$

DROP PROCEDURE IF EXISTS sp_v13_auditoria$$
CREATE PROCEDURE sp_v13_auditoria(IN p_tabla VARCHAR(64))
BEGIN
  -- eliminado
  CALL sp_v13_columna(p_tabla, 'eliminado', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  CALL sp_v13_columna(p_tabla, 'eliminado', 1,
       CONCAT('UPDATE `', p_tabla, '` SET eliminado = 0 WHERE eliminado IS NULL'));
  CALL sp_v13_columna(p_tabla, 'eliminado', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  -- usr_crea / usr_mod
  CALL sp_v13_columna(p_tabla, 'usr_crea', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v13_columna(p_tabla, 'usr_crea', 1,
       CONCAT('UPDATE `', p_tabla, '` SET usr_crea = 1 WHERE usr_crea IS NULL OR usr_crea = 0'));
  CALL sp_v13_columna(p_tabla, 'usr_crea', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v13_columna(p_tabla, 'usr_mod', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_mod INT NULL'));
  -- created_at / updated_at
  CALL sp_v13_columna(p_tabla, 'created_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v13_columna(p_tabla, 'created_at', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v13_columna(p_tabla, 'updated_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN updated_at DATETIME(6) NULL'));
  CALL sp_v13_columna(p_tabla, 'fecha_creacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET created_at = fecha_creacion WHERE created_at IS NULL'));
  CALL sp_v13_columna(p_tabla, 'fecha_modificacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET updated_at = fecha_modificacion WHERE updated_at IS NULL'));
END$$
DELIMITER ;

CALL sp_v13_auditoria('SmsProveedor');
CALL sp_v13_auditoria('SmsCuenta');
CALL sp_v13_auditoria('SmsTipoMensaje');
CALL sp_v13_auditoria('SmsRuta');
CALL sp_v13_auditoria('SmsRutaPaso');
CALL sp_v13_auditoria('SmsProveedorServicio');

DROP PROCEDURE IF EXISTS sp_v13_auditoria;
DROP PROCEDURE IF EXISTS sp_v13_columna;

-- Verificación: no debe quedar ningún eliminado NULL
SELECT 'SmsProveedor' AS tabla, SUM(eliminado IS NULL) AS nulos, COUNT(*) AS filas FROM SmsProveedor
UNION ALL SELECT 'SmsCuenta',      SUM(eliminado IS NULL), COUNT(*) FROM SmsCuenta
UNION ALL SELECT 'SmsTipoMensaje', SUM(eliminado IS NULL), COUNT(*) FROM SmsTipoMensaje
UNION ALL SELECT 'SmsRuta',        SUM(eliminado IS NULL), COUNT(*) FROM SmsRuta
UNION ALL SELECT 'SmsRutaPaso',    SUM(eliminado IS NULL), COUNT(*) FROM SmsRutaPaso;

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V14__auditoria_envios_plantillas.sql
-- =============================================================================
-- V14 · AuditInfo en SmsEnvio, SmsEnvioIntento, SmsPlantilla y SmsPlantillaVariable
-- -----------------------------------------------------------------------------
-- Estas entidades ahora extienden AuditInfo (@SoftDelete en `eliminado`,
-- usr_crea/usr_mod, created_at/updated_at). Requiere V13.
--
--   1. eliminado TINYINT(1) NOT NULL DEFAULT 0 y usr_crea INT NOT NULL DEFAULT 1
--      (si `eliminado` queda NULL, Hibernate no vuelve a encontrar el envío recién
--      creado y no registra intentos ni el resultado final).
--   2. created_at / updated_at si no existen, y backfill desde fecha_creacion /
--      fecha_modificacion.
--   3. SmsEnvio.fecha_creacion con DEFAULT CURRENT_TIMESTAMP(6): la aplicación ya
--      no la escribe (usa created_at) y sin default el INSERT falla.
--   4. Índices de consulta de envíos sobre created_at.
-- Idempotente. Compatible con MySQL 5.7 y 8.0.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_v14_columna;
DELIMITER $$
-- Ejecuta p_sql solo si la tabla existe y la columna (no) existe según p_debe_existir
CREATE PROCEDURE sp_v14_columna(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64),
                                IN p_debe_existir TINYINT, IN p_sql TEXT)
BEGIN
  DECLARE v_tabla INT DEFAULT 0;
  DECLARE v_col INT DEFAULT 0;
  SELECT COUNT(*) INTO v_tabla FROM information_schema.tables
   WHERE table_schema = DATABASE() AND table_name = p_tabla;
  SELECT COUNT(*) INTO v_col FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_tabla AND column_name = p_columna;
  IF v_tabla > 0 AND ((p_debe_existir = 1 AND v_col > 0) OR (p_debe_existir = 0 AND v_col = 0)) THEN
    SET @s = p_sql;
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$

DROP PROCEDURE IF EXISTS sp_v14_auditoria$$
CREATE PROCEDURE sp_v14_auditoria(IN p_tabla VARCHAR(64))
BEGIN
  -- eliminado
  CALL sp_v14_columna(p_tabla, 'eliminado', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  CALL sp_v14_columna(p_tabla, 'eliminado', 1,
       CONCAT('UPDATE `', p_tabla, '` SET eliminado = 0 WHERE eliminado IS NULL'));
  CALL sp_v14_columna(p_tabla, 'eliminado', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  -- usr_crea / usr_mod
  CALL sp_v14_columna(p_tabla, 'usr_crea', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v14_columna(p_tabla, 'usr_crea', 1,
       CONCAT('UPDATE `', p_tabla, '` SET usr_crea = 1 WHERE usr_crea IS NULL OR usr_crea = 0'));
  CALL sp_v14_columna(p_tabla, 'usr_crea', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v14_columna(p_tabla, 'usr_mod', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_mod INT NULL'));
  -- created_at / updated_at
  CALL sp_v14_columna(p_tabla, 'created_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v14_columna(p_tabla, 'created_at', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v14_columna(p_tabla, 'updated_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN updated_at DATETIME(6) NULL'));
  CALL sp_v14_columna(p_tabla, 'fecha_creacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET created_at = fecha_creacion WHERE created_at IS NULL'));
  CALL sp_v14_columna(p_tabla, 'fecha_modificacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET updated_at = fecha_modificacion WHERE updated_at IS NULL'));
END$$
DELIMITER ;

CALL sp_v14_auditoria('SmsEnvio');
CALL sp_v14_auditoria('SmsEnvioIntento');
CALL sp_v14_auditoria('SmsPlantilla');
CALL sp_v14_auditoria('SmsPlantillaVariable');

-- SmsEnvioIntento: created_at desde su columna `fecha`
CALL sp_v14_columna('SmsEnvioIntento', 'fecha', 1,
     'UPDATE SmsEnvioIntento SET created_at = fecha WHERE created_at IS NULL');

-- SmsEnvio.fecha_creacion ya no la escribe la aplicación
CALL sp_v14_columna('SmsEnvio', 'fecha_creacion', 1,
     'ALTER TABLE SmsEnvio MODIFY COLUMN fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');

DROP PROCEDURE IF EXISTS sp_v14_auditoria;
DROP PROCEDURE IF EXISTS sp_v14_columna;

-- Índices de consulta (/v1/sms/envios) sobre created_at
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'SmsEnvio' AND index_name = 'ix_sms_envio_destino_created') = 0,
  'CREATE INDEX ix_sms_envio_destino_created ON SmsEnvio (destino, created_at)', 'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'SmsEnvio' AND index_name = 'ix_sms_envio_estado_created') = 0,
  'CREATE INDEX ix_sms_envio_estado_created ON SmsEnvio (estado, created_at)', 'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación (solo tablas existentes): eliminado debe quedar NOT NULL DEFAULT 0
SELECT table_name AS tabla, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND column_name = 'eliminado'
  AND table_name IN ('SmsEnvio', 'SmsEnvioIntento', 'SmsPlantilla', 'SmsPlantillaVariable');

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V2__seed_inalambria.sql
/* =====================================================================
   V2 - Proveedor INALAMBRIA (MySQL). Idempotente. Requiere V1.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   Contraseñas CIFRADAS (AES-256-GCM) en clave_cifrada. Se descifran al enviar con la llave
   de la variable de entorno SMS_CRYPTO_KEY (nunca en BD ni en el repositorio).
   Si cambia la llave, vuelva a cifrar (ClaveCifradaCli o PUT /v1/sms/admin/cuentas/{id} con 'clave').
   ===================================================================== */

-- 1. Proveedor
INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, usr_crea, created_at)
SELECT 'INALAMBRIA', 'Inalambria', 'https://restaws.inalambria.com', 3000, 10000, 0, 1, NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'INALAMBRIA');

-- 2. Cuentas

INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'INALAMBRIA'), v.codigo, v.usuario, NULL, v.clave_cifrada, NULL, v.parametros_json, 0, 1
FROM (
  SELECT 'INALAMBRIA_OTP' AS codigo, 'COMFENALCOquindioOTP' AS usuario,
         'v1:MOIs/G3Q9IWGgpOqaYikpP3lactcYqEPCVnrrCtPhKF+0Xc=' AS clave_cifrada,
         '{"type":1,"messagePattern":"0","templateId":0}' AS parametros_json
  UNION ALL SELECT 'INALAMBRIA_MASIVO',        'COMFENALCOquindioMASIVO',
         'v1:zqg+2HKF6qCl2NTp2pQlnWwrrnGXtzu+2Iwj2VWSwKCGE3gVyd8=', '{"type":1,"messagePattern":"0","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_HABEASDATA',    'COMFENALCOquindioCOMUNICADOR',
         'v1:Wpp8p25Jr6cif+9OrAWOraLPR9vfw4pE6oMxGcmjoaldjQft', '{"type":1,"messagePattern":"0","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_TEMPLATE',      'COMFENALCOquindioOTP',
         'v1:g04hQjF0uvZaUSCrcNO4OWJLuIMoJrq5D5KUuMWqehGew8U=', '{"type":1,"messagePattern":"1","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_PERSONALIZADA', 'COMFENALCOquindioOTP',
         'v1:POKjUCKJbM1z/8VwYNPztUKTleO0Kf85gNEi5cG0Eh18bWs=', '{"type":1,"messagePattern":"1","templateId":0}'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta c WHERE c.codigo = v.codigo);

-- 3. Rutas del endpoint propio  POST /v1/sms/inalambria/send
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT 'INALAMBRIA', 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT 'INALAMBRIA', 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT 'INALAMBRIA', 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT 'INALAMBRIA', 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT 'INALAMBRIA', 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT 'INALAMBRIA', 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT 'INALAMBRIA', 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT 'INALAMBRIA', 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V3__seed_mock.sql
/* =====================================================================
   V3 - Proveedor MOCK (MySQL, solo pruebas). Idempotente. Requiere V1.
   No hace llamadas HTTP. parametros_json.simulateStatus = HTTP simulado.
   ===================================================================== */

INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, usr_crea, created_at)
SELECT 'MOCK', 'Mock (pruebas)', 'https://mock.local', 1000, 1000, 0, 1, NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'MOCK');


INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'MOCK'), v.codigo, 'mock', 'SMS_MOCK_PASSWORD', NULL, v.parametros_json, 0, 1
FROM (
  SELECT 'MOCK_OK' AS codigo, '{"simulateStatus":200}' AS parametros_json
  UNION ALL SELECT 'MOCK_FALLA', '{"simulateStatus":500}'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta c WHERE c.codigo = v.codigo);

-- Rutas del endpoint propio  POST /v1/sms/mock/send
-- OTP: la cuenta que falla va primero -> 3 intentos fallidos + fallback a MOCK_OK
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'MOCK' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'MOCK_FALLA' AS cuenta
  UNION ALL SELECT 'MOCK', 'OTP', 2, 'MOCK_OK'
  UNION ALL SELECT 'MOCK', 'NORMAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, 0, 1
FROM (
SELECT 'MOCK' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'MOCK_FALLA' AS cuenta
  UNION ALL SELECT 'MOCK', 'OTP', 2, 'MOCK_OK'
  UNION ALL SELECT 'MOCK', 'NORMAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V4__seed_go4clients.sql
/* =====================================================================
   V4 - Proveedor GO4CLIENTS (MySQL). Idempotente. Requiere V1.
   Credenciales en variables de entorno (no en BD):
     SmsCuenta.usuario    = 'env:GO4CLIENTS_API_KEY'  -> api key
     SmsCuenta.clave_cifrada = api secret cifrado (AES-256-GCM, llave SMS_CRYPTO_KEY)
   ===================================================================== */

INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, usr_crea, created_at)
SELECT 'GO4CLIENTS', 'Go4Clients', 'https://cloud.go4clients.com:8580/api', 3000, 15000, 0, 1, NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'GO4CLIENTS');


INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'GO4CLIENTS'), 'GO4CLIENTS_PRINCIPAL', 'env:GO4CLIENTS_API_KEY', NULL,
         'v1:y8o3E8dikzW62ZxJyJAaUiOhQWHdvwwdYEb/UqlBdwtNumcQNzAo5Iyvnxc=', 'RANDOM', '{"priority":"HIGH"}', 0, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta WHERE codigo = 'GO4CLIENTS_PRINCIPAL');

-- Rutas del endpoint propio  POST /v1/sms/go4clients/send
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'GO4CLIENTS' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'GO4CLIENTS_PRINCIPAL' AS cuenta
  UNION ALL SELECT 'GO4CLIENTS', 'NORMAL', 1, 'GO4CLIENTS_PRINCIPAL'
  UNION ALL SELECT 'GO4CLIENTS', 'MASIVO', 1, 'GO4CLIENTS_PRINCIPAL'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, 0, 1
FROM (
SELECT 'GO4CLIENTS' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'GO4CLIENTS_PRINCIPAL' AS cuenta
  UNION ALL SELECT 'GO4CLIENTS', 'NORMAL', 1, 'GO4CLIENTS_PRINCIPAL'
  UNION ALL SELECT 'GO4CLIENTS', 'MASIVO', 1, 'GO4CLIENTS_PRINCIPAL'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V5__rutas_centrales.sql
/* =====================================================================
   V5 - Rutas del ENDPOINT CENTRAL (MySQL)  POST /v1/sms/send y /v1/sms/batch
   (SmsRuta.proveedor_id = NULL). Idempotente. Requiere V1 + V2.
   Cada paso = una cuenta; el motor hace max_intentos (3) en esa cuenta y
   luego pasa al siguiente orden. NO pisa pasos existentes: para cambiar
   una ruta use SmsAdmin (/v1/sms/admin/rutas) o un UPDATE.
   ===================================================================== */

-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)
 -- Opcional, Go4Clients como respaldo (requiere V4): agregar como UNION ALL en los dos INSERT
 -- (NULL, 'OTP', 3, 'GO4CLIENTS_PRINCIPAL'), (NULL, 'NORMAL', 2, 'GO4CLIENTS_PRINCIPAL'), (NULL, 'MASIVO', 2, 'GO4CLIENTS_PRINCIPAL')

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT NULL AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT NULL, 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT NULL, 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT NULL, 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT NULL, 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, 0, 1
FROM (
SELECT NULL AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT NULL, 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT NULL, 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT NULL, 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT NULL, 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V6__claves_cifradas.sql
/* =====================================================================
   V6 - Claves CIFRADAS para cuentas existentes (MySQL). Idempotente.
   Para bases que ya tenían las cuentas con secret_ref (V2/V4 anteriores).

   - Agrega la columna clave_cifrada si falta y deja secret_ref opcional.
   - Guarda la contraseña cifrada (AES-256-GCM). Se descifra al enviar con la
     llave de la variable de entorno SMS_CRYPTO_KEY (no está en BD ni en git).
   - secret_ref NO se borra: queda como respaldo si la clave no se puede descifrar.
     Para usar solo la clave cifrada, ejecute el UPDATE opcional del final.
   ===================================================================== */

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smscuenta' AND column_name = 'clave_cifrada') = 0,
  'ALTER TABLE SmsCuenta ADD COLUMN clave_cifrada VARCHAR(500) NULL AFTER secret_ref',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE SmsCuenta MODIFY secret_ref VARCHAR(100) NULL;

UPDATE SmsCuenta SET clave_cifrada = 'v1:MOIs/G3Q9IWGgpOqaYikpP3lactcYqEPCVnrrCtPhKF+0Xc='
WHERE codigo = 'INALAMBRIA_OTP';
UPDATE SmsCuenta SET clave_cifrada = 'v1:zqg+2HKF6qCl2NTp2pQlnWwrrnGXtzu+2Iwj2VWSwKCGE3gVyd8='
WHERE codigo = 'INALAMBRIA_MASIVO';
UPDATE SmsCuenta SET clave_cifrada = 'v1:Wpp8p25Jr6cif+9OrAWOraLPR9vfw4pE6oMxGcmjoaldjQft'
WHERE codigo = 'INALAMBRIA_HABEASDATA';
UPDATE SmsCuenta SET clave_cifrada = 'v1:g04hQjF0uvZaUSCrcNO4OWJLuIMoJrq5D5KUuMWqehGew8U='
WHERE codigo = 'INALAMBRIA_TEMPLATE';
UPDATE SmsCuenta SET clave_cifrada = 'v1:POKjUCKJbM1z/8VwYNPztUKTleO0Kf85gNEi5cG0Eh18bWs='
WHERE codigo = 'INALAMBRIA_PERSONALIZADA';
UPDATE SmsCuenta SET clave_cifrada = 'v1:y8o3E8dikzW62ZxJyJAaUiOhQWHdvwwdYEb/UqlBdwtNumcQNzAo5Iyvnxc='
WHERE codigo = 'GO4CLIENTS_PRINCIPAL';

-- Verificación (no muestra la clave, solo si está cargada)
SELECT codigo, secret_ref, clave_cifrada IS NOT NULL AS tiene_clave_cifrada, LENGTH(clave_cifrada) AS largo
FROM SmsCuenta WHERE codigo IN ('INALAMBRIA_OTP', 'INALAMBRIA_MASIVO', 'INALAMBRIA_HABEASDATA', 'INALAMBRIA_TEMPLATE', 'INALAMBRIA_PERSONALIZADA', 'GO4CLIENTS_PRINCIPAL') ORDER BY codigo;

/* Opcional: usar SOLO la clave cifrada (sin respaldo por variable de entorno)
UPDATE SmsCuenta SET secret_ref = NULL WHERE clave_cifrada IS NOT NULL;
*/

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V8__servicios_proveedor.sql
/* =====================================================================
   V8 - Servicios por proveedor (MySQL). Idempotente. Requiere V1 + V6__tipos_mensaje.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   SmsProveedorServicio = (proveedor, tipo de mensaje) que el proveedor SÍ atiende.
   El motor usa esta tabla para:
     - Responder 422 SERVICIO_NO_SOPORTADO si se pide un tipo a un proveedor que no lo tiene
       (endpoint del proveedor, 'proveedor' preferido o 'cuenta' en el central).
     - Saltar en el fallback las cuentas de proveedores sin el MISMO tipo de envío.
   SmsAdmin: GET/PUT /v1/sms/admin/proveedores/{id}/servicios

   IMPORTANTE: sin filas, el proveedor no atiende NINGÚN tipo. Ejecutar antes de desplegar.
   ===================================================================== */

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS SmsProveedorServicio (
  id_sms_proveedor_servicio BIGINT      NOT NULL AUTO_INCREMENT,
  proveedor_id              BIGINT      NOT NULL,
  tipo_mensaje_id           BIGINT      NOT NULL,
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_proveedor_servicio PRIMARY KEY (id_sms_proveedor_servicio),
  CONSTRAINT uq_sms_proveedor_servicio UNIQUE (proveedor_id, tipo_mensaje_id),
  CONSTRAINT fk_sms_prov_servicio_proveedor FOREIGN KEY (proveedor_id)    REFERENCES SmsProveedor (id_sms_proveedor),
  CONSTRAINT fk_sms_prov_servicio_tipo      FOREIGN KEY (tipo_mensaje_id) REFERENCES SmsTipoMensaje (id_sms_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semilla. Go4Clients NO tiene OTP: un OTP por Go4Clients responde
-- "El proveedor GO4CLIENTS no cuenta con el servicio OTP" (su ruta GO4CLIENTS/OTP de V4 queda sin uso).
INSERT INTO SmsProveedorServicio (proveedor_id, tipo_mensaje_id, eliminado, usr_crea, created_at)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 0, 1, NOW(6)
FROM (
  SELECT 'INALAMBRIA' AS proveedor, 'OTP' AS tipo
  UNION ALL SELECT 'INALAMBRIA', 'HABEAS_DATA'
  UNION ALL SELECT 'INALAMBRIA', 'TEMPLATE'
  UNION ALL SELECT 'INALAMBRIA', 'PERSONALIZED'
  UNION ALL SELECT 'INALAMBRIA', 'NORMAL'
  UNION ALL SELECT 'INALAMBRIA', 'MASIVO'
  UNION ALL SELECT 'GO4CLIENTS', 'NORMAL'
  UNION ALL SELECT 'GO4CLIENTS', 'MASIVO'
  UNION ALL SELECT 'MOCK', 'OTP'
  UNION ALL SELECT 'MOCK', 'HABEAS_DATA'
  UNION ALL SELECT 'MOCK', 'TEMPLATE'
  UNION ALL SELECT 'MOCK', 'PERSONALIZED'
  UNION ALL SELECT 'MOCK', 'NORMAL'
  UNION ALL SELECT 'MOCK', 'MASIVO'
) v
JOIN SmsProveedor   p ON p.codigo = v.proveedor
JOIN SmsTipoMensaje t ON t.codigo = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);

-- Verificación
SELECT p.codigo AS proveedor, GROUP_CONCAT(t.codigo ORDER BY t.codigo) AS servicios
FROM SmsProveedorServicio s
JOIN SmsProveedor p ON p.id_sms_proveedor = s.proveedor_id
JOIN SmsTipoMensaje t ON t.id_sms_tipo_mensaje = s.tipo_mensaje_id
WHERE s.eliminado = 0
GROUP BY p.codigo ORDER BY p.codigo;

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V9__plantillas.sql
/* =====================================================================
   V9 - Plantillas SMS con variables (MySQL). Idempotente. Requiere V1 + V6__tipos_mensaje.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   SmsPlantilla.contenido usa variables {{nombre}}; cada una se declara en SmsPlantillaVariable
   con tipo (TEXTO, ENTERO, DECIMAL, MONEDA, FECHA, HORA, URL), obligatoriedad, longitud, patrón
   y valor por defecto. El request de envío trae 'plantilla' + 'variables' y se valida en modo
   estricto (faltantes, desconocidas, tipo, longitud, formato) ANTES de enviar.
   SmsAdmin: /v1/sms/admin/plantillas   Clientes: GET /v1/sms/plantillas/{codigo}, POST .../previsualizar
   ===================================================================== */

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS SmsPlantilla (
  id_sms_plantilla   BIGINT        NOT NULL AUTO_INCREMENT,
  codigo             VARCHAR(50)   NOT NULL,
  nombre             VARCHAR(100)  NOT NULL,
  descripcion        VARCHAR(255)  NULL,
  tipo_mensaje_id    BIGINT        NULL,                 -- tipo por defecto (NULL = lo define el request)
  contenido          VARCHAR(1600) NOT NULL,
  version            INT           NOT NULL DEFAULT 1,  -- sube en cada modificación
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_plantilla PRIMARY KEY (id_sms_plantilla),
  CONSTRAINT uq_sms_plantilla_codigo UNIQUE (codigo),
  CONSTRAINT fk_sms_plantilla_tipo FOREIGN KEY (tipo_mensaje_id) REFERENCES SmsTipoMensaje (id_sms_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsPlantillaVariable (
  id_sms_plantilla_variable BIGINT       NOT NULL AUTO_INCREMENT,
  plantilla_id              BIGINT       NOT NULL,
  nombre                    VARCHAR(50)  NOT NULL,
  tipo_dato                 VARCHAR(20)  NOT NULL DEFAULT 'TEXTO',
  obligatoria               BIT(1)       NOT NULL DEFAULT b'1',
  longitud_max              INT          NULL,
  patron                    VARCHAR(200) NULL,       -- regex sobre el valor completo
  valor_defecto             VARCHAR(200) NULL,
  descripcion               VARCHAR(255) NULL,
  orden                     INT          NOT NULL DEFAULT 0,
  -- Auditoría (AuditInfo): eliminado = 0 => registro activo
  eliminado          TINYINT(1)   NOT NULL DEFAULT 0,
  usr_crea           INT          NOT NULL DEFAULT 1,
  usr_mod            INT          NULL,
  created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NULL,
  CONSTRAINT pk_sms_plantilla_variable PRIMARY KEY (id_sms_plantilla_variable),
  CONSTRAINT uq_sms_plantilla_variable UNIQUE (plantilla_id, nombre),
  CONSTRAINT fk_sms_plantilla_variable_plantilla FOREIGN KEY (plantilla_id) REFERENCES SmsPlantilla (id_sms_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Log: plantilla con la que se generó cada envío
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND column_name = 'plantilla_codigo') = 0,
  'ALTER TABLE SmsEnvio ADD COLUMN plantilla_codigo VARCHAR(50) NULL AFTER mensaje',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

/* ----------------------------- EJEMPLOS ----------------------------- */

INSERT INTO SmsPlantilla (codigo, nombre, descripcion, tipo_mensaje_id, contenido, version, eliminado, usr_crea, created_at)
SELECT v.codigo, v.nombre, v.descripcion, t.id_sms_tipo_mensaje, v.contenido, 1, 0, 1, NOW(6)
FROM (
  SELECT 'RECORDATORIO_CITA' AS codigo, 'Recordatorio de cita' AS nombre, 'Ejemplo: recordatorio con fecha, hora y sede' AS descripcion,
         'NORMAL' AS tipo, 'Hola {{nombre}}, le recordamos su cita el {{fecha}} a las {{hora}} en {{sede}}. Comfenalco Quindio' AS contenido
  UNION ALL
  SELECT 'OTP_VERIFICACION', 'Código de verificación', 'Ejemplo: OTP con vigencia',
         'OTP', 'Comfenalco: su codigo de verificacion es {{codigo}}. Vence en {{minutos}} minutos. No lo comparta.'
) v
LEFT JOIN SmsTipoMensaje t ON t.codigo = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM SmsPlantilla p WHERE p.codigo = v.codigo);

INSERT INTO SmsPlantillaVariable (plantilla_id, nombre, tipo_dato, obligatoria, longitud_max, patron, valor_defecto, descripcion, orden, eliminado, usr_crea, created_at)
SELECT p.id_sms_plantilla, v.nombre, v.tipo_dato, v.obligatoria, v.longitud_max, v.patron, v.valor_defecto, v.descripcion, v.orden, 0, 1, NOW(6)
FROM (
  SELECT 'RECORDATORIO_CITA' AS plantilla, 'nombre' AS nombre, 'TEXTO' AS tipo_dato, b'1' AS obligatoria, 30 AS longitud_max,
         NULL AS patron, NULL AS valor_defecto, 'Primer nombre del afiliado' AS descripcion, 0 AS orden
  UNION ALL SELECT 'RECORDATORIO_CITA', 'fecha', 'FECHA', b'1', NULL, NULL, NULL, 'yyyy-MM-dd o dd/MM/yyyy', 1
  UNION ALL SELECT 'RECORDATORIO_CITA', 'hora',  'HORA',  b'1', NULL, NULL, NULL, 'HH:mm (24 h)', 2
  UNION ALL SELECT 'RECORDATORIO_CITA', 'sede',  'TEXTO', b'0', 40,   NULL, 'Sede principal', 'Sede de atención', 3
  UNION ALL SELECT 'OTP_VERIFICACION',  'codigo',  'TEXTO',  b'1', 8,  '[0-9]{4,8}', NULL, 'PIN de 4 a 8 dígitos', 0
  UNION ALL SELECT 'OTP_VERIFICACION',  'minutos', 'ENTERO', b'0', 3,  NULL, '5', 'Vigencia en minutos', 1
) v
JOIN SmsPlantilla p ON p.codigo = v.plantilla
WHERE NOT EXISTS (SELECT 1 FROM SmsPlantillaVariable x WHERE x.plantilla_id = p.id_sms_plantilla AND x.nombre = v.nombre);

-- Verificación
SELECT p.codigo, p.version, t.codigo AS tipo, COUNT(v.id_sms_plantilla_variable) AS variables, p.contenido
FROM SmsPlantilla p
LEFT JOIN SmsTipoMensaje t ON t.id_sms_tipo_mensaje = p.tipo_mensaje_id
LEFT JOIN SmsPlantillaVariable v ON v.plantilla_id = p.id_sms_plantilla
GROUP BY p.id_sms_plantilla, p.codigo, p.version, t.codigo, p.contenido ORDER BY p.codigo;

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V10__inalambria_masivo.sql
/* =====================================================================
   V10 - Envío MASIVO nativo de Inalambria (MySQL). Idempotente. Requiere V2 + V8.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   API: POST https://rest.inalambria.com/mtmessage/massive  (docs.inalambria.com,
        "Mensaje SMS Unidireccional (Batch - En Lotes)"), autenticación Basic con la cuenta.
   El motor masivo (POST /v1/sms/batch y /v1/sms/inalambria/batch) usa esta API para los pasos
   de cuentas INALAMBRIA: lotes de sms.inalambria.massive.chunk-size (500), 3 intentos por lote
   y, si se agotan, el lote completo pasa a la siguiente cuenta de la ruta (mismo tipo de mensaje).

   Parámetros opcionales de SmsCuenta.parametros_json para el masivo:
     massiveUrl        URL del endpoint (defecto: la de application.yaml)
     massiveChunkSize  destinatarios por petición para esta cuenta
     xAuth             valor del header x-auth (la doc lo marca como requerido sin explicarlo:
                       confirmar con Inalambria; si no se configura no se envía)
     notifyUrl         URL de callback de estados (notifyContentType por defecto application/json)
   ===================================================================== */

-- 1. URL masiva explícita en la cuenta masiva (no pisa un valor ya configurado)
UPDATE SmsCuenta
SET parametros_json = JSON_SET(IF(parametros_json IS NULL OR JSON_VALID(parametros_json) = 0, '{}', parametros_json),
                               '$.massiveUrl', 'https://rest.inalambria.com/mtmessage/massive')
WHERE codigo = 'INALAMBRIA_MASIVO'
  AND (parametros_json IS NULL OR JSON_VALID(parametros_json) = 0
       OR JSON_EXTRACT(parametros_json, '$.massiveUrl') IS NULL);

-- 2. Rutas MASIVO (ya las crean V2/V5; se garantizan aquí por si se borraron)
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'MASIVO' AS tipo, 1 AS orden, 'INALAMBRIA_MASIVO' AS cuenta
  UNION ALL SELECT NULL, 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 1000, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'MASIVO' AS tipo, 1 AS orden, 'INALAMBRIA_MASIVO' AS cuenta
  UNION ALL SELECT NULL, 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta);


-- Verificación
SELECT codigo, JSON_UNQUOTE(JSON_EXTRACT(parametros_json, '$.massiveUrl')) AS massive_url, parametros_json
FROM SmsCuenta WHERE codigo = 'INALAMBRIA_MASIVO';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V11__inalambria_whatsapp.sql
/* =====================================================================
   V11 - WhatsApp de Inalambria (plantillas de Meta) (MySQL). Idempotente. Requiere V1, V2, V6__tipos_mensaje, V8.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   API: POST https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp (Basic)
        body {source, destination, templateId, parameters[], optionalAditionalData{}}
   Endpoints: POST /v1/whatsapp/send (central) y POST /v1/whatsapp/inalambria/send

   Crea:
     - Tipo de mensaje WHATSAPP (canal WHATSAPP).
     - Cuenta INALAMBRIA_WHATSAPP. CONFIGURAR ANTES DE USAR:
         usuario  -> 'env:INALAMBRIA_WHATSAPP_USER'  (o el usuario literal)
         clave    -> variable de entorno INALAMBRIA_WHATSAPP_PASSWORD (secret_ref), o mejor
                     PUT /v1/sms/admin/cuentas/{id} con "clave" (se guarda cifrada)
         remitente-> número de origen autorizado por Inalambria/Meta (ej. 573001234567)
     - parametros_json.canal = WHATSAPP: la cuenta solo envía WhatsApp y las cuentas SMS no envían WhatsApp.
     - Servicio WHATSAPP para INALAMBRIA y MOCK (pruebas). GO4CLIENTS no lo tiene.
     - Rutas: INALAMBRIA/WHATSAPP, central/WHATSAPP -> INALAMBRIA_WHATSAPP; MOCK/WHATSAPP -> MOCK_OK.
   Las cuentas SMS de Inalambria NO tienen el canal WhatsApp: no se agregan a rutas WHATSAPP.
   ===================================================================== */

SET NAMES utf8mb4;

-- 1. Tipo de mensaje
INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, eliminado, usr_crea)
SELECT 'WHATSAPP', 'Mensaje de WhatsApp con plantilla aprobada por Meta', 'WHATSAPP', b'0', b'1', 0, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje WHERE codigo = 'WHATSAPP');

-- 2. Cuenta WhatsApp de Inalambria

INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'INALAMBRIA'), 'INALAMBRIA_WHATSAPP', 'env:INALAMBRIA_WHATSAPP_USER', 'INALAMBRIA_WHATSAPP_PASSWORD', NULL, NULL,
       '{"canal":"WHATSAPP","whatsappUrl":"https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp"}', 0, 1
FROM DUAL WHERE EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'INALAMBRIA') AND NOT EXISTS (SELECT 1 FROM SmsCuenta WHERE codigo = 'INALAMBRIA_WHATSAPP');

-- 3. Servicio WHATSAPP
INSERT INTO SmsProveedorServicio (proveedor_id, tipo_mensaje_id, eliminado, usr_crea, created_at)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 0, 1, NOW(6)
FROM (SELECT 'INALAMBRIA' AS proveedor UNION ALL SELECT 'MOCK') v
JOIN SmsProveedor   p ON p.codigo = v.proveedor
JOIN SmsTipoMensaje t ON t.codigo = 'WHATSAPP'
WHERE NOT EXISTS (SELECT 1 FROM SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);

-- 4. Rutas
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'WHATSAPP' AS tipo, 1 AS orden, 'INALAMBRIA_WHATSAPP' AS cuenta
  UNION ALL SELECT NULL, 'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP'
  UNION ALL SELECT 'MOCK', 'WHATSAPP', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 1000, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'WHATSAPP' AS tipo, 1 AS orden, 'INALAMBRIA_WHATSAPP' AS cuenta
  UNION ALL SELECT NULL, 'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP'
  UNION ALL SELECT 'MOCK', 'WHATSAPP', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


-- 5. (Opcional) número de origen. Descomente y ajuste:
-- UPDATE SmsCuenta SET remitente = '573001234567' WHERE codigo = 'INALAMBRIA_WHATSAPP';

-- Verificación
SELECT c.codigo, c.usuario, c.secret_ref, c.clave_cifrada IS NOT NULL AS tiene_clave_cifrada, c.remitente AS source, c.parametros_json
FROM SmsCuenta c WHERE c.codigo = 'INALAMBRIA_WHATSAPP';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V12__inalambria_bidireccional.sql
/* =====================================================================
   V12 - SMS bidireccional de Inalambria (MySQL). Idempotente. Requiere V1, V2, V6__tipos_mensaje, V8.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   API: POST https://rest.inalambria.com/momessage (Basic) body {MessageText, PhoneNumber, ShortCode, Url?}
        respuesta {Id (28 dígitos), Message OK|ERROR, Status true|false}
   Envío: POST /v1/sms/send o /v1/sms/inalambria/send con "tipo_mensaje": "BIDIRECCIONAL".

   Crea:
     - Tipo de mensaje BIDIRECCIONAL (canal SMS).
     - Cuenta INALAMBRIA_BIDIRECCIONAL. CONFIGURAR ANTES DE USAR:
         usuario   -> 'env:INALAMBRIA_BIDIRECCIONAL_USER' (o el usuario literal)
         clave     -> variable INALAMBRIA_BIDIRECCIONAL_PASSWORD (secret_ref) o PUT /v1/sms/admin/cuentas/{id} con "clave"
         remitente -> código corto asignado por Inalambria (ej. 12345); también parametros_json.shortCode
     - Servicio BIDIRECCIONAL para INALAMBRIA y MOCK (pruebas). GO4CLIENTS no lo tiene.
     - Rutas: INALAMBRIA/BIDIRECCIONAL y central/BIDIRECCIONAL -> INALAMBRIA_BIDIRECCIONAL; MOCK/BIDIRECCIONAL -> MOCK_OK.
   Las respuestas del usuario (MO) no las documenta este endpoint: pendiente (webhook/callback de Inalambria).
   ===================================================================== */

SET NAMES utf8mb4;

INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, eliminado, usr_crea)
SELECT 'BIDIRECCIONAL', 'SMS bidireccional desde código corto (el destinatario puede responder)', 'SMS', b'0', b'0', 0, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje WHERE codigo = 'BIDIRECCIONAL');


INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'INALAMBRIA'), 'INALAMBRIA_BIDIRECCIONAL', 'env:INALAMBRIA_BIDIRECCIONAL_USER', 'INALAMBRIA_BIDIRECCIONAL_PASSWORD', NULL, NULL,
       '{"bidireccionalUrl":"https://rest.inalambria.com/momessage"}', 0, 1
FROM DUAL WHERE EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'INALAMBRIA') AND NOT EXISTS (SELECT 1 FROM SmsCuenta WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL');

INSERT INTO SmsProveedorServicio (proveedor_id, tipo_mensaje_id, eliminado, usr_crea, created_at)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 0, 1, NOW(6)
FROM (SELECT 'INALAMBRIA' AS proveedor UNION ALL SELECT 'MOCK') v
JOIN SmsProveedor   p ON p.codigo = v.proveedor
JOIN SmsTipoMensaje t ON t.codigo = 'BIDIRECCIONAL'
WHERE NOT EXISTS (SELECT 1 FROM SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);

-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'BIDIRECCIONAL' AS tipo, 1 AS orden, 'INALAMBRIA_BIDIRECCIONAL' AS cuenta
  UNION ALL SELECT NULL, 'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL'
  UNION ALL SELECT 'MOCK', 'BIDIRECCIONAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 1000, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'BIDIRECCIONAL' AS tipo, 1 AS orden, 'INALAMBRIA_BIDIRECCIONAL' AS cuenta
  UNION ALL SELECT NULL, 'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL'
  UNION ALL SELECT 'MOCK', 'BIDIRECCIONAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


-- Código corto (descomente y ajuste):
-- UPDATE SmsCuenta SET remitente = '12345' WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL';

SELECT codigo, usuario, secret_ref, remitente AS short_code, parametros_json FROM SmsCuenta WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V13__auditoria_soft_delete.sql
-- =============================================================================
-- V13 · Auditoría + soft delete (AuditInfo) en las tablas de configuración
--       SmsProveedor, SmsCuenta, SmsTipoMensaje, SmsRuta, SmsRutaPaso, SmsProveedorServicio
-- -----------------------------------------------------------------------------
-- AuditInfo mapea `eliminado` con @SoftDelete e insertable=false: Hibernate NUNCA
-- escribe la columna y filtra con `eliminado = 0`. Si queda NULL (columna creada
-- por ddl-auto=update sin DEFAULT), las filas quedan invisibles (404 / RUTA_NO_CONFIGURADA).
--
-- Deja en cada tabla:
--   eliminado  TINYINT(1)  NOT NULL DEFAULT 0     (NULL -> 0)
--   usr_crea   INT         NOT NULL DEFAULT 1     (NULL/0 -> 1)
--   usr_mod    INT         NULL
--   created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)  (backfill desde fecha_creacion)
--   updated_at DATETIME(6) NULL
-- La aplicación ya no usa `activo` ni `fecha_creacion`; si existen en la base se ignoran.
-- EJECUTAR en una base existente ANTES de los scripts de datos (V2-V5, V8-V12) y otra vez al final.
-- Idempotente. Compatible con MySQL 5.7 y 8.0.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_v13_columna;
DELIMITER $$
-- Ejecuta p_sql solo si la tabla existe y la columna (no) existe según p_debe_existir
CREATE PROCEDURE sp_v13_columna(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64),
                                IN p_debe_existir TINYINT, IN p_sql TEXT)
BEGIN
  DECLARE v_tabla INT DEFAULT 0;
  DECLARE v_col INT DEFAULT 0;
  SELECT COUNT(*) INTO v_tabla FROM information_schema.tables
   WHERE table_schema = DATABASE() AND table_name = p_tabla;
  SELECT COUNT(*) INTO v_col FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_tabla AND column_name = p_columna;
  IF v_tabla > 0 AND ((p_debe_existir = 1 AND v_col > 0) OR (p_debe_existir = 0 AND v_col = 0)) THEN
    SET @s = p_sql;
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$

DROP PROCEDURE IF EXISTS sp_v13_auditoria$$
CREATE PROCEDURE sp_v13_auditoria(IN p_tabla VARCHAR(64))
BEGIN
  -- eliminado
  CALL sp_v13_columna(p_tabla, 'eliminado', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  CALL sp_v13_columna(p_tabla, 'eliminado', 1,
       CONCAT('UPDATE `', p_tabla, '` SET eliminado = 0 WHERE eliminado IS NULL'));
  CALL sp_v13_columna(p_tabla, 'eliminado', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  -- usr_crea / usr_mod
  CALL sp_v13_columna(p_tabla, 'usr_crea', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v13_columna(p_tabla, 'usr_crea', 1,
       CONCAT('UPDATE `', p_tabla, '` SET usr_crea = 1 WHERE usr_crea IS NULL OR usr_crea = 0'));
  CALL sp_v13_columna(p_tabla, 'usr_crea', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v13_columna(p_tabla, 'usr_mod', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_mod INT NULL'));
  -- created_at / updated_at
  CALL sp_v13_columna(p_tabla, 'created_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v13_columna(p_tabla, 'created_at', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v13_columna(p_tabla, 'updated_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN updated_at DATETIME(6) NULL'));
  CALL sp_v13_columna(p_tabla, 'fecha_creacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET created_at = fecha_creacion WHERE created_at IS NULL'));
  CALL sp_v13_columna(p_tabla, 'fecha_modificacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET updated_at = fecha_modificacion WHERE updated_at IS NULL'));
END$$
DELIMITER ;

CALL sp_v13_auditoria('SmsProveedor');
CALL sp_v13_auditoria('SmsCuenta');
CALL sp_v13_auditoria('SmsTipoMensaje');
CALL sp_v13_auditoria('SmsRuta');
CALL sp_v13_auditoria('SmsRutaPaso');
CALL sp_v13_auditoria('SmsProveedorServicio');

DROP PROCEDURE IF EXISTS sp_v13_auditoria;
DROP PROCEDURE IF EXISTS sp_v13_columna;

-- Verificación: no debe quedar ningún eliminado NULL
SELECT 'SmsProveedor' AS tabla, SUM(eliminado IS NULL) AS nulos, COUNT(*) AS filas FROM SmsProveedor
UNION ALL SELECT 'SmsCuenta',      SUM(eliminado IS NULL), COUNT(*) FROM SmsCuenta
UNION ALL SELECT 'SmsTipoMensaje', SUM(eliminado IS NULL), COUNT(*) FROM SmsTipoMensaje
UNION ALL SELECT 'SmsRuta',        SUM(eliminado IS NULL), COUNT(*) FROM SmsRuta
UNION ALL SELECT 'SmsRutaPaso',    SUM(eliminado IS NULL), COUNT(*) FROM SmsRutaPaso;

DELIMITER ;

-- >>>>>>>>>>>>>>>>>>>> V14__auditoria_envios_plantillas.sql
-- =============================================================================
-- V14 · AuditInfo en SmsEnvio, SmsEnvioIntento, SmsPlantilla y SmsPlantillaVariable
-- -----------------------------------------------------------------------------
-- Estas entidades ahora extienden AuditInfo (@SoftDelete en `eliminado`,
-- usr_crea/usr_mod, created_at/updated_at). Requiere V13.
--
--   1. eliminado TINYINT(1) NOT NULL DEFAULT 0 y usr_crea INT NOT NULL DEFAULT 1
--      (si `eliminado` queda NULL, Hibernate no vuelve a encontrar el envío recién
--      creado y no registra intentos ni el resultado final).
--   2. created_at / updated_at si no existen, y backfill desde fecha_creacion /
--      fecha_modificacion.
--   3. SmsEnvio.fecha_creacion con DEFAULT CURRENT_TIMESTAMP(6): la aplicación ya
--      no la escribe (usa created_at) y sin default el INSERT falla.
--   4. Índices de consulta de envíos sobre created_at.
-- Idempotente. Compatible con MySQL 5.7 y 8.0.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_v14_columna;
DELIMITER $$
-- Ejecuta p_sql solo si la tabla existe y la columna (no) existe según p_debe_existir
CREATE PROCEDURE sp_v14_columna(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64),
                                IN p_debe_existir TINYINT, IN p_sql TEXT)
BEGIN
  DECLARE v_tabla INT DEFAULT 0;
  DECLARE v_col INT DEFAULT 0;
  SELECT COUNT(*) INTO v_tabla FROM information_schema.tables
   WHERE table_schema = DATABASE() AND table_name = p_tabla;
  SELECT COUNT(*) INTO v_col FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_tabla AND column_name = p_columna;
  IF v_tabla > 0 AND ((p_debe_existir = 1 AND v_col > 0) OR (p_debe_existir = 0 AND v_col = 0)) THEN
    SET @s = p_sql;
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$

DROP PROCEDURE IF EXISTS sp_v14_auditoria$$
CREATE PROCEDURE sp_v14_auditoria(IN p_tabla VARCHAR(64))
BEGIN
  -- eliminado
  CALL sp_v14_columna(p_tabla, 'eliminado', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  CALL sp_v14_columna(p_tabla, 'eliminado', 1,
       CONCAT('UPDATE `', p_tabla, '` SET eliminado = 0 WHERE eliminado IS NULL'));
  CALL sp_v14_columna(p_tabla, 'eliminado', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  -- usr_crea / usr_mod
  CALL sp_v14_columna(p_tabla, 'usr_crea', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v14_columna(p_tabla, 'usr_crea', 1,
       CONCAT('UPDATE `', p_tabla, '` SET usr_crea = 1 WHERE usr_crea IS NULL OR usr_crea = 0'));
  CALL sp_v14_columna(p_tabla, 'usr_crea', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v14_columna(p_tabla, 'usr_mod', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_mod INT NULL'));
  -- created_at / updated_at
  CALL sp_v14_columna(p_tabla, 'created_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v14_columna(p_tabla, 'created_at', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v14_columna(p_tabla, 'updated_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN updated_at DATETIME(6) NULL'));
  CALL sp_v14_columna(p_tabla, 'fecha_creacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET created_at = fecha_creacion WHERE created_at IS NULL'));
  CALL sp_v14_columna(p_tabla, 'fecha_modificacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET updated_at = fecha_modificacion WHERE updated_at IS NULL'));
END$$
DELIMITER ;

CALL sp_v14_auditoria('SmsEnvio');
CALL sp_v14_auditoria('SmsEnvioIntento');
CALL sp_v14_auditoria('SmsPlantilla');
CALL sp_v14_auditoria('SmsPlantillaVariable');

-- SmsEnvioIntento: created_at desde su columna `fecha`
CALL sp_v14_columna('SmsEnvioIntento', 'fecha', 1,
     'UPDATE SmsEnvioIntento SET created_at = fecha WHERE created_at IS NULL');

-- SmsEnvio.fecha_creacion ya no la escribe la aplicación
CALL sp_v14_columna('SmsEnvio', 'fecha_creacion', 1,
     'ALTER TABLE SmsEnvio MODIFY COLUMN fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');

DROP PROCEDURE IF EXISTS sp_v14_auditoria;
DROP PROCEDURE IF EXISTS sp_v14_columna;

-- Índices de consulta (/v1/sms/envios) sobre created_at
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'SmsEnvio' AND index_name = 'ix_sms_envio_destino_created') = 0,
  'CREATE INDEX ix_sms_envio_destino_created ON SmsEnvio (destino, created_at)', 'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'SmsEnvio' AND index_name = 'ix_sms_envio_estado_created') = 0,
  'CREATE INDEX ix_sms_envio_estado_created ON SmsEnvio (estado, created_at)', 'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación (solo tablas existentes): eliminado debe quedar NOT NULL DEFAULT 0
SELECT table_name AS tabla, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND column_name = 'eliminado'
  AND table_name IN ('SmsEnvio', 'SmsEnvioIntento', 'SmsPlantilla', 'SmsPlantillaVariable');

DELIMITER ;
