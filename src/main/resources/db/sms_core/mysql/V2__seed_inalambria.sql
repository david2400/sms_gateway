/* =====================================================================
   V2 - Proveedor INALAMBRIA (MySQL). Idempotente. Requiere V1.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   Contraseñas CIFRADAS (AES-256-GCM) en clave_cifrada. Se descifran al enviar con la llave
   de la variable de entorno SMS_CRYPTO_KEY (nunca en BD ni en el repositorio).
   Si cambia la llave, vuelva a cifrar (ClaveCifradaCli o PUT /v1/sms/admin/cuentas/{id} con 'clave').
   ===================================================================== */

-- 1. Proveedor
INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, usr_crea, created_at)
SELECT 'INALAMBRIA', 'Inalambria', 'https://restaws.inalambria.com', 3000, 10000, 0, 1, NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'INALAMBRIA');

-- 2. Cuentas

INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado, usr_crea)
SELECT (SELECT id_sms_proveedor FROM SmsProveedor WHERE codigo = 'INALAMBRIA'), v.codigo, v.usuario, NULL, v.clave_cifrada, NULL, v.parametros_json, 0, 1
FROM (
  SELECT 'INALAMBRIA_OTP' AS codigo, 'COMFENALCOquindioOTP' AS usuario,
         'v1:MOIs/G3Q9IWGgpOqaYikpP3lactcYqEPCVnrrCtPhKF+0Xc=' AS clave_cifrada,
         '{"type":1,"messagePattern":"0","templateId":0}' AS parametros_json
  UNION ALL SELECT 'INALAMBRIA_MASIVO',        'COMFENALCOquindioMASIVO',
         'v1:zqg+2HKF6qCl2NTp2pQlnWwrrnGXtzu+2Iwj2VWSwKCGE3gVyd8=', '{"type":1,"messagePattern":"0","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_HABEASDATA',    'COMFENALCOquindioCOMUNICADOR',
         'v1:Wpp8p25Jr6cif+9OrAWOraLPR9vfw4pE6oMxGcmjoaldjQft', '{"type":1,"messagePattern":"0","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_TEMPLATE',      'COMFENALCOquindioOTP',
         'v1:g04hQjF0uvZaUSCrcNO4OWJLuIMoJrq5D5KUuMWqehGew8U=', '{"type":1,"messagePattern":"1","templateId":0}'
  UNION ALL SELECT 'INALAMBRIA_PERSONALIZADA', 'COMFENALCOquindioOTP',
         'v1:POKjUCKJbM1z/8VwYNPztUKTleO0Kf85gNEi5cG0Eh18bWs=', '{"type":1,"messagePattern":"1","templateId":0}'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta c WHERE c.codigo = v.codigo);

-- 3. Rutas del endpoint propio  POST /v1/sms/inalambria/send
-- Pasos (proveedor, tipo, orden, cuenta) como tabla derivada: cada INSERT es independiente (sin tablas auxiliares)

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado, usr_crea)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT 'INALAMBRIA', 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT 'INALAMBRIA', 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT 'INALAMBRIA', 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT 'INALAMBRIA', 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado, usr_crea)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, 0, 1
FROM (
SELECT 'INALAMBRIA' AS proveedor, 'OTP' AS tipo, 1 AS orden, 'INALAMBRIA_OTP' AS cuenta
  UNION ALL SELECT 'INALAMBRIA', 'OTP', 2, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'HABEAS_DATA', 1, 'INALAMBRIA_HABEASDATA'
  UNION ALL SELECT 'INALAMBRIA', 'TEMPLATE', 1, 'INALAMBRIA_TEMPLATE'
  UNION ALL SELECT 'INALAMBRIA', 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA'
  UNION ALL SELECT 'INALAMBRIA', 'NORMAL', 1, 'INALAMBRIA_MASIVO'
  UNION ALL SELECT 'INALAMBRIA', 'MASIVO', 1, 'INALAMBRIA_MASIVO'
) p
JOIN SmsTipoMensaje tm ON tm.codigo = CAST(p.tipo AS BINARY)
LEFT JOIN SmsProveedor pr ON pr.codigo = CAST(p.proveedor AS BINARY)
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);

