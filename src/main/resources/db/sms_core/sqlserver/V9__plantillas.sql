/* =====================================================================
   V9 - Plantillas SMS con variables (SQL Server, BD MAESTRA). Idempotente. Requiere V8.
   Equivalente a mysql/V9__plantillas.sql.
   ===================================================================== */

IF OBJECT_ID('dbo.SmsPlantilla', 'U') IS NULL
CREATE TABLE dbo.SmsPlantilla (
  id_sms_plantilla   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_plantilla PRIMARY KEY,
  codigo             VARCHAR(50)    NOT NULL CONSTRAINT uq_sms_plantilla_codigo UNIQUE,
  nombre             NVARCHAR(100)  NOT NULL,
  descripcion        NVARCHAR(255)  NULL,
  tipo_mensaje_id    BIGINT         NULL CONSTRAINT fk_sms_plantilla_tipo REFERENCES dbo.SmsTipoMensaje(id_sms_tipo_mensaje),
  contenido          NVARCHAR(1600) NOT NULL,
  version            INT            NOT NULL CONSTRAINT df_sms_plantilla_version DEFAULT 1,
  activo             BIT            NOT NULL CONSTRAINT df_sms_plantilla_activo  DEFAULT 1,
  fecha_creacion     DATETIME2      NOT NULL CONSTRAINT df_sms_plantilla_fecha   DEFAULT SYSDATETIME(),
  fecha_modificacion DATETIME2      NULL
);
GO

IF OBJECT_ID('dbo.SmsPlantillaVariable', 'U') IS NULL
CREATE TABLE dbo.SmsPlantillaVariable (
  id_sms_plantilla_variable BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_plantilla_variable PRIMARY KEY,
  plantilla_id  BIGINT        NOT NULL CONSTRAINT fk_sms_plantilla_variable_plantilla REFERENCES dbo.SmsPlantilla(id_sms_plantilla),
  nombre        VARCHAR(50)   NOT NULL,
  tipo_dato     VARCHAR(20)   NOT NULL CONSTRAINT df_sms_plantilla_var_tipo  DEFAULT 'TEXTO',
  obligatoria   BIT           NOT NULL CONSTRAINT df_sms_plantilla_var_oblig DEFAULT 1,
  longitud_max  INT           NULL,
  patron        NVARCHAR(200) NULL,
  valor_defecto NVARCHAR(200) NULL,
  descripcion   NVARCHAR(255) NULL,
  orden         INT           NOT NULL CONSTRAINT df_sms_plantilla_var_orden DEFAULT 0,
  CONSTRAINT uq_sms_plantilla_variable UNIQUE (plantilla_id, nombre)
);
GO

IF COL_LENGTH('dbo.SmsEnvio', 'plantilla_codigo') IS NULL
  ALTER TABLE dbo.SmsEnvio ADD plantilla_codigo VARCHAR(50) NULL;
GO

INSERT INTO dbo.SmsPlantilla (codigo, nombre, descripcion, tipo_mensaje_id, contenido, version, activo, fecha_creacion)
SELECT v.codigo, v.nombre, v.descripcion, t.id_sms_tipo_mensaje, v.contenido, 1, 1, SYSDATETIME()
FROM (VALUES
  ('RECORDATORIO_CITA', N'Recordatorio de cita', N'Ejemplo: recordatorio con fecha, hora y sede', 'NORMAL',
   N'Hola {{nombre}}, le recordamos su cita el {{fecha}} a las {{hora}} en {{sede}}. Comfenalco Quindio'),
  ('OTP_VERIFICACION', N'Código de verificación', N'Ejemplo: OTP con vigencia', 'OTP',
   N'Comfenalco: su codigo de verificacion es {{codigo}}. Vence en {{minutos}} minutos. No lo comparta.')
) v (codigo, nombre, descripcion, tipo, contenido)
LEFT JOIN dbo.SmsTipoMensaje t ON t.codigo = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsPlantilla p WHERE p.codigo = v.codigo);
GO

INSERT INTO dbo.SmsPlantillaVariable (plantilla_id, nombre, tipo_dato, obligatoria, longitud_max, patron, valor_defecto, descripcion, orden)
SELECT p.id_sms_plantilla, v.nombre, v.tipo_dato, v.obligatoria, v.longitud_max, v.patron, v.valor_defecto, v.descripcion, v.orden
FROM (VALUES
  ('RECORDATORIO_CITA', 'nombre',  'TEXTO',  1, 30,   NULL,         NULL,              N'Primer nombre del afiliado', 0),
  ('RECORDATORIO_CITA', 'fecha',   'FECHA',  1, NULL, NULL,         NULL,              N'yyyy-MM-dd o dd/MM/yyyy',    1),
  ('RECORDATORIO_CITA', 'hora',    'HORA',   1, NULL, NULL,         NULL,              N'HH:mm (24 h)',               2),
  ('RECORDATORIO_CITA', 'sede',    'TEXTO',  0, 40,   NULL,         N'Sede principal', N'Sede de atención',           3),
  ('OTP_VERIFICACION',  'codigo',  'TEXTO',  1, 8,    N'[0-9]{4,8}', NULL,             N'PIN de 4 a 8 dígitos',       0),
  ('OTP_VERIFICACION',  'minutos', 'ENTERO', 0, 3,    NULL,         N'5',              N'Vigencia en minutos',        1)
) v (plantilla, nombre, tipo_dato, obligatoria, longitud_max, patron, valor_defecto, descripcion, orden)
JOIN dbo.SmsPlantilla p ON p.codigo = v.plantilla
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsPlantillaVariable x WHERE x.plantilla_id = p.id_sms_plantilla AND x.nombre = v.nombre);
GO
