/* =====================================================================
   V3 - Proveedor MOCK (MySQL, solo pruebas). Idempotente. Requiere V1.
   No hace llamadas HTTP. parametros_json.simulateStatus = HTTP simulado.
   ===================================================================== */

INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo, fecha_creacion)
SELECT 'MOCK', 'Mock (pruebas)', 'https://mock.local', 1000, 1000, b'1', NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'MOCK');

SET @prov = (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'MOCK');

INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, activo)
SELECT @prov, v.codigo, 'mock', 'SMS_MOCK_PASSWORD', NULL, v.parametros_json, b'1'
FROM (
  SELECT 'MOCK_OK' AS codigo, '{"simulateStatus":200}' AS parametros_json
  UNION ALL SELECT 'MOCK_FALLA', '{"simulateStatus":500}'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta c WHERE c.codigo = v.codigo);

-- Rutas del endpoint propio  POST /v1/sms/mock/send
-- OTP: la cuenta que falla va primero -> 3 intentos fallidos + fallback a MOCK_OK
DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('MOCK', 'OTP',    1, 'MOCK_FALLA'),
 ('MOCK', 'OTP',    2, 'MOCK_OK'),
 ('MOCK', 'NORMAL', 1, 'MOCK_OK');

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, activo)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, b'1'
FROM tmp_sms_pasos p
JOIN SmsTipoMensaje tm ON tm.codigo = p.tipo
LEFT JOIN SmsProveedor pr ON pr.codigo = p.proveedor
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, activo)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, b'1'
FROM tmp_sms_pasos p
JOIN SmsTipoMensaje tm ON tm.codigo = p.tipo
LEFT JOIN SmsProveedor pr ON pr.codigo = p.proveedor
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = p.cuenta
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);

DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
