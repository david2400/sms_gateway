/* =====================================================================
   V5 - Rutas del ENDPOINT CENTRAL (MySQL)  POST /v1/sms/send y /v1/sms/batch
   (SmsRuta.proveedor_id = NULL). Idempotente. Requiere V1 + V2.
   Cada paso = una cuenta; el motor hace max_intentos (3) en esa cuenta y
   luego pasa al siguiente orden. NO pisa pasos existentes: para cambiar
   una ruta use SmsAdmin (/v1/sms/admin/rutas) o un UPDATE.
   ===================================================================== */

-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)
 -- Opcional, Go4Clients como respaldo (requiere V4): agregar como UNION ALL en los dos INSERT
 -- (NULL, 'OTP', 3, 'GO4CLIENTS_PRINCIPAL'), (NULL, 'NORMAL', 2, 'GO4CLIENTS_PRINCIPAL'), (NULL, 'MASIVO', 2, 'GO4CLIENTS_PRINCIPAL')

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT NULL AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT NULL, 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT NULL, 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT NULL, 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT NULL, 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, 0, 1
FROM (
SELECT NULL AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT NULL, 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT NULL, 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT NULL, 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT NULL, 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT NULL, 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);


-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
