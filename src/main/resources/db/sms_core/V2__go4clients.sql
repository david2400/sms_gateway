/* =====================================================================
   SmsCore V2 - Proveedor Go4Clients (módulo Go4Clients). Idempotente.
   Ejecutar después de V1__sms_core.sql.

   Credenciales (en variables de entorno, no en BD):
     sms_cuenta.usuario    = 'env:GO4CLIENTS_API_KEY'   -> api key
     sms_cuenta.secret_ref = 'GO4CLIENTS_API_SECRET'    -> api secret

   Go4Clients NO se agrega a las rutas del endpoint CENTRAL: solo queda
   disponible en su endpoint propio (/v1/sms/go4clients/...). Para usarlo como
   respaldo del central, agregue un paso en sms_ruta_paso (ver ejemplo al final).
   ===================================================================== */

IF NOT EXISTS (SELECT 1 FROM dbo.sms_proveedor WHERE codigo = 'GO4CLIENTS')
INSERT INTO dbo.sms_proveedor (codigo, nombre, base_url, connect_timeout_ms, read_timeout_ms, activo)
VALUES ('GO4CLIENTS', 'Go4Clients', 'https://cloud.go4clients.com:8580/api', 3000, 15000, 1);
GO

DECLARE @g4c BIGINT = (SELECT id FROM dbo.sms_proveedor WHERE codigo = 'GO4CLIENTS');
IF NOT EXISTS (SELECT 1 FROM dbo.sms_cuenta WHERE codigo = 'GO4CLIENTS_PRINCIPAL')
INSERT INTO dbo.sms_cuenta (proveedor_id, codigo, usuario, secret_ref, remitente, parametros_json)
VALUES (@g4c, 'GO4CLIENTS_PRINCIPAL', 'env:GO4CLIENTS_API_KEY', 'GO4CLIENTS_API_SECRET', 'RANDOM', '{"priority":"HIGH"}');
GO

IF OBJECT_ID('tempdb..#pasos') IS NOT NULL DROP TABLE #pasos;
CREATE TABLE #pasos (proveedor VARCHAR(30) NULL, tipo VARCHAR(30), orden INT, cuenta VARCHAR(50));
INSERT INTO #pasos VALUES
 ('GO4CLIENTS', 'OTP',    1, 'GO4CLIENTS_PRINCIPAL'),
 ('GO4CLIENTS', 'NORMAL', 1, 'GO4CLIENTS_PRINCIPAL'),
 ('GO4CLIENTS', 'MASIVO', 1, 'GO4CLIENTS_PRINCIPAL');

INSERT INTO dbo.sms_ruta (proveedor_id, tipo_mensaje, activo)
SELECT DISTINCT pr.id, p.tipo, 1
FROM #pasos p
JOIN dbo.sms_proveedor pr ON pr.codigo = p.proveedor
WHERE NOT EXISTS (SELECT 1 FROM dbo.sms_ruta r WHERE r.tipo_mensaje = p.tipo AND r.proveedor_id = pr.id);

INSERT INTO dbo.sms_ruta_paso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, activo)
SELECT r.id, p.orden, c.id, 3, 500, 1
FROM #pasos p
JOIN dbo.sms_proveedor pr ON pr.codigo = p.proveedor
JOIN dbo.sms_ruta r ON r.tipo_mensaje = p.tipo AND r.proveedor_id = pr.id
JOIN dbo.sms_cuenta c ON c.codigo = p.cuenta
WHERE NOT EXISTS (SELECT 1 FROM dbo.sms_ruta_paso x WHERE x.ruta_id = r.id AND x.orden = p.orden);

DROP TABLE #pasos;
GO

/* Ejemplo: usar Go4Clients como 3er respaldo del OTP central (después de INALAMBRIA_OTP y INALAMBRIA_MASIVO)
INSERT INTO dbo.sms_ruta_paso (ruta_id, orden, cuenta_id, max_intentos, espera_ms, activo)
SELECT r.id, 3, c.id, 3, 500, 1
FROM dbo.sms_ruta r, dbo.sms_cuenta c
WHERE r.proveedor_id IS NULL AND r.tipo_mensaje = 'OTP' AND c.codigo = 'GO4CLIENTS_PRINCIPAL';
-- luego: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
*/
