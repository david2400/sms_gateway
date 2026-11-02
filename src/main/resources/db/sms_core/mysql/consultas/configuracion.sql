/* Consultas de CONFIGURACIÓN (MySQL, solo lectura). Tablas unificadas de AdminCanales de notificacion, canal = 'SMS'. */

-- Rutas efectivas: qué cuenta/proveedor se usa por tipo y en qué orden
SELECT COALESCE(pr.codigo, '(CENTRAL)') AS endpoint, tm.codigo AS tipo_mensaje, rp.orden,
       pc.codigo AS proveedor_paso, c.codigo AS cuenta, rp.max_intentos, rp.espera_ms,
       IF(r.eliminado = 0 AND rp.eliminado = 0 AND c.eliminado = 0 AND pc.eliminado = 0, 'SI', 'NO') AS efectivo
FROM Ruta r
JOIN TipoMensaje tm    ON tm.id_tipo_mensaje = r.tipo_mensaje_id
LEFT JOIN Proveedor pr ON pr.id_proveedor = r.proveedor_id
JOIN RutaPaso rp       ON rp.ruta_id = r.id_ruta
JOIN Cuenta c          ON c.id_cuenta = rp.cuenta_id
JOIN Proveedor pc      ON pc.id_proveedor = c.proveedor_id
WHERE r.canal = 'SMS'
ORDER BY r.proveedor_id IS NOT NULL, pr.codigo, tm.codigo, rp.orden;

-- Cuentas y la variable de entorno que debe existir para cada una
SELECT p.codigo AS proveedor, c.codigo AS cuenta, c.usuario, c.clave_cifrada IS NOT NULL AS tiene_clave_cifrada, c.secret_ref AS variable_entorno_password,
       c.remitente, c.parametros_json, c.eliminado + 0 AS eliminado
FROM Cuenta c
JOIN Proveedor p ON p.id_proveedor = c.proveedor_id
WHERE c.canal = 'SMS'
ORDER BY p.codigo, c.codigo;

-- Tipos sin ruta central (el endpoint central respondería 422)
SELECT t.tipo
FROM (SELECT 'OTP' AS tipo UNION ALL SELECT 'HABEAS_DATA' UNION ALL SELECT 'TEMPLATE'
      UNION ALL SELECT 'PERSONALIZED' UNION ALL SELECT 'NORMAL' UNION ALL SELECT 'MASIVO') t
WHERE NOT EXISTS (SELECT 1 FROM Ruta r JOIN TipoMensaje tm ON tm.id_tipo_mensaje = r.tipo_mensaje_id
                  WHERE r.canal = 'SMS' AND r.proveedor_id IS NULL AND tm.codigo = t.tipo AND r.eliminado = 0);
