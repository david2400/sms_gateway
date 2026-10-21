/* =====================================================================
   V4 - Proveedor GO4CLIENTS (módulo Go4Clients). Idempotente.
   Requiere: V1__schema.sql

   Credenciales en variables de entorno (no en BD):
     SmsCuenta.usuario    = 'env:GO4CLIENTS_API_KEY'  -> api key
     SmsCuenta.clave_cifrada = api secret cifrado (AES-256-GCM, llave SMS_CRYPTO_KEY)
   ===================================================================== */

IF NOT EXISTS (SELECT 1 FROM dbo.SmsProveedor WHERE codigo = 'GO4CLIENTS')
  INSERT INTO dbo.SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo, fecha_creacion)
  VALUES ('GO4CLIENTS', 'Go4Clients', 'https://cloud.go4clients.com:8580/api', 3000, 15000, 1, SYSDATETIME());
GO

DECLARE @prov BIGINT = (SELECT id_sms_proveedor FROM dbo.SmsProveedor WHERE codigo = 'GO4CLIENTS');

IF NOT EXISTS (SELECT 1 FROM dbo.SmsCuenta WHERE codigo = 'GO4CLIENTS_PRINCIPAL')
  INSERT INTO dbo.SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, activo)
  VALUES (@prov, 'GO4CLIENTS_PRINCIPAL', 'env:GO4CLIENTS_API_KEY', NULL,
         'v1:y8o3E8dikzW62ZxJyJAaUiOhQWHdvwwdYEb/UqlBdwtNumcQNzAo5Iyvnxc=', 'RANDOM', N'{"priority":"HIGH"}', 1);
GO

-- Rutas del endpoint propio  POST /v1/sms/go4clients/send
EXEC dbo.SmsRuta_AgregarPaso 'GO4CLIENTS', 'OTP',    1, 'GO4CLIENTS_PRINCIPAL';
EXEC dbo.SmsRuta_AgregarPaso 'GO4CLIENTS', 'NORMAL', 1, 'GO4CLIENTS_PRINCIPAL';
EXEC dbo.SmsRuta_AgregarPaso 'GO4CLIENTS', 'MASIVO', 1, 'GO4CLIENTS_PRINCIPAL';
GO
