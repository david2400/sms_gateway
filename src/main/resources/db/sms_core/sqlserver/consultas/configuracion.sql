/* Consultas de CONFIGURACIÓN (solo lectura) */

-- Rutas efectivas: qué cuenta/proveedor se usa por tipo y en qué orden
SELECT COALESCE(pr.codigo, '(CENTRAL)') AS endpoint, r.tipo_mensaje, rp.orden,
       pc.codigo AS proveedor_paso, c.codigo AS cuenta, rp.max_intentos, rp.espera_ms,
       CASE WHEN r.activo = 1 AND rp.activo = 1 AND c.activo = 1 AND pc.activo = 1 THEN 'SI' ELSE 'NO' END AS efectivo
FROM dbo.SmsRuta r
LEFT JOIN dbo.SmsProveedor pr ON pr.id_sms_proveedor = r.proveedor_id
JOIN dbo.SmsRutaPaso rp       ON rp.ruta_id = r.id_sms_ruta
JOIN dbo.SmsCuenta c          ON c.id_sms_cuenta = rp.cuenta_id
JOIN dbo.SmsProveedor pc      ON pc.id_sms_proveedor = c.proveedor_id
ORDER BY CASE WHEN r.proveedor_id IS NULL THEN 0 ELSE 1 END, pr.codigo, r.tipo_mensaje, rp.orden;

-- Cuentas y la variable de entorno que debe existir para cada una
SELECT p.codigo AS proveedor, c.codigo AS cuenta, c.usuario, CASE WHEN c.clave_cifrada IS NOT NULL THEN 'SI' ELSE 'NO' END AS tiene_clave_cifrada, c.secret_ref AS variable_entorno_password,
       c.remitente, c.parametros_json, c.activo
FROM dbo.SmsCuenta c
JOIN dbo.SmsProveedor p ON p.id_sms_proveedor = c.proveedor_id
ORDER BY p.codigo, c.codigo;

-- Tipos sin ruta central (el endpoint central respondería 422)
SELECT t.tipo
FROM (VALUES ('OTP'),('HABEAS_DATA'),('TEMPLATE'),('PERSONALIZED'),('NORMAL'),('MASIVO')) t(tipo)
WHERE NOT EXISTS (SELECT 1 FROM dbo.SmsRuta r WHERE r.proveedor_id IS NULL AND r.tipo_mensaje = t.tipo AND r.activo = 1);
