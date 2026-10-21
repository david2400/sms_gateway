/* =====================================================================
   V1 - SmsCore: ESQUEMA (SQL Server, BD MAESTRA)
   ---------------------------------------------------------------------
   Crea las 6 tablas del orquestador SMS + índices + procedimiento helper
   para armar rutas. Idempotente: se puede ejecutar varias veces.

   Debe coincidir con las entidades JPA:
     SmsCore/infrastructure/outbound/persistence/entity/Sms*Entity.java
     SmsAdmin/infrastructure/outbound/persistence/entity/Admin*Entity.java

   Configuración          Log de envíos
   ------------------     -----------------
   SmsProveedor           SmsEnvio
   SmsCuenta              SmsEnvioIntento
   SmsRuta
   SmsRutaPaso
   ===================================================================== */

/* ---------------------------- CONFIGURACIÓN ---------------------------- */

IF OBJECT_ID('dbo.SmsProveedor', 'U') IS NULL
CREATE TABLE dbo.SmsProveedor (
  id_sms_proveedor   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_proveedor PRIMARY KEY,
  codigo             VARCHAR(30)  NOT NULL CONSTRAINT uq_sms_proveedor_codigo UNIQUE,   -- INALAMBRIA, GO4CLIENTS, MOCK (= SmsSenderPort.providerCode())
  nombre             VARCHAR(100) NOT NULL,
  base_url           VARCHAR(255) NOT NULL,
  connect_timeout_ms INT       NOT NULL CONSTRAINT df_sms_proveedor_connect DEFAULT 2000,
  read_timeout_ms    INT       NOT NULL CONSTRAINT df_sms_proveedor_read    DEFAULT 5000,
  activo             BIT       NOT NULL CONSTRAINT df_sms_proveedor_activo  DEFAULT 1,
  fecha_creacion     DATETIME2 NOT NULL CONSTRAINT df_sms_proveedor_fecha   DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('dbo.SmsCuenta', 'U') IS NULL
CREATE TABLE dbo.SmsCuenta (
  id_sms_cuenta   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_cuenta PRIMARY KEY,
  proveedor_id    BIGINT        NOT NULL CONSTRAINT fk_sms_cuenta_proveedor REFERENCES dbo.SmsProveedor(id_sms_proveedor),
  codigo          VARCHAR(50)   NOT NULL CONSTRAINT uq_sms_cuenta_codigo UNIQUE,
  usuario         VARCHAR(100)  NULL,          -- valor literal, o 'env:NOMBRE_VARIABLE'
  secret_ref      VARCHAR(100)  NOT NULL,      -- NOMBRE de la variable de entorno con la contraseña (nunca la contraseña)
  remitente       VARCHAR(20)   NULL,
  parametros_json NVARCHAR(MAX) NULL,          -- parámetros propios de cada proveedor
  activo          BIT           NOT NULL CONSTRAINT df_sms_cuenta_activo DEFAULT 1
);
GO

IF OBJECT_ID('dbo.SmsRuta', 'U') IS NULL
CREATE TABLE dbo.SmsRuta (
  id_sms_ruta  BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_ruta PRIMARY KEY,
  proveedor_id BIGINT      NULL CONSTRAINT fk_sms_ruta_proveedor REFERENCES dbo.SmsProveedor(id_sms_proveedor), -- NULL = endpoint CENTRAL
  tipo_mensaje VARCHAR(30) NOT NULL,           -- OTP, HABEAS_DATA, TEMPLATE, PERSONALIZED, NORMAL, MASIVO
  activo       BIT         NOT NULL CONSTRAINT df_sms_ruta_activo DEFAULT 1
);
GO

IF OBJECT_ID('dbo.SmsRutaPaso', 'U') IS NULL
CREATE TABLE dbo.SmsRutaPaso (
  id_sms_ruta_paso BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_ruta_paso PRIMARY KEY,
  ruta_id          BIGINT NOT NULL CONSTRAINT fk_sms_ruta_paso_ruta   REFERENCES dbo.SmsRuta(id_sms_ruta),
  orden            INT    NOT NULL,            -- 1, 2, 3... orden de fallback
  cuenta_id        BIGINT NOT NULL CONSTRAINT fk_sms_ruta_paso_cuenta REFERENCES dbo.SmsCuenta(id_sms_cuenta),
  max_intentos     INT    NOT NULL CONSTRAINT df_sms_ruta_paso_intentos DEFAULT 3,
  espera_ms        INT    NOT NULL CONSTRAINT df_sms_ruta_paso_espera   DEFAULT 500,
  activo           BIT    NOT NULL CONSTRAINT df_sms_ruta_paso_activo   DEFAULT 1,
  CONSTRAINT uq_sms_ruta_paso UNIQUE (ruta_id, orden)
);
GO

/* ------------------------------ LOG DE ENVÍOS ------------------------------ */

IF OBJECT_ID('dbo.SmsEnvio', 'U') IS NULL
CREATE TABLE dbo.SmsEnvio (
  id_sms_envio         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_envio PRIMARY KEY,
  uuid                 UNIQUEIDENTIFIER NOT NULL CONSTRAINT uq_sms_envio_uuid UNIQUE,  -- id público (envio_id en la API)
  idempotency_key      VARCHAR(100)   NULL,
  canal                VARCHAR(20)    NOT NULL,   -- CENTRAL | PROVEEDOR
  proveedor_solicitado VARCHAR(30)    NULL,       -- solo canal PROVEEDOR
  tipo_mensaje         VARCHAR(30)    NOT NULL,
  destino              VARCHAR(20)    NOT NULL,
  mensaje              NVARCHAR(1600) NOT NULL,
  estado               VARCHAR(20)    NOT NULL,   -- EN_PROCESO | ENVIADO | FALLIDO
  proveedor_final_id   BIGINT NULL CONSTRAINT fk_sms_envio_proveedor REFERENCES dbo.SmsProveedor(id_sms_proveedor),
  cuenta_final_id      BIGINT NULL CONSTRAINT fk_sms_envio_cuenta    REFERENCES dbo.SmsCuenta(id_sms_cuenta),
  http_status_final    INT            NULL,
  total_intentos       INT            NOT NULL CONSTRAINT df_sms_envio_intentos DEFAULT 0,
  codigo_error         VARCHAR(50)    NULL,
  detalle_error        NVARCHAR(1000) NULL,
  fecha_creacion       DATETIME2      NOT NULL,
  fecha_finalizacion   DATETIME2      NULL
);
GO

IF OBJECT_ID('dbo.SmsEnvioIntento', 'U') IS NULL
CREATE TABLE dbo.SmsEnvioIntento (
  id_sms_envio_intento BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_envio_intento PRIMARY KEY,
  envio_id     BIGINT NOT NULL CONSTRAINT fk_sms_intento_envio     REFERENCES dbo.SmsEnvio(id_sms_envio),
  paso_orden   INT    NOT NULL,
  nro_intento  INT    NOT NULL,
  proveedor_id BIGINT NOT NULL CONSTRAINT fk_sms_intento_proveedor REFERENCES dbo.SmsProveedor(id_sms_proveedor),
  cuenta_id    BIGINT NOT NULL CONSTRAINT fk_sms_intento_cuenta    REFERENCES dbo.SmsCuenta(id_sms_cuenta),
  exitoso      BIT    NOT NULL,
  http_status  INT    NULL,
  detalle      NVARCHAR(1000) NULL,
  duracion_ms  INT       NOT NULL,
  fecha        DATETIME2 NOT NULL
);
GO

/* -------------------------------- ÍNDICES --------------------------------
   Separados de los CREATE TABLE para que también se creen si Hibernate
   (ddl-auto=update) creó las tablas antes que este script.               */

-- Una sola ruta por (proveedor, tipo). En SQL Server el NULL cuenta como valor: una sola ruta central por tipo.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_sms_ruta' AND object_id = OBJECT_ID('dbo.SmsRuta'))
  CREATE UNIQUE INDEX ux_sms_ruta ON dbo.SmsRuta (proveedor_id, tipo_mensaje);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_sms_envio_idem' AND object_id = OBJECT_ID('dbo.SmsEnvio'))
  CREATE UNIQUE INDEX ux_sms_envio_idem ON dbo.SmsEnvio (idempotency_key) WHERE idempotency_key IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_sms_envio_destino' AND object_id = OBJECT_ID('dbo.SmsEnvio'))
  CREATE INDEX ix_sms_envio_destino ON dbo.SmsEnvio (destino, fecha_creacion);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_sms_envio_estado' AND object_id = OBJECT_ID('dbo.SmsEnvio'))
  CREATE INDEX ix_sms_envio_estado ON dbo.SmsEnvio (estado, fecha_creacion);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_sms_intento_envio' AND object_id = OBJECT_ID('dbo.SmsEnvioIntento'))
  CREATE INDEX ix_sms_intento_envio ON dbo.SmsEnvioIntento (envio_id);
GO

/* ------------------------- HELPER PARA LOS SEEDS -------------------------
   dbo.SmsRuta_AgregarPaso: crea la ruta (si no existe) y le agrega un paso
   (si ese orden no existe). No modifica pasos existentes.
     @proveedor NULL = ruta del endpoint CENTRAL.
   Ej: EXEC dbo.SmsRuta_AgregarPaso NULL, 'OTP', 1, 'INALAMBRIA_OTP';      */
GO
CREATE OR ALTER PROCEDURE dbo.SmsRuta_AgregarPaso
  @proveedor    VARCHAR(30),
  @tipo         VARCHAR(30),
  @orden        INT,
  @cuenta       VARCHAR(50),
  @max_intentos INT = 3,
  @espera_ms    INT = 500
AS
BEGIN
  SET NOCOUNT ON;
  DECLARE @prov_id BIGINT = NULL, @ruta_id BIGINT, @cuenta_id BIGINT;

  IF @proveedor IS NOT NULL
  BEGIN
    SELECT @prov_id = id_sms_proveedor FROM dbo.SmsProveedor WHERE codigo = @proveedor;
    IF @prov_id IS NULL BEGIN RAISERROR('Proveedor %s no existe', 16, 1, @proveedor); RETURN; END
  END

  SELECT @cuenta_id = id_sms_cuenta FROM dbo.SmsCuenta WHERE codigo = @cuenta;
  IF @cuenta_id IS NULL BEGIN RAISERROR('Cuenta %s no existe', 16, 1, @cuenta); RETURN; END

  SELECT @ruta_id = id_sms_ruta FROM dbo.SmsRuta
   WHERE tipo_mensaje = @tipo
     AND ((@prov_id IS NULL AND proveedor_id IS NULL) OR proveedor_id = @prov_id);

  IF @ruta_id IS NULL
  BEGIN
    INSERT INTO dbo.SmsRuta (proveedor_id, tipo_mensaje, activo) VALUES (@prov_id, @tipo, 1);
    SET @ruta_id = SCOPE_IDENTITY();
  END

  IF NOT EXISTS (SELECT 1 FROM dbo.SmsRutaPaso WHERE ruta_id = @ruta_id AND orden = @orden)
    INSERT INTO dbo.SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, activo)
    VALUES (@ruta_id, @orden, @cuenta_id, @max_intentos, @espera_ms, 1);
END
GO
