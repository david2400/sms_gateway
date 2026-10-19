/* =====================================================================
   SmsCore - orquestación SMS multi-proveedor (SQL Server, BD MAESTRA)
   Idempotente: se puede ejecutar varias veces.
   Las contraseñas NO van aquí: secret_ref es el NOMBRE de la variable de
   entorno que la contiene (ej. INALAMBRIA_OTP_PASSWORD).
   ===================================================================== */

IF OBJECT_ID('dbo.sms_proveedor') IS NULL
CREATE TABLE dbo.sms_proveedor (
  id                 BIGINT IDENTITY PRIMARY KEY,
  codigo             VARCHAR(30)  NOT NULL CONSTRAINT uq_sms_proveedor_codigo UNIQUE,
  nombre             VARCHAR(100) NOT NULL,
  base_url           VARCHAR(255) NOT NULL,
  connect_timeout_ms INT NOT NULL DEFAULT 2000,
  read_timeout_ms    INT NOT NULL DEFAULT 5000,
  activo             BIT NOT NULL DEFAULT 1,
  fecha_creacion     DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('dbo.sms_cuenta') IS NULL
CREATE TABLE dbo.sms_cuenta (
  id              BIGINT IDENTITY PRIMARY KEY,
  proveedor_id    BIGINT NOT NULL REFERENCES dbo.sms_proveedor(id),
  codigo          VARCHAR(50) NOT NULL CONSTRAINT uq_sms_cuenta_codigo UNIQUE,
  usuario         VARCHAR(100) NULL,
  secret_ref      VARCHAR(100) NOT NULL,
  remitente       VARCHAR(20)  NULL,
  parametros_json NVARCHAR(MAX) NULL,
  activo          BIT NOT NULL DEFAULT 1
);
GO

IF OBJECT_ID('dbo.sms_ruta') IS NULL
CREATE TABLE dbo.sms_ruta (
  id           BIGINT IDENTITY PRIMARY KEY,
  proveedor_id BIGINT NULL REFERENCES dbo.sms_proveedor(id),   -- NULL = ruta del endpoint central
  tipo_mensaje VARCHAR(30) NOT NULL,                             -- OTP, HABEAS_DATA, TEMPLATE, PERSONALIZED, NORMAL, MASIVO
  activo       BIT NOT NULL DEFAULT 1
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_sms_ruta')
CREATE UNIQUE INDEX ux_sms_ruta ON dbo.sms_ruta(proveedor_id, tipo_mensaje);
GO

IF OBJECT_ID('dbo.sms_ruta_paso') IS NULL
CREATE TABLE dbo.sms_ruta_paso (
  id           BIGINT IDENTITY PRIMARY KEY,
  ruta_id      BIGINT NOT NULL REFERENCES dbo.sms_ruta(id),
  orden        INT NOT NULL,
  cuenta_id    BIGINT NOT NULL REFERENCES dbo.sms_cuenta(id),
  max_intentos INT NOT NULL DEFAULT 3,
  espera_ms    INT NOT NULL DEFAULT 500,
  activo       BIT NOT NULL DEFAULT 1,
  CONSTRAINT uq_sms_ruta_paso UNIQUE (ruta_id, orden)
);
GO

IF OBJECT_ID('dbo.sms_envio') IS NULL
CREATE TABLE dbo.sms_envio (
  id                   UNIQUEIDENTIFIER PRIMARY KEY,
  idempotency_key      VARCHAR(100) NULL,
  canal                VARCHAR(20)  NOT NULL,                    -- CENTRAL | PROVEEDOR
  proveedor_solicitado VARCHAR(30)  NULL,
  tipo_mensaje         VARCHAR(30)  NOT NULL,
  destino              VARCHAR(20)  NOT NULL,
  mensaje              NVARCHAR(1600) NOT NULL,
  estado               VARCHAR(20)  NOT NULL,                    -- EN_PROCESO | ENVIADO | FALLIDO
  proveedor_final_id   BIGINT NULL REFERENCES dbo.sms_proveedor(id),
  cuenta_final_id      BIGINT NULL REFERENCES dbo.sms_cuenta(id),
  http_status_final    INT NULL,
  total_intentos       INT NOT NULL DEFAULT 0,
  codigo_error         VARCHAR(50) NULL,
  detalle_error        NVARCHAR(1000) NULL,
  fecha_creacion       DATETIME2 NOT NULL,
  fecha_finalizacion   DATETIME2 NULL
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_sms_envio_idem')
CREATE UNIQUE INDEX ux_sms_envio_idem ON dbo.sms_envio(idempotency_key) WHERE idempotency_key IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_sms_envio_destino')
CREATE INDEX ix_sms_envio_destino ON dbo.sms_envio(destino, fecha_creacion);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_sms_envio_estado')
CREATE INDEX ix_sms_envio_estado ON dbo.sms_envio(estado, fecha_creacion);
GO

IF OBJECT_ID('dbo.sms_envio_intento') IS NULL
CREATE TABLE dbo.sms_envio_intento (
  id           BIGINT IDENTITY PRIMARY KEY,
  envio_id     UNIQUEIDENTIFIER NOT NULL REFERENCES dbo.sms_envio(id),
  paso_orden   INT NOT NULL,
  nro_intento  INT NOT NULL,
  proveedor_id BIGINT NOT NULL REFERENCES dbo.sms_proveedor(id),
  cuenta_id    BIGINT NOT NULL REFERENCES dbo.sms_cuenta(id),
  exitoso      BIT NOT NULL,
  http_status  INT NULL,
  detalle      NVARCHAR(1000) NULL,
  duracion_ms  INT NOT NULL,
  fecha        DATETIME2 NOT NULL
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_sms_intento_envio')
CREATE INDEX ix_sms_intento_envio ON dbo.sms_envio_intento(envio_id);
GO

/* ============================ DATOS INICIALES ============================ */

-- Proveedores
IF NOT EXISTS (SELECT 1 FROM dbo.sms_proveedor WHERE codigo = 'INALAMBRIA')
INSERT INTO dbo.sms_proveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo)
VALUES ('INALAMBRIA', 'Inalambria', 'https://restaws.inalambria.com', 3000, 10000, 1);
IF NOT EXISTS (SELECT 1 FROM dbo.sms_proveedor WHERE codigo = 'MOCK')
INSERT INTO dbo.sms_proveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo)
VALUES ('MOCK', 'Mock (pruebas)', 'https://mock.local', 1000, 1000, 1);
GO

-- Cuentas (usuario aquí; contraseña en la variable de entorno indicada en secret_ref)
DECLARE @inal BIGINT = (SELECT id FROM dbo.sms_proveedor WHERE codigo = 'INALAMBRIA');
DECLARE @mock BIGINT = (SELECT id FROM dbo.sms_proveedor WHERE codigo = 'MOCK');

IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'INALAMBRIA_OTP')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, parametros_json)
VALUES (@inal, 'INALAMBRIA_OTP', 'COMFENALCOquindioOTP', 'INALAMBRIA_OTP_PASSWORD', '{"type":1,"messagePattern":"0","templateId":0}');
IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'INALAMBRIA_MASIVO')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, parametros_json)
VALUES (@inal, 'INALAMBRIA_MASIVO', 'COMFENALCOquindioMASIVO', 'INALAMBRIA_MASIVO_PASSWORD', '{"type":1,"messagePattern":"0","templateId":0}');
IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'INALAMBRIA_HABEASDATA')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, parametros_json)
VALUES (@inal, 'INALAMBRIA_HABEASDATA', 'COMFENALCOquindioCOMUNICADOR', 'INALAMBRIA_HABEASDATA_PASSWORD', '{"type":1,"messagePattern":"0","templateId":0}');
IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'INALAMBRIA_TEMPLATE')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, parametros_json)
VALUES (@inal, 'INALAMBRIA_TEMPLATE', 'COMFENALCOquindioOTP', 'INALAMBRIA_TEMPLATE_PASSWORD', '{"type":1,"messagePattern":"1","templateId":0}');
IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'INALAMBRIA_PERSONALIZADA')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, parametros_json)
VALUES (@inal, 'INALAMBRIA_PERSONALIZADA', 'COMFENALCOquindioOTP', 'INALAMBRIA_PERSONALIZADA_PASSWORD', '{"type":1,"messagePattern":"1","templateId":0}');
IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'MOCK_OK')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, parametros_json)
VALUES (@mock, 'MOCK_OK', 'mock', 'SMS_MOCK_PASSWORD', '{"simulateStatus":200}');
IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'MOCK_FALLA')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, parametros_json)
VALUES (@mock, 'MOCK_FALLA', 'mock', 'SMS_MOCK_PASSWORD', '{"simulateStatus":500}');
GO

/* Rutas: (proveedor NULL = central). Helper para crear ruta + pasos sin duplicar. */
IF OBJECT_ID('tempdb..#pasos') IS NOT NULL DROP TABLE #pasos;
CREATE TABLE #pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO #pasos VALUES
 -- Endpoint CENTRAL (puede mezclar proveedores cuando existan más)
 (NULL, 'OTP',          1, 'INALAMBRIA_OTP'),
 (NULL, 'OTP',          2, 'INALAMBRIA_MASIVO'),
 (NULL, 'HABEAS_DATA',  1, 'INALAMBRIA_HABEASDATA'),
 (NULL, 'TEMPLATE',     1, 'INALAMBRIA_TEMPLATE'),
 (NULL, 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'),
 (NULL, 'NORMAL',       1, 'INALAMBRIA_MASIVO'),
 (NULL, 'MASIVO',       1, 'INALAMBRIA_MASIVO'),
 -- Endpoint propio de INALAMBRIA
 ('INALAMBRIA', 'OTP',          1, 'INALAMBRIA_OTP'),
 ('INALAMBRIA', 'OTP',          2, 'INALAMBRIA_MASIVO'),
 ('INALAMBRIA', 'HABEAS_DATA',  1, 'INALAMBRIA_HABEASDATA'),
 ('INALAMBRIA', 'TEMPLATE',     1, 'INALAMBRIA_TEMPLATE'),
 ('INALAMBRIA', 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'),
 ('INALAMBRIA', 'NORMAL',       1, 'INALAMBRIA_MASIVO'),
 ('INALAMBRIA', 'MASIVO',       1, 'INALAMBRIA_MASIVO'),
 -- Endpoint MOCK: la cuenta que falla va primero para probar 3 intentos + fallback
 ('MOCK', 'OTP',    1, 'MOCK_FALLA'),
 ('MOCK', 'OTP',    2, 'MOCK_OK'),
 ('MOCK', 'NORMAL', 1, 'MOCK_OK');

INSERT INTO dbo.sms_ruta (proveedor_id, tipo_mensaje, activo)
SELECT DISTINCT pr.id, p.tipo, 1
FROM #pasos p
LEFT JOIN dbo.sms_proveedor pr ON pr.codigo = p.proveedor
WHERE NOT EXISTS (SELECT 1 FROM dbo.sms_ruta r
                  WHERE r.tipo_mensaje = p.tipo
                    AND ((r.proveedor_id IS NULL AND pr.id IS NULL) OR r.proveedor_id = pr.id));

INSERT INTO dbo.sms_ruta_paso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, activo)
SELECT r.id, p.orden, c.id, 3, 500, 1
FROM #pasos p
LEFT JOIN dbo.sms_proveedor pr ON pr.codigo = p.proveedor
JOIN dbo.sms_ruta r ON r.tipo_mensaje = p.tipo
                   AND ((r.proveedor_id IS NULL AND pr.id IS NULL) OR r.proveedor_id = pr.id)
JOIN dbo.sms_cuenta c ON c.codigo = p.cuenta
WHERE NOT EXISTS (SELECT 1 FROM dbo.sms_ruta_paso x WHERE x.ruta_id = r.id AND x.orden = p.orden);

DROP TABLE #pasos;
GO

/* Consulta útil: cómo se envió cada SMS
SELECT e.fecha_creacion, e.canal, e.tipo_mensaje, e.destino, e.estado, p.codigo AS proveedor,
       c.codigo AS cuenta, e.http_status_final, e.total_intentos, e.detalle_error
FROM dbo.sms_envio e
LEFT JOIN dbo.sms_proveedor p ON p.id = e.proveedor_final_id
LEFT JOIN dbo.sms_cuenta c    ON c.id = e.cuenta_final_id
ORDER BY e.fecha_creacion DESC;
*/
