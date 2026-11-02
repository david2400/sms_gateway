/* =====================================================================
   V3 - Proveedor MOCK (MySQL, solo pruebas). Idempotente. Requiere V1.
   No hace llamadas HTTP. parametros_json.simulateStatus = HTTP simulado.
   ===================================================================== */

INSERT INTO Proveedor (canal, codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, created_at)
SELECT 'SMS', 'MOCK', 'Mock (pruebas)', 'https://mock.local', 1000, 1000, b'0', NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Proveedor WHERE codigo = 'MOCK' AND canal = 'SMS');


SET @prov := (SELECT id_proveedor FROM Proveedor WHERE codigo = 'MOCK' AND canal = 'SMS');

INSERT INTO Cuenta (canal, proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, eliminado)
SELECT 'SMS', @prov, v.codigo, 'mock', 'SMS_MOCK_PASSWORD', NULL, v.parametros_json, b'0'
FROM (
  SELECT 'MOCK_OK' AS codigo, '{"simulateStatus":200}' AS parametros_json
  UNION ALL SELECT 'MOCK_FALLA', '{"simulateStatus":500}'
) v
WHERE NOT EXISTS (SELECT 1 FROM Cuenta c WHERE c.codigo = v.codigo AND c.canal = 'SMS');

-- Rutas del endpoint propio  POST /v1/sms/mock/send
-- OTP: la cuenta que falla va primero -> 3 intentos fallidos + fallback a MOCK_OK
-- Pasos (proveedor, tipo, orden, cuenta) en una tabla temporal de esta misma sesión

DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('MOCK', 'OTP',    1, 'MOCK_FALLA'),
 ('MOCK', 'OTP',    2, 'MOCK_OK'),
 ('MOCK', 'NORMAL', 1, 'MOCK_OK');

INSERT INTO Ruta (canal, proveedor_id, tipo_mensaje_id, eliminado)
SELECT DISTINCT 'SMS', pr.id_proveedor, tm.id_tipo_mensaje, b'0'
FROM tmp_sms_pasos p
JOIN TipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN Proveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM Ruta r
                  WHERE r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.proveedor_id <=> pr.id_proveedor);

INSERT INTO RutaPaso (canal, ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado)
SELECT 'SMS', r.id_ruta, p.orden, c.id_cuenta, 3, 500, b'0'
FROM tmp_sms_pasos p
JOIN TipoMensaje tm ON tm.codigo = p.tipo AND tm.canal = 'SMS'
LEFT JOIN Proveedor pr ON pr.codigo = p.proveedor AND pr.canal = 'SMS'
JOIN Ruta r   ON r.tipo_mensaje_id = tm.id_tipo_mensaje AND r.proveedor_id <=> pr.id_proveedor AND r.canal = 'SMS'
JOIN Cuenta c ON c.codigo = CAST(p.cuenta AS BINARY) AND c.canal = 'SMS'
WHERE (p.proveedor IS NULL OR pr.id_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM RutaPaso x WHERE x.ruta_id = r.id_ruta AND x.orden = p.orden);


DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
