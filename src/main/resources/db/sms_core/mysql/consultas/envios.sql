/* Consultas del LOG DE ENVÍOS (MySQL, solo lectura). uuid se guarda en BINARY(16). */

-- Cómo se envió cada SMS: proveedor y cuenta final
SELECT e.created_at, BIN_TO_UUID(e.uuid) AS envio_id, e.canal, e.proveedor_solicitado, e.tipo_mensaje,
       e.destino, e.estado, p.codigo AS proveedor_final, c.codigo AS cuenta_final,
       e.http_status_final, e.total_intentos, e.codigo_error, e.detalle_error
FROM SmsEnvio e
LEFT JOIN CanalProveedor p ON p.id_proveedor = e.proveedor_final_id
LEFT JOIN CanalCuenta c    ON c.id_cuenta    = e.cuenta_final_id
ORDER BY e.created_at DESC
LIMIT 200;

-- Detalle de intentos de un envío (reintentos y fallback)
SET @envio = '00000000-0000-0000-0000-000000000000';   -- <- poner el envio_id
SELECT i.paso_orden, i.nro_intento, p.codigo AS proveedor, c.codigo AS cuenta,
       i.exitoso + 0 AS exitoso, i.http_status, i.duracion_ms, i.detalle, i.fecha
FROM SmsEnvioIntento i
JOIN SmsEnvio e     ON e.id_sms_envio = i.envio_id
JOIN CanalProveedor p ON p.id_proveedor = i.proveedor_id
JOIN CanalCuenta c    ON c.id_cuenta = i.cuenta_id
WHERE e.uuid = UUID_TO_BIN(@envio)
ORDER BY i.paso_orden, i.nro_intento;

-- Historial por número
SET @destino = '3157132338';
SELECT e.created_at, e.tipo_mensaje, e.estado, p.codigo AS proveedor, c.codigo AS cuenta, e.total_intentos
FROM SmsEnvio e
LEFT JOIN CanalProveedor p ON p.id_proveedor = e.proveedor_final_id
LEFT JOIN CanalCuenta c    ON c.id_cuenta    = e.cuenta_final_id
WHERE e.destino LIKE CONCAT('%', @destino)
ORDER BY e.created_at DESC;

-- Resumen del día por proveedor/cuenta/estado
SELECT p.codigo AS proveedor, c.codigo AS cuenta, e.estado, COUNT(*) AS total, AVG(e.total_intentos) AS prom_intentos
FROM SmsEnvio e
LEFT JOIN CanalProveedor p ON p.id_proveedor = e.proveedor_final_id
LEFT JOIN CanalCuenta c    ON c.id_cuenta    = e.cuenta_final_id
WHERE e.created_at >= CURDATE()
GROUP BY p.codigo, c.codigo, e.estado
ORDER BY p.codigo, c.codigo, e.estado;
