/* =====================================================================
   V12 - SMS bidireccional de Inalambria (MySQL). Idempotente. Requiere V1, V2, V6__tipos_mensaje, V8.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   API: POST https://rest.inalambria.com/momessage (Basic) body {MessageText, PhoneNumber, ShortCode, Url?}
        respuesta {Id (28 dígitos), Message OK|ERROR, Status true|false}
   Envío: POST /v1/sms/send o /v1/sms/inalambria/send con "tipo_mensaje": "BIDIRECCIONAL".

   Crea:
     - Tipo de mensaje BIDIRECCIONAL (canal SMS).
     - Cuenta INALAMBRIA_BIDIRECCIONAL. CONFIGURAR ANTES DE USAR:
         usuario   -> 'env:INALAMBRIA_BIDIRECCIONAL_USER' (o el usuario literal)
         clave     -> variable INALAMBRIA_BIDIRECCIONAL_PASSWORD (secret_ref) o PUT /v1/sms/admin/cuentas/{id} con "clave"
         remitente -> código corto asignado por Inalambria (ej. 12345); también parametros_json.shortCode
     - Servicio BIDIRECCIONAL para INALAMBRIA y MOCK (pruebas). GO4CLIENTS no lo tiene.
     - Rutas: INALAMBRIA/BIDIRECCIONAL y central/BIDIRECCIONAL -> INALAMBRIA_BIDIRECCIONAL; MOCK/BIDIRECCIONAL -> MOCK_OK.
   Las respuestas del usuario (MO) no las documenta este endpoint: pendiente (webhook/callback de Inalambria).
   ===================================================================== */

SET NAMES utf8mb4;

INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, eliminado, usr_crea)
SELECT 'BIDIRECCIONAL', 'SMS bidireccional desde código corto (el destinatario puede responder)', 'SMS', b'0', b'0', 0, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje WHERE codigo = 'BIDIRECCIONAL');


INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'INALAMBRIA'), 'INALAMBRIA_BIDIRECCIONAL', 'env:INALAMBRIA_BIDIRECCIONAL_USER', 'INALAMBRIA_BIDIRECCIONAL_PASSWORD', NULL, NULL,
       '{"bidireccionalUrl":"https://rest.inalambria.com/momessage"}', 0, 1
FROM DUAL WHERE EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'INALAMBRIA') AND NOT EXISTS (SELECT 1 FROM SmsCuenta WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL');

INSERT INTO SmsProveedorServicio (proveedor_id, tipo_mensaje_id, eliminado, usr_crea, created_at)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 0, 1, NOW(6)
FROM (SELECT 'INALAMBRIA' AS proveedor UNION ALL SELECT 'MOCK') v
JOIN SmsProveedor   p ON p.codigo = v.proveedor
JOIN SmsTipoMensaje t ON t.codigo = 'BIDIRECCIONAL'
WHERE NOT EXISTS (SELECT 1 FROM SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);

-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'BIDIRECCIONAL' AS tipo, 1 AS orden, 'INALAMBRIA_BIDIRECCIONAL' AS cuenta
  UNION ALL SELECT NULL, 'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL'
  UNION ALL SELECT 'MOCK', 'BIDIRECCIONAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 1000, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'BIDIRECCIONAL' AS tipo, 1 AS orden, 'INALAMBRIA_BIDIRECCIONAL' AS cuenta
  UNION ALL SELECT NULL, 'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL'
  UNION ALL SELECT 'MOCK', 'BIDIRECCIONAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


-- Código corto (descomente y ajuste):
-- UPDATE SmsCuenta SET remitente = '12345' WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL';

SELECT codigo, usuario, secret_ref, remitente AS short_code, parametros_json FROM SmsCuenta WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
