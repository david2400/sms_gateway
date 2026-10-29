/* =====================================================================
   V10 - Envío MASIVO nativo de Inalambria (MySQL). Idempotente. Requiere V2 + V8.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   API: POST https://rest.inalambria.com/mtmessage/massive  (docs.inalambria.com,
        "Mensaje SMS Unidireccional (Batch - En Lotes)"), autenticación Basic con la cuenta.
   El motor masivo (POST /v1/sms/batch y /v1/sms/inalambria/batch) usa esta API para los pasos
   de cuentas INALAMBRIA: lotes de sms.inalambria.massive.chunk-size (500), 3 intentos por lote
   y, si se agotan, el lote completo pasa a la siguiente cuenta de la ruta (mismo tipo de mensaje).

   Parámetros opcionales de CanalCuenta.parametros_json para el masivo:
     massiveUrl        URL del endpoint (defecto: la de application.yaml)
     massiveChunkSize  destinatarios por petición para esta cuenta
     xAuth             valor del header x-auth (la doc lo marca como requerido sin explicarlo:
                       confirmar con Inalambria; si no se configura no se envía)
     notifyUrl         URL de callback de estados (notifyContentType por defecto application/json)
   ===================================================================== */

-- 1. URL masiva explícita en la cuenta masiva (no pisa un valor ya configurado)
UPDATE CanalCuenta
SET parametros_json = JSON_SET(IF(parametros_json IS NULL OR JSON_VALID(parametros_json) = 0, '{}', parametros_json),
                               '$.massiveUrl', 'https://rest.inalambria.com/mtmessage/massive')
WHERE codigo = 'INALAMBRIA_MASIVO'
  AND (parametros_json IS NULL OR JSON_VALID(parametros_json) = 0
       OR JSON_EXTRACT(parametros_json, '$.massiveUrl') IS NULL);

-- 2. Rutas MASIVO (ya las crean V2/V5; se garantizan aquí por si se borraron)
DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('INALAMBRIA', 'MASIVO', 1, 'INALAMBRIA_MASIVO'),
 (NULL,         'MASIVO', 1, 'INALAMBRIA_MASIVO');

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
  AND NOT EXISTS (SELECT 1 FROM CanalRutaPaso x WHERE x.ruta_id = r.id_ruta);


-- Verificación
SELECT codigo, JSON_UNQUOTE(JSON_EXTRACT(parametros_json, '$.massiveUrl')) AS massive_url, parametros_json
FROM CanalCuenta WHERE codigo = 'INALAMBRIA_MASIVO';

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
