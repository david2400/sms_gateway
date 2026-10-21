/* =====================================================================
   V2 - Proveedor INALAMBRIA (módulo INALAMBRIA). Idempotente.
   Requiere: V1__schema.sql

   Contraseñas CIFRADAS (AES-256-GCM) en clave_cifrada. Se descifran al enviar con la llave
   de la variable de entorno SMS_CRYPTO_KEY (nunca en BD ni en el repositorio).
   Si cambia la llave, vuelva a cifrar (ClaveCifradaCli o PUT /v1/sms/admin/cuentas/{id} con 'clave').
   parametros_json: type / messagePattern / templateId (RESTAWS v3.1)
   ===================================================================== */

-- 1. Proveedor
IF NOT EXISTS (SELECT 1 FROM dbo.SmsProveedor WHERE codigo = 'INALAMBRIA')
  INSERT INTO dbo.SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo, fecha_creacion)
  VALUES ('INALAMBRIA', 'Inalambria', 'https://restaws.inalambria.com', 3000, 10000, 1, SYSDATETIME());
GO

-- 2. Cuentas
DECLARE @prov BIGINT = (SELECT id_sms_proveedor FROM dbo.SmsProveedor WHERE codigo = 'INALAMBRIA');

INSERT INTO dbo.SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, activo)
SELECT @prov, v.codigo, v.usuario, NULL, v.clave_cifrada, NULL, v.parametros_json, 1
FROM (VALUES
  ('INALAMBRIA_OTP',           'COMFENALCOquindioOTP',
   'v1:MOIs/G3Q9IWGgpOqaYikpP3lactcYqEPCVnrrCtPhKF+0Xc=', N'{"type":1,"messagePattern":"0","templateId":0}'),
  ('INALAMBRIA_MASIVO',        'COMFENALCOquindioMASIVO',
   'v1:zqg+2HKF6qCl2NTp2pQlnWwrrnGXtzu+2Iwj2VWSwKCGE3gVyd8=', N'{"type":1,"messagePattern":"0","templateId":0}'),
  ('INALAMBRIA_HABEASDATA',    'COMFENALCOquindioCOMUNICADOR',
   'v1:Wpp8p25Jr6cif+9OrAWOraLPR9vfw4pE6oMxGcmjoaldjQft', N'{"type":1,"messagePattern":"0","templateId":0}'),
  ('INALAMBRIA_TEMPLATE',      'COMFENALCOquindioOTP',
   'v1:g04hQjF0uvZaUSCrcNO4OWJLuIMoJrq5D5KUuMWqehGew8U=', N'{"type":1,"messagePattern":"1","templateId":0}'),
  ('INALAMBRIA_PERSONALIZADA', 'COMFENALCOquindioOTP',
   'v1:POKjUCKJbM1z/8VwYNPztUKTleO0Kf85gNEi5cG0Eh18bWs=', N'{"type":1,"messagePattern":"1","templateId":0}')
) AS v (codigo, usuario, clave_cifrada, parametros_json)
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
