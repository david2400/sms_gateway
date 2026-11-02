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
INSERT INTO TipoMensaje (canal, codigo, descripcion, canal_mensaje, otp, masivo, eliminado)
SELECT 'SMS', 'WHATSAPP', 'Mensaje de WhatsApp con plantilla aprobada por Meta', 'WHATSAPP', b'0', b'1', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TipoMensaje WHERE codigo = 'WHATSAPP' AND canal = 'SMS');

-- 2. Cuenta WhatsApp de Inalambria
SET @prov := (SELECT id_proveedor FROM Proveedor WHERE codigo = 'INALAMBRIA' AND canal = 'SMS');

INSERT INTO Cuenta (canal, proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado)
SELECT 'SMS', @prov, 'INALAMBRIA_WHATSAPP', 'env:INALAMBRIA_WHATSAPP_USER', 'INALAMBRIA_WHATSAPP_PASSWORD', NULL, NULL,
       '{"canal":"WHATSAPP","whatsappUrl":"https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp"}', b'0'
FROM DUAL WHERE @prov IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Cuenta WHERE codigo = 'INALAMBRIA_WHATSAPP' AND canal = 'SMS');

-- 3. Servicio WHATSAPP
INSERT INTO ProveedorServicio (canal, proveedor_id, tipo_mensaje_id, eliminado, created_at)
SELECT 'SMS', p.id_proveedor, t.id_tipo_mensaje, b'0', NOW(6)
FROM (SELECT 'INALAMBRIA' AS proveedor UNION ALL SELECT 'MOCK') v
JOIN Proveedor   p ON p.codigo = v.proveedor AND p.canal = 'SMS'
JOIN TipoMensaje t ON t.codigo = 'WHATSAPP' AND t.canal = 'SMS'
WHERE NOT EXISTS (SELECT 1 FROM ProveedorServicio s
                  WHERE s.proveedor_id = p.id_proveedor AND s.tipo_mensaje_id = t.id_tipo_mensaje);

-- 4. Rutas

DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('INALAMBRIA', 'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP'),
 (NULL,         'WHATSAPP', 1, 'INALAMBRIA_WHATSAPP'),
 ('MOCK',       'WHATSAPP', 1, 'MOCK_OK');

INSERT INTO Ruta (canal, proveedor_id, tipo_mensaje_id, eliminado)
SELECT DISTINCT 'SMS', pr.id_proveedor, tm.id_tipo_mensaje, b'0'
FROM tmp_sms_pasos p
JOIN TipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN Proveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM Ruta r
                  WHERE r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.proveedor_id <=> pr.id_proveedor);

INSERT INTO RutaPaso (canal, ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado)
SELECT 'SMS', r.id_ruta, p.orden, c.id_cuenta, 3, 1000, b'0'
FROM tmp_sms_pasos p
JOIN TipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN Proveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
JOIN Ruta r   ON r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.proveedor_id <=> pr.id_proveedor AND r.canal = 'SMS'
JOIN Cuenta c ON c.codigo = CAST(p.cuenta AS BINARY) AND c.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM RutaPaso x WHERE x.ruta_id = r.id_ruta AND x.orden = p.orden);


-- 5. (Opcional) número de origen. Descomente y ajuste:
-- UPDATE Cuenta SET remitente = '573001234567' WHERE codigo = 'INALAMBRIA_WHATSAPP' AND canal = 'SMS';

-- Verificación
SELECT c.codigo, c.usuario, c.secret_ref, c.clave_cifrada IS NOT NULL AS tiene_clave_cifrada, c.remitente AS source, c.parametros_json
FROM Cuenta c WHERE c.codigo = 'INALAMBRIA_WHATSAPP' AND c.canal = 'SMS';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh

DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
