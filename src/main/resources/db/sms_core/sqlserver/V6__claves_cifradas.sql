/* =====================================================================
   V6 - Claves CIFRADAS para cuentas existentes (SQL Server). Idempotente.
   Para bases que ya tenían las cuentas con secret_ref (V2/V4 anteriores).

   - Agrega la columna clave_cifrada si falta y deja secret_ref opcional.
   - Guarda la contraseña cifrada (AES-256-GCM). Se descifra al enviar con la
     llave de la variable de entorno SMS_CRYPTO_KEY (no está en BD ni en git).
   - secret_ref NO se borra: queda como respaldo si la clave no se puede descifrar.
     Para usar solo la clave cifrada, ejecute el UPDATE opcional del final.
   ===================================================================== */

IF COL_LENGTH('dbo.SmsCuenta', 'clave_cifrada') IS NULL
  ALTER TABLE dbo.SmsCuenta ADD clave_cifrada VARCHAR(500) NULL;
GO
ALTER TABLE dbo.SmsCuenta ALTER COLUMN secret_ref VARCHAR(100) NULL;
GO

UPDATE dbo.SmsCuenta SET clave_cifrada = 'v1:MOIs/G3Q9IWGgpOqaYikpP3lactcYqEPCVnrrCtPhKF+0Xc='
WHERE codigo = 'INALAMBRIA_OTP';
UPDATE dbo.SmsCuenta SET clave_cifrada = 'v1:zqg+2HKF6qCl2NTp2pQlnWwrrnGXtzu+2Iwj2VWSwKCGE3gVyd8='
WHERE codigo = 'INALAMBRIA_MASIVO';
UPDATE dbo.SmsCuenta SET clave_cifrada = 'v1:Wpp8p25Jr6cif+9OrAWOraLPR9vfw4pE6oMxGcmjoaldjQft'
WHERE codigo = 'INALAMBRIA_HABEASDATA';
UPDATE dbo.SmsCuenta SET clave_cifrada = 'v1:g04hQjF0uvZaUSCrcNO4OWJLuIMoJrq5D5KUuMWqehGew8U='
WHERE codigo = 'INALAMBRIA_TEMPLATE';
UPDATE dbo.SmsCuenta SET clave_cifrada = 'v1:POKjUCKJbM1z/8VwYNPztUKTleO0Kf85gNEi5cG0Eh18bWs='
WHERE codigo = 'INALAMBRIA_PERSONALIZADA';
UPDATE dbo.SmsCuenta SET clave_cifrada = 'v1:y8o3E8dikzW62ZxJyJAaUiOhQWHdvwwdYEb/UqlBdwtNumcQNzAo5Iyvnxc='
WHERE codigo = 'GO4CLIENTS_PRINCIPAL';
GO

-- Verificación (no muestra la clave, solo si está cargada)
SELECT codigo, secret_ref, CASE WHEN clave_cifrada IS NOT NULL THEN 'SI' ELSE 'NO' END AS tiene_clave_cifrada, LEN(clave_cifrada) AS largo
FROM dbo.SmsCuenta WHERE codigo IN ('INALAMBRIA_OTP', 'INALAMBRIA_MASIVO', 'INALAMBRIA_HABEASDATA', 'INALAMBRIA_TEMPLATE', 'INALAMBRIA_PERSONALIZADA', 'GO4CLIENTS_PRINCIPAL') ORDER BY codigo;

/* Opcional: usar SOLO la clave cifrada (sin respaldo por variable de entorno)
UPDATE dbo.SmsCuenta SET secret_ref = NULL WHERE clave_cifrada IS NOT NULL;
*/

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
