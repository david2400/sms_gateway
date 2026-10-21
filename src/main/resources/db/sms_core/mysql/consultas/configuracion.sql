/* Consultas de CONFIGURACIÓN (MySQL, solo lectura) */

-- Rutas efectivas: qué cuenta/proveedor se usa por tipo y en qué orden
SELECT COALESCE(pr.codigo, '(CENTRAL)') AS endpoint, tm.codigo AS tipo_mensaje, rp.orden,
       pc.codigo AS proveedor_paso, c.codigo AS cuenta, rp.max_intentos, rp.espera_ms,
       IF(r.activo = 1 AND rp.activo = 1 AND c.activo = 1 AND pc.activo = 1, 'SI', 'NO') AS efectivo
FROM SmsRuta r
JOIN SmsTipoMensaje tm    ON tm.id_sms_tipo_mensaje = r.tipo_mensaje_id
LEFT JOIN SmsProveedor pr ON pr.id_sms_proveedor = r.proveedor_id
JOIN SmsRutaPaso rp       ON rp.ruta_id = r.id_sms_ruta
JOIN SmsCuenta c          ON c.id_sms_cuenta = rp.cuenta_id
JOIN SmsProveedor pc      ON pc.id_sms_proveedor = c.proveedor_id
ORDER BY r.proveedor_id IS NOT NULL, pr.codigo, tm.codigo, rp.orden;

-- Cuentas y la variable de entorno que debe existir para cada una
SELECT p.codigo AS proveedor, c.codigo AS cuenta, c.usuario, c.clave_cifrada IS NOT NULL AS tiene_clave_cifrada, c.secret_ref AS variable_entorno_password,
       c.remitente, c.parametros_json, c.activo + 0 AS activo
FROM SmsCuenta c
JOIN SmsProveedor p ON p.id_sms_proveedor = c.proveedor_id
ORDER BY p.codigo, c.codigo;

-- Tipos sin ruta central (el endpoint central respondería 422)
SELECT t.tipo
FROM (SELECT 'OTP' AS tipo UNION ALL SELECT 'HABEAS_DATA' UNION ALL SELECT 'TEMPLATE'
      UNION ALL SELECT 'PERSONALIZED' UNION ALL SELECT 'NORMAL' UNION ALL SELECT 'MASIVO') t
WHERE NOT EXISTS (SELECT 1 FROM SmsRuta r JOIN SmsTipoMensaje tm ON tm.id_sms_tipo_mensaje = r.tipo_mensaje_id
                  WHERE r.proveedor_id IS NULL AND tm.codigo = t.tipo AND r.activo = 1);
