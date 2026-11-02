/* =====================================================================
   V5 - Rutas del ENDPOINT CENTRAL (MySQL)  POST /v1/sms/send y /v1/sms/batch
   (Ruta.proveedor_id = NULL). Idempotente. Requiere V1 + V2.
   Cada paso = una cuenta; el motor hace max_intentos (3) en esa cuenta y
   luego pasa al siguiente orden. NO pisa pasos existentes: para cambiar
   una ruta use SmsAdmin (/v1/sms/admin/rutas) o un UPDATE.
   ===================================================================== */

-- Pasos (proveedor, tipo, orden, cuenta)
DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 (NULL, 'OTP',          1, 'INALAMBRIA_OTP'),
 (NULL, 'OTP',          2, 'INALAMBRIA_MASIVO'),
 (NULL, 'HABEAS_DATA',  1, 'INALAMBRIA_HABEASDATA'),
 (NULL, 'TEMPLATE',     1, 'INALAMBRIA_TEMPLATE'),
 (NULL, 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'),
 (NULL, 'NORMAL',       1, 'INALAMBRIA_MASIVO'),
 (NULL, 'MASIVO',       1, 'INALAMBRIA_MASIVO');
 -- Opcional, Go4Clients como respaldo (requiere V4): agregar a la lista
 -- (NULL, 'OTP', 3, 'GO4CLIENTS_PRINCIPAL'), (NULL, 'NORMAL', 2, 'GO4CLIENTS_PRINCIPAL'), (NULL, 'MASIVO', 2, 'GO4CLIENTS_PRINCIPAL')

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


-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
