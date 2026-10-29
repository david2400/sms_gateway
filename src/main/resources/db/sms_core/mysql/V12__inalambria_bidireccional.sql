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

INSERT INTO CanalTipoMensaje (codigo, descripcion, canal_mensaje, otp, masivo, eliminado)
SELECT 'BIDIRECCIONAL', 'SMS bidireccional desde código corto (el destinatario puede responder)', 'SMS', b'0', b'0', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CanalTipoMensaje WHERE codigo = 'BIDIRECCIONAL' AND canal = 'SMS');


SET @prov := (SELECT id_canal_proveedor FROM CanalProveedor WHERE codigo = 'INALAMBRIA' AND canal = 'SMS');

INSERT INTO CanalCuenta (canal_proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado)
SELECT @prov, 'INALAMBRIA_BIDIRECCIONAL', 'env:INALAMBRIA_BIDIRECCIONAL_USER', 'INALAMBRIA_BIDIRECCIONAL_PASSWORD', NULL, NULL,
       '{"bidireccionalUrl":"https://rest.inalambria.com/momessage"}', b'0'
FROM DUAL WHERE @prov IS NOT NULL AND NOT EXISTS (SELECT 1 FROM CanalCuenta WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL');

INSERT INTO CanalProveedorServicio (canal_proveedor_id, tipo_mensaje_id, eliminado, created_at)
SELECT p.id_canal_proveedor, t.id_tipo_mensaje, b'0', NOW(6)
FROM (SELECT 'INALAMBRIA' AS proveedor UNION ALL SELECT 'MOCK') v
JOIN CanalProveedor   p ON p.codigo = v.proveedor AND p.canal = 'SMS'
JOIN CanalTipoMensaje t ON t.codigo = 'BIDIRECCIONAL' AND t.canal = 'SMS'
WHERE NOT EXISTS (SELECT 1 FROM CanalProveedorServicio s
                  WHERE s.canal_proveedor_id = p.id_canal_proveedor AND s.tipo_mensaje_id = t.id_tipo_mensaje);

DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('INALAMBRIA', 'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL'),
 (NULL,         'BIDIRECCIONAL', 1, 'INALAMBRIA_BIDIRECCIONAL'),
 ('MOCK',       'BIDIRECCIONAL', 1, 'MOCK_OK');

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


-- Código corto (descomente y ajuste):
-- UPDATE CanalCuenta SET remitente = '12345' WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL';

SELECT codigo, usuario, secret_ref, remitente AS short_code, parametros_json FROM CanalCuenta WHERE codigo = 'INALAMBRIA_BIDIRECCIONAL';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
