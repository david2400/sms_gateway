/* =====================================================================
   V8 - Servicios por proveedor (SQL Server, BD MAESTRA). Idempotente.
   Equivalente a mysql/V8__servicios_proveedor.sql.

   SmsProveedorServicio = (proveedor, tipo de mensaje) que el proveedor SÍ atiende. El motor responde
   422 SERVICIO_NO_SOPORTADO si se pide un tipo a un proveedor sin el servicio y lo salta en el fallback.
   IMPORTANTE: sin filas, el proveedor no atiende NINGÚN tipo. Ejecutar antes de desplegar.

   NOTA: la carpeta sqlserver/ no tiene aún el catálogo SmsTipoMensaje ni la migración
   SmsRuta.tipo_mensaje -> tipo_mensaje_id (mysql/V6__tipos_mensaje.sql y V7). Este script crea el
   catálogo si falta; la migración de SmsRuta debe portarse antes de usar este código en SQL Server.
   ===================================================================== */

IF OBJECT_ID('dbo.SmsTipoMensaje', 'U') IS NULL
CREATE TABLE dbo.SmsTipoMensaje (
  id_sms_tipo_mensaje BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_tipo_mensaje PRIMARY KEY,
  codigo              VARCHAR(30)  NOT NULL CONSTRAINT uq_sms_tipo_mensaje_codigo UNIQUE,
  descripcion         VARCHAR(255) NULL,
  canal               VARCHAR(20)  NOT NULL,
  otp                 BIT NOT NULL CONSTRAINT df_sms_tipo_mensaje_otp    DEFAULT 0,
  masivo              BIT NOT NULL CONSTRAINT df_sms_tipo_mensaje_masivo DEFAULT 0,
  activo              BIT NOT NULL CONSTRAINT df_sms_tipo_mensaje_activo DEFAULT 1
);
GO

INSERT INTO dbo.SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, activo)
SELECT v.codigo, v.descripcion, 'SMS', v.otp, v.masivo, 1
FROM (VALUES
  ('OTP',          N'Código de verificación de un solo uso',            1, 0),
  ('HABEAS_DATA',  N'Notificación judicial / habeas data',              0, 0),
  ('TEMPLATE',     N'Mensaje basado en plantilla del proveedor',        0, 0),
  ('PERSONALIZED', N'Mensaje con contenido personalizado por destino',  0, 0),
  ('NORMAL',       N'Mensaje estándar / masivo',                        0, 1),
  ('MASIVO',       N'Envío masivo',                                     0, 1)
) v (codigo, descripcion, otp, masivo)
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsTipoMensaje t WHERE t.codigo = v.codigo);
GO

IF OBJECT_ID('dbo.SmsProveedorServicio', 'U') IS NULL
CREATE TABLE dbo.SmsProveedorServicio (
  id_sms_proveedor_servicio BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sms_proveedor_servicio PRIMARY KEY,
  proveedor_id    BIGINT    NOT NULL CONSTRAINT fk_sms_prov_servicio_proveedor REFERENCES dbo.SmsProveedor(id_sms_proveedor),
  tipo_mensaje_id BIGINT    NOT NULL CONSTRAINT fk_sms_prov_servicio_tipo      REFERENCES dbo.SmsTipoMensaje(id_sms_tipo_mensaje),
  activo          BIT       NOT NULL CONSTRAINT df_sms_prov_servicio_activo DEFAULT 1,
  fecha_creacion  DATETIME2 NOT NULL CONSTRAINT df_sms_prov_servicio_fecha  DEFAULT SYSDATETIME(),
  CONSTRAINT uq_sms_proveedor_servicio UNIQUE (proveedor_id, tipo_mensaje_id)
);
GO

-- Go4Clients NO tiene OTP (un OTP por Go4Clients responde SERVICIO_NO_SOPORTADO)
INSERT INTO dbo.SmsProveedorServicio (proveedor_id, tipo_mensaje_id, activo, fecha_creacion)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 1, SYSDATETIME()
FROM (VALUES
  ('INALAMBRIA', 'OTP'), ('INALAMBRIA', 'HABEAS_DATA'), ('INALAMBRIA', 'TEMPLATE'),
  ('INALAMBRIA', 'PERSONALIZED'), ('INALAMBRIA', 'NORMAL'), ('INALAMBRIA', 'MASIVO'),
  ('GO4CLIENTS', 'NORMAL'), ('GO4CLIENTS', 'MASIVO'),
  ('MOCK', 'OTP'), ('MOCK', 'HABEAS_DATA'), ('MOCK', 'TEMPLATE'),
  ('MOCK', 'PERSONALIZED'), ('MOCK', 'NORMAL'), ('MOCK', 'MASIVO')
) v (proveedor, tipo)
JOIN dbo.SmsProveedor   p ON p.codigo = v.proveedor
JOIN dbo.SmsTipoMensaje t ON t.codigo = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);
GO

SELECT p.codigo AS proveedor, t.codigo AS servicio
FROM dbo.SmsProveedorServicio s
JOIN dbo.SmsProveedor p ON p.id_sms_proveedor = s.proveedor_id
JOIN dbo.SmsTipoMensaje t ON t.id_sms_tipo_mensaje = s.tipo_mensaje_id
WHERE s.activo = 1 ORDER BY p.codigo, t.codigo;
GO
