/* =====================================================================
   V3 - Proveedor MOCK (módulo Mock, solo pruebas). Idempotente.
   Requiere: V1__schema.sql
   No hace llamadas HTTP. parametros_json.simulateStatus = HTTP simulado.
   En producción se puede omitir o dejar con activo = 0.
   ===================================================================== */

IF NOT EXISTS (SELECT 1 FROM dbo.SmsProveedor WHERE codigo = 'MOCK')
  INSERT INTO dbo.SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo, fecha_creacion)
  VALUES ('MOCK', 'Mock (pruebas)', 'https://mock.local', 1000, 1000, 1, SYSDATETIME());
GO

DECLARE @prov BIGINT = (SELECT id_sms_proveedor FROM dbo.SmsProveedor WHERE codigo = 'MOCK');

INSERT INTO dbo.SmsCuenta (proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, activo)
SELECT @prov, v.codigo, 'mock', 'SMS_MOCK_PASSWORD', NULL, v.parametros_json, 1
FROM (VALUES
  ('MOCK_OK',    N'{"simulateStatus":200}'),
  ('MOCK_FALLA', N'{"simulateStatus":500}')
) AS v (codigo, parametros_json)
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsCuenta c WHERE c.codigo = v.codigo);
GO

-- Rutas del endpoint propio  POST /v1/sms/mock/send
-- OTP: la cuenta que falla va primero -> 3 intentos fallidos + fallback a MOCK_OK
EXEC dbo.SmsRuta_AgregarPaso 'MOCK', 'OTP',    1, 'MOCK_FALLA';
EXEC dbo.SmsRuta_AgregarPaso 'MOCK', 'OTP',    2, 'MOCK_OK';
EXEC dbo.SmsRuta_AgregarPaso 'MOCK', 'NORMAL', 1, 'MOCK_OK';
GO
