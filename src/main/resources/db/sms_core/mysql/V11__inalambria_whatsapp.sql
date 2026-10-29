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
INSERT INTO CanalTipoMensaje (codigo, descripcion, canal_mensaje, otp, masivo, eliminado)
SELECT 'WHATSAPP', 'Mensaje de WhatsApp con plantilla aprobada por Meta', 'WHATSAPP', b'0', b'1', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CanalTipoMensaje WHERE codigo = 'WHATSAPP' AND canal = 'SMS');

-- 2. Cuenta WhatsApp de Inalambria
SET @prov := (SELECT id_canal_proveedor FROM CanalProveedor WHERE codigo = 'INALAMBRIA' AND canal = 'SMS');

INSERT INTO CanalCuenta (canal_proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado)
SELECT @prov, 'INALAMBRIA_WHATSAPP', 'env:INALAMBRIA_WHATSAPP_USER', 'INALAMBRIA_WHATSAPP_PASSWORD', NULL, NULL,
       '{"canal":"WHATSAPP","whatsappUrl":"https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp"}', b'0'
FROM DUAL WHERE @prov IS NOT NULL AND NOT EXISTS (SELECT 1 FROM CanalCuenta WHERE codigo = 'INALAMBRIA_WHATSAPP');

-- 3. Servicio WHATSAPP
INSERT INTO CanalProveedorServicio (canal_proveedor_id, tipo_mensaje_id, eliminado, created_at)
SELECT p.id_canal_proveedor, t.id_tipo_mensaje, b'0', NOW(6)
FROM (SELECT 'INALAMBRIA' AS proveedor UNION ALL SELECT 'MOCK') v
JOIN CanalProveedor   p ON p.codigo = v.proveedor AND p.canal = 'SMS'
JOIN CanalTipoMensaje t ON t.codigo = 'WHATSAPP' AND t.canal = 'SMS'
WHERE NOT EXISTS (SELECT 1 FROM CanalProveedorServicio s
                  WHERE s.canal_proveedor_id = p.id_canal_proveedor AND s.tipo_mensaje_id = t.id_tipo_mensaje);

-- 4. Rutas

INSERT INTO CanalRuta (canal_proveedor_id, tipo_mensaje_id, eliminado)
SELECT DISTINCT pr.id_canal_proveedor, tm.id_tipo_mensaje, b'0'
FROM tmp_sms_pasos p
JOIN CanalTipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN CanalProveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_canal_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM CanalRuta r
                  WHERE r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.canal_proveedor_id <=> pr.id_canal_proveedor);

INSERT INTO CanalRutaPaso (ruta_id, orden, canal_cuenta_id, max_intentos, espera_ms, eliminado)
SELECT r.id_ruta, p.orden, c.id_canal_cuenta, 3, 1000, b'0'
FROM tmp_sms_pasos p
JOIN CanalTipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN CanalProveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
JOIN CanalRuta r   ON r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.canal_proveedor_id <=> pr.id_canal_proveedor AND r.canal = 'SMS'
JOIN CanalCuenta c ON c.codigo = CAST(p.cuenta AS BINARY) AND c.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_canal_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM CanalRutaPaso x WHERE x.ruta_id = r.id_ruta AND x.orden = p.orden);


-- 5. (Opcional) número de origen. Descomente y ajuste:
-- UPDATE CanalCuenta SET remitente = '573001234567' WHERE codigo = 'INALAMBRIA_WHATSAPP';

-- Verificación
SELECT c.codigo, c.usuario, c.secret_ref, c.clave_cifrada IS NOT NULL AS tiene_clave_cifrada, c.remitente AS source, c.parametros_json
FROM CanalCuenta c WHERE c.codigo = 'INALAMBRIA_WHATSAPP';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
