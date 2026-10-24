/* =====================================================================
   V11 - WhatsApp de Inalambria (plantillas de Meta) (MySQL). Idempotente. Requiere V1, V2, V6__tipos_mensaje, V8.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   API: POST https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp (Basic)
        body {source, destination, templateId, parameters[], optionalAditionalData{}}
   Endpoints: POST /v1/whatsapp/send (central) y POST /v1/whatsapp/inalambria/send

   Crea:
     - Tipo de mensaje WHATSAPP (canal WHATSAPP).
     - Cuenta INALAMBRIA_WHATSAPP. CONFIGURAR ANTES DE USAR:
         usuario  -> 'env:INALAMBRIA_WHATSAPP_USER'  (o el usuario literal)
         clave    -> variable de entorno INALAMBRIA_WHATSAPP_PASSWORD (secret_ref), o mejor
                     PUT /v1/sms/admin/cuentas/{id} con "clave" (se guarda cifrada)
         remitente-> número de origen autorizado por Inalambria/Meta (ej. 573001234567)
     - parametros_json.canal = WHATSAPP: la cuenta solo envía WhatsApp y las cuentas SMS no envían WhatsApp.
     - Servicio WHATSAPP para INALAMBRIA y MOCK (pruebas). GO4CLIENTS no lo tiene.
     - Rutas: INALAMBRIA/WHATSAPP, central/WHATSAPP -> INALAMBRIA_WHATSAPP; MOCK/WHATSAPP -> MOCK_OK.
   Las cuentas SMS de Inalambria NO tienen el canal WhatsApp: no se agregan a rutas WHATSAPP.
   ===================================================================== */

SET NAMES utf8mb4;

-- 1. Tipo de mensaje
INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, eliminado, usr_crea)
SELECT 'WHATSAPP', 'Mensaje de WhatsApp con plantilla aprobada por Meta', 'WHATSAPP', b'0', b'1', 0, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje WHERE codigo = 'WHATSAPP');

-- 2. Cuenta WhatsApp de Inalambria

INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'INALAMBRIA'), 'INALAMBRIA_WHATSAPP', 'env:INALAMBRIA_WHATSAPP_USER', 'INALAMBRIA_WHATSAPP_PASSWORD', NULL, NULL,
       '{"canal":"WHATSAPP","whatsappUrl":"https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp"}', 0, 1
FROM DUAL WHERE EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'INALAMBRIA') AND NOT EXISTS (SELECT 1 FROM SmsCuenta WHERE codigo = 'INALAMBRIA_WHATSAPP');

-- 3. Servicio WHATSAPP
INSERT INTO SmsProveedorServicio (proveedor_id, tipo_mensaje_id, eliminado, usr_crea, created_at)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, 0, 1, NOW(6)
FROM (SELECT 'INALAMBRIA' AS proveedor UNION ALL SELECT 'MOCK') v
JOIN SmsProveedor   p ON p.codigo = v.proveedor
JOIN SmsTipoMensaje t ON t.codigo = 'WHATSAPP'
WHERE NOT EXISTS (SELECT 1 FROM SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);

-- 4. Rutas
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'WHATSAPP' AS tipo, 1 AS orden, 'INALAMBRIA_WHATSAPP' AS cuenta
  UNION ALL SELECT NULL, 'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP'
  UNION ALL SELECT 'MOCK', 'WHATSAPP', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 1000, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'WHATSAPP' AS tipo, 1 AS orden, 'INALAMBRIA_WHATSAPP' AS cuenta
  UNION ALL SELECT NULL, 'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP'
  UNION ALL SELECT 'MOCK', 'WHATSAPP', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


-- 5. (Opcional) número de origen. Descomente y ajuste:
-- UPDATE SmsCuenta SET remitente = '573001234567' WHERE codigo = 'INALAMBRIA_WHATSAPP';

-- Verificación
SELECT c.codigo, c.usuario, c.secret_ref, c.clave_cifrada IS NOT NULL AS tiene_clave_cifrada, c.remitente AS source, c.parametros_json
FROM SmsCuenta c WHERE c.codigo = 'INALAMBRIA_WHATSAPP';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
