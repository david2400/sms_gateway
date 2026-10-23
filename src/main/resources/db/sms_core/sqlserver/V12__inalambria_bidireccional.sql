/* =====================================================================
   V12 - SMS bidireccional de Inalambria (SQL Server). Idempotente. Requiere V2 + V8.
   Equivalente a mysql/V12__inalambria_bidireccional.sql (ver ahí qué configurar en la cuenta).
   ===================================================================== */

IF NOT EXISTS (SELECT 1 FROM dbo.SmsTipoMensaje WHERE codigo = 'BIDIRECCIONAL')
  INSERT INTO dbo.SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, activo)
  VALUES ('BIDIRECCIONAL', N'SMS bidireccional desde código corto (el destinatario puede responder)', 'SMS', 0, 0, 1);
GO

DECLARE @prov BIGINT = (SELECT id_sms_proveedor FROM dbo.SmsProveedor WHERE codigo = 'INALAMBRIA');
IF @prov IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.SmsCuenta WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL')
  INSERT INTO dbo.SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, activo)
  VALUES (@prov, 'INALAMBRIA_BIDIRECCIONAL', 'env:INALAMBRIA_BIDIRECCIONAL_USER', 'INALAMBRIA_BIDIRECCIONAL_PASSWORD', NULL, NULL,
          N'{"bidireccionalUrl":"https://rest.inalambria.com/momessage"}', 1);
GO

INSERT INTO dbo.SmsProveedorServicio (proveedor_id, tipo_mensaje_id, activo, fecha_creacion)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 1, SYSDATETIME()
FROM (VALUES ('INALAMBRIA'), ('MOCK')) v (proveedor)
JOIN dbo.SmsProveedor   p ON p.codigo = v.proveedor
JOIN dbo.SmsTipoMensaje t ON t.codigo = 'BIDIRECCIONAL'
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);
GO

EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL';
EXEC dbo.SmsRuta_AgregarPaso NULL,         'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL';
EXEC dbo.SmsRuta_AgregarPaso 'MOCK',       'BIDIRECCIONAL', 1, 'MOCK_OK';
GO

-- UPDATE dbo.SmsCuenta SET remitente = '12345' WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL';
