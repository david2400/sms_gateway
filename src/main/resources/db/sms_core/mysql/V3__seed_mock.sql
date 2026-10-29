/* =====================================================================
   V3 - Proveedor MOCK (MySQL, solo pruebas). Idempotente. Requiere V1.
   No hace llamadas HTTP. parametros_json.simulateStatus = HTTP simulado.
   ===================================================================== */

INSERT INTO CanalProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, created_at)
SELECT 'MOCK', 'Mock (pruebas)', 'https://mock.local', 1000, 1000, b'0', NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CanalProveedor WHERE codigo = 'MOCK');


SET @prov := (SELECT id_canal_proveedor FROM CanalProveedor WHERE codigo = 'MOCK' AND canal = 'SMS');

INSERT INTO CanalCuenta (canal_proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, eliminado)
SELECT @prov, v.codigo, 'mock', 'SMS_MOCK_PASSWORD', NULL, v.parametros_json, b'0'
FROM (
  SELECT 'MOCK_OK' AS codigo, '{"simulateStatus":200}' AS parametros_json
  UNION ALL SELECT 'MOCK_FALLA', '{"simulateStatus":500}'
) v
WHERE NOT EXISTS (SELECT 1 FROM CanalCuenta c WHERE c.codigo = v.codigo);

-- Rutas del endpoint propio  POST /v1/sms/mock/send
-- OTP: la cuenta que falla va primero -> 3 intentos fallidos + fallback a MOCK_OK
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO CanalRuta (canal_proveedor_id, tipo_mensaje_id, eliminado)
SELECT DISTINCT pr.id_canal_proveedor, tm.id_tipo_mensaje, b'0'
FROM tmp_sms_pasos p
JOIN CanalTipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN CanalProveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_canal_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM CanalRuta r
                  WHERE r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.canal_proveedor_id <=> pr.id_canal_proveedor);

INSERT INTO CanalRutaPaso (ruta_id, orden, canal_cuenta_id, max_intentos, espera_ms, eliminado)
SELECT r.id_ruta, p.orden, c.id_canal_cuenta, 3, 500, b'0'
FROM tmp_sms_pasos p
JOIN CanalTipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN CanalProveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
JOIN CanalRuta r   ON r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.canal_proveedor_id <=> pr.id_canal_proveedor AND r.canal = 'SMS'
JOIN CanalCuenta c ON c.codigo = CAST(p.cuenta AS BINARY) AND c.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_canal_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM CanalRutaPaso x WHERE x.ruta_id = r.id_ruta AND x.orden = p.orden);

