/* =====================================================================
   V11 - WhatsApp de Inalambria (SQL Server, BD MAESTRA). Idempotente. Requiere V2 + V8.
   Equivalente a mysql/V11__inalambria_whatsapp.sql (ver ahí qué configurar en la cuenta).
   NOTA: las rutas usan dbo.SmsRuta_AgregarPaso, que en sqlserver/ aún trabaja con SmsRuta.tipo_mensaje
   (texto); cuando se porte V7 (tipo_mensaje_id) hay que ajustar ese procedimiento.
   ===================================================================== */

IF NOT EXISTS (SELECT 1 FROM dbo.SmsTipoMensaje WHERE codigo = 'WHATSAPP')
  INSERT INTO dbo.SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, activo)
  VALUES ('WHATSAPP', N'Mensaje de WhatsApp con plantilla aprobada por Meta', 'WHATSAPP', 0, 1, 1);
GO

DECLARE @prov BIGINT = (SELECT id_sms_proveedor FROM dbo.SmsProveedor WHERE codigo = 'INALAMBRIA');
IF @prov IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.SmsCuenta WHERE codigo = 'INALAMBRIA_WHATSAPP')
  INSERT INTO dbo.SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, activo)
  VALUES (@prov, 'INALAMBRIA_WHATSAPP', 'env:INALAMBRIA_WHATSAPP_USER', 'INALAMBRIA_WHATSAPP_PASSWORD', NULL, NULL,
          N'{"canal":"WHATSAPP","whatsappUrl":"https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp"}', 1);
GO

INSERT INTO dbo.SmsProveedorServicio (proveedor_id, tipo_mensaje_id, activo, fecha_creacion)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 1, SYSDATETIME()
FROM (VALUES ('INALAMBRIA'), ('MOCK')) v (proveedor)
JOIN dbo.SmsProveedor   p ON p.codigo = v.proveedor
JOIN dbo.SmsTipoMensaje t ON t.codigo = 'WHATSAPP'
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);
GO

EXEC dbo.SmsRuta_AgregarPaso 'INALAMBRIA', 'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP';
EXEC dbo.SmsRuta_AgregarPaso NULL,         'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP';
EXEC dbo.SmsRuta_AgregarPaso 'MOCK',       'WHATSAPP', 1, 'MOCK_OK';
GO

-- UPDATE dbo.SmsCuenta SET remitente = '573001234567' WHERE codigo = 'INALAMBRIA_WHATSAPP';
