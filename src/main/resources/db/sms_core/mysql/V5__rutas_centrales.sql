/* =====================================================================
   V5 - Rutas del ENDPOINT CENTRAL (MySQL)  POST /v1/sms/send y /v1/sms/batch
   (SmsRuta.proveedor_id = NULL). Idempotente. Requiere V1 + V2.
   Cada paso = una cuenta; el motor hace max_intentos (3) en esa cuenta y
   luego pasa al siguiente orden. NO pisa pasos existentes: para cambiar
   una ruta use SmsAdmin (/v1/sms/admin/rutas) o un UPDATE.
   ===================================================================== */

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

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje, activo)
SELECT DISTINCT pr.id_sms_proveedor, p.tipo, b'1'
FROM tmp_sms_pasos p
LEFT JOIN SmsProveedor pr ON pr.codigo = p.proveedor
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje = p.tipo AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, activo)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, b'1'
FROM tmp_sms_pasos p
LEFT JOIN SmsProveedor pr ON pr.codigo = p.proveedor
JOIN SmsRuta r   ON r.tipo_mensaje = p.tipo AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = p.cuenta
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);

DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
