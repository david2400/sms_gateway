/* =====================================================================
   V2 - Proveedor INALAMBRIA (MySQL). Idempotente. Requiere V1.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   Contraseñas CIFRADAS (AES-256-GCM) en clave_cifrada. Se descifran al enviar con la llave
   de la variable de entorno SMS_CRYPTO_KEY (nunca en BD ni en el repositorio).
   Si cambia la llave, vuelva a cifrar (ClaveCifradaCli o PUT /v1/sms/admin/cuentas/{id} con 'clave').
   ===================================================================== */

-- 1. Proveedor
INSERT INTO CanalProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, created_at)
SELECT 'INALAMBRIA', 'Inalambria', 'https://restaws.inalambria.com', 3000, 10000, b'0', NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CanalProveedor WHERE codigo = 'INALAMBRIA');

-- 2. Cuentas
SET @prov := (SELECT id_canal_proveedor FROM CanalProveedor WHERE codigo = 'INALAMBRIA' AND canal = 'SMS');

INSERT INTO CanalCuenta (canal_proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado)
SELECT @prov, v.codigo, v.usuario, NULL, v.clave_cifrada, NULL, v.parametros_json, b'0'
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
WHERE NOT EXISTS (SELECT 1 FROM CanalCuenta c WHERE c.codigo = v.codigo);

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

