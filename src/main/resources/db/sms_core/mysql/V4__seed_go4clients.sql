/* =====================================================================
   V4 - Proveedor GO4CLIENTS (MySQL). Idempotente. Requiere V1.
   Credenciales en variables de entorno (no en BD):
     CanalCuenta.usuario    = 'env:GO4CLIENTS_API_KEY'  -> api key
     CanalCuenta.clave_cifrada = api secret cifrado (AES-256-GCM, llave SMS_CRYPTO_KEY)
   ===================================================================== */

INSERT INTO CanalProveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, eliminado, created_at)
SELECT 'GO4CLIENTS', 'Go4Clients', 'https://cloud.go4clients.com:8580/api', 3000, 15000, b'0', NOW(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CanalProveedor WHERE codigo = 'GO4CLIENTS');


SET @prov := (SELECT id_canal_proveedor FROM CanalProveedor WHERE codigo = 'GO4CLIENTS' AND canal = 'SMS');

INSERT INTO CanalCuenta (canal_proveedor_id, codigo, usuario, secret_ref, clave_cifrada, remitente, parametros_json, eliminado)
SELECT @prov, 'GO4CLIENTS_PRINCIPAL', 'env:GO4CLIENTS_API_KEY', NULL,
         'v1:y8o3E8dikzW62ZxJyJAaUiOhQWHdvwwdYEb/UqlBdwtNumcQNzAo5Iyvnxc=', 'RANDOM', '{"priority":"HIGH"}', b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CanalCuenta WHERE codigo = 'GO4CLIENTS_PRINCIPAL');

-- Rutas del endpoint propio  POST /v1/sms/go4clients/send
DROP TEMPORARY TABLE IF EXISTS tmp_sms_pasos;
CREATE TEMPORARY TABLE tmp_sms_pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO tmp_sms_pasos VALUES
 ('GO4CLIENTS', 'OTP',    1, 'GO4CLIENTS_PRINCIPAL'),
 ('GO4CLIENTS', 'NORMAL', 1, 'GO4CLIENTS_PRINCIPAL'),
 ('GO4CLIENTS', 'MASIVO', 1, 'GO4CLIENTS_PRINCIPAL');

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

