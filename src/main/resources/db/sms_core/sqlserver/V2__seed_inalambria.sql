/* =====================================================================
   V2 - Proveedor INALAMBRIA (módulo INALAMBRIA). Idempotente.
   Requiere: V1__schema.sql

   Contraseñas en variables de entorno (secret_ref = nombre de la variable):
     INALAMBRIA_OTP_PASSWORD, INALAMBRIA_MASIVO_PASSWORD,
     INALAMBRIA_HABEASDATA_PASSWORD, INALAMBRIA_TEMPLATE_PASSWORD,
     INALAMBRIA_PERSONALIZADA_PASSWORD
   parametros_json: type / messagePattern / templateId (RESTAWS v3.1)
   ===================================================================== */

-- 1. Proveedor
IF NOT EXISTS (SELECT 1 FROM dbo.SmsProveedor WHERE codigo = 'INALAMBRIA')
  INSERT INTO dbo.SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo, fecha_creacion)
  VALUES ('INALAMBRIA', 'Inalambria', 'https://restaws.inalambria.com', 3000, 10000, 1, SYSDATETIME());
GO

-- 2. Cuentas
DECLARE @prov BIGINT = (SELECT id_sms_proveedor FROM dbo.SmsProveedor WHERE codigo = 'INALAMBRIA');

INSERT INTO dbo.SmsCuenta (proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, activo)
SELECT @prov, v.codigo, v.usuario, v.secret_ref, NULL, v.parametros_json, 1
FROM (VALUES
  ('INALAMBRIA_OTP',           'COMFENALCOquindioOTP',         'INALAMBRIA_OTP_PASSWORD',           N'{"type":1,"messagePattern":"0","templateId":0}'),
  ('INALAMBRIA_MASIVO',        'COMFENALCOquindioMASIVO',      'INALAMBRIA_MASIVO_PASSWORD',        N'{"type":1,"messagePattern":"0","templateId":0}'),
  ('INALAMBRIA_HABEASDATA',    'COMFENALCOquindioCOMUNICADOR', 'INALAMBRIA_HABEASDATA_PASSWORD',    N'{"type":1,"messagePattern":"0","templateId":0}'),
  ('INALAMBRIA_TEMPLATE',      'COMFENALCOquindioOTP',         'INALAMBRIA_TEMPLATE_PASSWORD',      N'{"type":1,"messagePattern":"1","templateId":0}'),
  ('INALAMBRIA_PERSONALIZADA', 'COMFENALCOquindioOTP',         'INALAMBRIA_PERSONALIZADA_PASSWORD', N'{"type":1,"messagePattern":"1","templateId":0}')
) AS v (codigo, usuario, secret_ref, parametros_json)
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsCuenta c WHERE c.codigo = v.codigo);
GO

-- 3. Rutas del endpoint propio  POST /v1/sms/inalambria/send
--                              proveedor     tipo            orden cuenta
EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'OTP',          1, 'INALAMBRIA_OTP';
EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'OTP',          2, 'INALAMBRIA_MASIVO';
EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'HABEAS_DATA',  1, 'INALAMBRIA_HABEASDATA';
EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'TEMPLATE',     1, 'INALAMBRIA_TEMPLATE';
EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA';
EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'NORMAL',       1, 'INALAMBRIA_MASIVO';
EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'MASIVO',       1, 'INALAMBRIA_MASIVO';
GO
