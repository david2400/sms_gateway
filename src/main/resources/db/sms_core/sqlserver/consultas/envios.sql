/* Consultas del LOG DE ENVÍOS (solo lectura) */

-- Cómo se envió cada SMS: proveedor y cuenta final
SELECT TOP 200 e.fecha_creacion, e.uuid AS envio_id, e.canal, e.proveedor_solicitado, e.tipo_mensaje,
       e.destino, e.estado, p.codigo AS proveedor_final, c.codigo AS cuenta_final,
       e.http_status_final, e.total_intentos, e.codigo_error, e.detalle_error
FROM dbo.SmsEnvio e
LEFT JOIN dbo.SmsProveedor p ON p.id_sms_proveedor = e.proveedor_final_id
LEFT JOIN dbo.SmsCuenta c    ON c.id_sms_cuenta    = e.cuenta_final_id
ORDER BY e.fecha_creacion DESC;

-- Detalle de intentos de un envío (reintentos y fallback)
DECLARE @envio UNIQUEIDENTIFIER = NULL;   -- <- poner el envio_id
SELECT i.paso_orden, i.nro_intento, p.codigo AS proveedor, c.codigo AS cuenta,
       i.exitoso, i.http_status, i.duracion_ms, i.detalle, i.fecha
FROM dbo.SmsEnvioIntento i
JOIN dbo.SmsEnvio e     ON e.id_sms_envio = i.envio_id
JOIN dbo.SmsProveedor p ON p.id_sms_proveedor = i.proveedor_id
JOIN dbo.SmsCuenta c    ON c.id_sms_cuenta = i.cuenta_id
WHERE e.uuid = @envio
ORDER BY i.paso_orden, i.nro_intento;

-- Historial por número
DECLARE @destino VARCHAR(20) = '3157132338';
SELECT e.fecha_creacion, e.tipo_mensaje, e.estado, p.codigo AS proveedor, c.codigo AS cuenta, e.total_intentos
FROM dbo.SmsEnvio e
LEFT JOIN dbo.SmsProveedor p ON p.id_sms_proveedor = e.proveedor_final_id
LEFT JOIN dbo.SmsCuenta c    ON c.id_sms_cuenta    = e.cuenta_final_id
WHERE e.destino LIKE '%' + @destino
ORDER BY e.fecha_creacion DESC;

-- Resumen del día por proveedor/cuenta/estado
SELECT p.codigo AS proveedor, c.codigo AS cuenta, e.estado, COUNT(*) AS total, AVG(e.total_intentos * 1.0) AS prom_intentos
FROM dbo.SmsEnvio e
LEFT JOIN dbo.SmsProveedor p ON p.id_sms_proveedor = e.proveedor_final_id
LEFT JOIN dbo.SmsCuenta c    ON c.id_sms_cuenta    = e.cuenta_final_id
WHERE e.fecha_creacion >= CAST(SYSDATETIME() AS DATE)
GROUP BY p.codigo, c.codigo, e.estado
ORDER BY p.codigo, c.codigo, e.estado;
