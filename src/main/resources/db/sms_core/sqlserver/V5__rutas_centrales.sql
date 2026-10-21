/* =====================================================================
   V5 - Rutas del ENDPOINT CENTRAL  POST /v1/sms/send  y  /v1/sms/batch
   (SmsRuta.proveedor_id = NULL). Idempotente.
   Requiere: V1 + los seeds de los proveedores usados (V2..V4).

   Cada paso = una cuenta; el motor hace max_intentos (3) en esa cuenta y,
   si fallan todos, pasa al siguiente orden (puede ser otro proveedor).
   Para cambiar una ruta ya creada use el módulo SmsAdmin
   (/v1/sms/admin/rutas) o un UPDATE; este script NO pisa pasos existentes.
   ===================================================================== */

--                            proveedor tipo            orden cuenta
EXEC dbo.SmsRuta_AgregarPaso NULL, 'OTP',          1, 'INALAMBRIA_OTP';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'OTP',          2, 'INALAMBRIA_MASIVO';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'HABEAS_DATA',  1, 'INALAMBRIA_HABEASDATA';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'TEMPLATE',     1, 'INALAMBRIA_TEMPLATE';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'PERSONALIZED', 1, 'INALAMBRIA_PERSONALIZADA';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'NORMAL',       1, 'INALAMBRIA_MASIVO';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'MASIVO',       1, 'INALAMBRIA_MASIVO';
GO

/* Opcional - Go4Clients como respaldo del central (requiere V4). Descomentar:
EXEC dbo.SmsRuta_AgregarPaso NULL, 'OTP',    3, 'GO4CLIENTS_PRINCIPAL';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'NORMAL', 2, 'GO4CLIENTS_PRINCIPAL';
EXEC dbo.SmsRuta_AgregarPaso NULL, 'MASIVO', 2, 'GO4CLIENTS_PRINCIPAL';
*/

/* Si la aplicación ya está corriendo, refrescar la caché de rutas (TTL 60 s):
   POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh                 */
