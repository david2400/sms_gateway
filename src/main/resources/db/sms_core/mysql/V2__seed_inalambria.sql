/* =====================================================================
   V2 - Proveedor INALAMBRIA (MySQL). Idempotente. Requiere V1.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   Contraseñas en variables de entorno (secret_ref = nombre de la variable):
     INALAMBRIA_OTP_PASSWORD, INALAMBRIA_MASIVO_PASSWORD,
     INALAMBRIA_HABEASDATA_PASSWORD, INALAMBRIA_TEMPLATE_PASSWORD,
     INALAMBRIA_PERSONALIZADA_PASSWORD
   ===================================================================== */

-- 1. Proveedor
INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo, fecha_creacion)
SELECT 'INALAMBRIA', 'Inalambria', 'https://restaws.inalambria.com', 3000, 10000, b'1', NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'INALAMBRIA');

-- 2. Cuentas
SET @prov = (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'INALAMBRIA');

INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json, activo)
SELECT @prov, v.codigo, v.usuario, v.secret_ref, NULL, v.parametros_json, b'1'
FROM (
  SELECT 'INALAMBRIA_OTP' AS codigo, 'COMFENALCOquindioOTP' AS usuario, 'INALAMBRIA_OTP_PASSWORD' AS secret_ref,
         '{"type":1,"messagePattern":"0","templateId":0}' AS parametros_json
  UNION ALL SELECT 'INALAMBRIA_MASIVO',        'COMFENALCOquindioMASIVO',      'INALAMBRIA_MASIVO_PASSWORD',        '{"type":1,"messagePattern":"0","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_HABEASDATA',    'COMFENALCOquindioCOMUNICADOR', 'INALAMBRIA_HABEASDATA_PASSWORD',    '{"type":1,"messagePattern":"0","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_TEMPLATE',      'COMFENALCOquindioOTP',         'INALAMBRIA_TEMPLATE_PASSWORD',      '{"type":1,"messagePattern":"1","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_PERSONALIZADA', 'COMFENALCOquindioOTP',         'INALAMBRIA_PERSONALIZADA_PASSWORD', '{"type":1,"messagePattern":"1","templateId":0}'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta c WHERE c.codigo = v.codigo);

-- 3. Rutas del endpoint propio  POST /v1/sms/inalambria/send
DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('INALAMBRIA', 'OTP',          1, 'INALAMBRIA_OTP'),
 ('INALAMBRIA', 'OTP',          2, 'INALAMBRIA_MASIVO'),
 ('INALAMBRIA', 'HABEAS_DATA',  1, 'INALAMBRIA_HABEASDATA'),
 ('INALAMBRIA', 'TEMPLATE',     1, 'INALAMBRIA_TEMPLATE'),
 ('INALAMBRIA', 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'),
 ('INALAMBRIA', 'NORMAL',       1, 'INALAMBRIA_MASIVO'),
 ('INALAMBRIA', 'MASIVO',       1, 'INALAMBRIA_MASIVO');

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
