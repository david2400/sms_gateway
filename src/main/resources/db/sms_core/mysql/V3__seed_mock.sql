/* =====================================================================
   V3 - Proveedor MOCK (MySQL, solo pruebas). Idempotente. Requiere V1.
   No hace llamadas HTTP. parametros_json.simulateStatus = HTTP simulado.
   ===================================================================== */

INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, usr_crea, created_at)
SELECT 'MOCK', 'Mock (pruebas)', 'https://mock.local', 1000, 1000, 0, 1, NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'MOCK');


INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'MOCK'), v.codigo, 'mock', 'SMS_MOCK_PASSWORD', NULL, v.parametros_json, 0, 1
FROM (
  SELECT 'MOCK_OK' AS codigo, '{"simulateStatus":200}' AS parametros_json
  UNION ALL SELECT 'MOCK_FALLA', '{"simulateStatus":500}'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta c WHERE c.codigo = v.codigo);

-- Rutas del endpoint propio  POST /v1/sms/mock/send
-- OTP: la cuenta que falla va primero -> 3 intentos fallidos + fallback a MOCK_OK
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'MOCK' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'MOCK_FALLA' AS cuenta
  UNION ALL SELECT 'MOCK', 'OTP', 2, 'MOCK_OK'
  UNION ALL SELECT 'MOCK', 'NORMAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, 0, 1
FROM (
SELECT 'MOCK' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'MOCK_FALLA' AS cuenta
  UNION ALL SELECT 'MOCK', 'OTP', 2, 'MOCK_OK'
  UNION ALL SELECT 'MOCK', 'NORMAL', 1, 'MOCK_OK'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);

