/* =====================================================================
   V4 - Proveedor GO4CLIENTS (MySQL). Idempotente. Requiere V1.
   Credenciales en variables de entorno (no en BD):
     SmsCuenta.usuario    = 'env:GO4CLIENTS_API_KEY'  -> api key
     SmsCuenta.clave_cifrada = api secret cifrado (AES-256-GCM, llave SMS_CRYPTO_KEY)
   ===================================================================== */

INSERT INTO SmsProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, fecha_creacion)
SELECT 'GO4CLIENTS', 'Go4Clients', 'https://cloud.go4clients.com:8580/api', 3000, 15000, b'0', NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsProveedor WHERE codigo = 'GO4CLIENTS');


INSERT INTO SmsCuenta (proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado)
SELECT @prov, 'GO4CLIENTS_PRINCIPAL', 'env:GO4CLIENTS_API_KEY', NULL,
         'v1:y8o3E8dikzW62ZxJyJAaUiOhQWHdvwwdYEb/UqlBdwtNumcQNzAo5Iyvnxc=', 'RANDOM', '{"priority":"HIGH"}', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM SmsCuenta WHERE codigo = 'GO4CLIENTS_PRINCIPAL');

-- Rutas del endpoint propio  POST /v1/sms/go4clients/send
DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('GO4CLIENTS', 'OTP',    1, 'GO4CLIENTS_PRINCIPAL'),
 ('GO4CLIENTS', 'NORMAL', 1, 'GO4CLIENTS_PRINCIPAL'),
 ('GO4CLIENTS', 'MASIVO', 1, 'GO4CLIENTS_PRINCIPAL');

INSERT INTO SmsRuta (proveedor_id, tipo_mensaje_id, eliminado)
SELECT DISTINCT pr.id_sms_proveedor, tm.id_sms_tipo_mensaje, b'0'
FROM tmp_sms_pasos p
JOIN SmsTipoMensaje tm ON tm.codigo = p.tipo
LEFT JOIN SmsProveedor pr ON pr.codigo = p.proveedor
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRuta r
                  WHERE r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor);

INSERT INTO SmsRutaPaso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, eliminado)
SELECT r.id_sms_ruta, p.orden, c.id_sms_cuenta, 3, 500, b'0'
FROM tmp_sms_pasos p
JOIN SmsTipoMensaje tm ON tm.codigo = p.tipo
LEFT JOIN SmsProveedor pr ON pr.codigo = p.proveedor
JOIN SmsRuta r   ON r.tipo_mensaje_id = tm.id_sms_tipo_mensaje AND r.proveedor_id <=> pr.id_sms_proveedor
JOIN SmsCuenta c ON c.codigo = CAST(p.cuenta AS BINARY)
WHERE (p.proveedor IS NULL OR pr.id_sms_proveedor IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM SmsRutaPaso x WHERE x.ruta_id = r.id_sms_ruta AND x.orden = p.orden);

