/* =====================================================================
   V6 - Claves CIFRADAS para cuentas existentes (MySQL). Idempotente.
   Para bases que ya tenían las cuentas con secret_ref (V2/V4 anteriores).

   - Agrega la columna clave_cifrada si falta y deja secret_ref opcional.
   - Guarda la contraseña cifrada (AES-256-GCM). Se descifra al enviar con la
     llave de la variable de entorno SMS_CRYPTO_KEY (no está en BD ni en git).
   - secret_ref NO se borra: queda como respaldo si la clave no se puede descifrar.
     Para usar solo la clave cifrada, ejecute el UPDATE opcional del final.
   ===================================================================== */

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smscuenta' AND column_name = 'clave_cifrada') = 0,
  'ALTER TABLE SmsCuenta ADD COLUMN clave_cifrada VARCHAR(500) NULL AFTER secret_ref',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE SmsCuenta MODIFY secret_ref VARCHAR(100) NULL;

UPDATE SmsCuenta SET clave_cifrada = 'v1:MOIs/G3Q9IWGgpOqaYikpP3lactcYqEPCVnrrCtPhKF+0Xc='
WHERE codigo = 'INALAMBRIA_OTP';
UPDATE SmsCuenta SET clave_cifrada = 'v1:zqg+2HKF6qCl2NTp2pQlnWwrrnGXtzu+2Iwj2VWSwKCGE3gVyd8='
WHERE codigo = 'INALAMBRIA_MASIVO';
UPDATE SmsCuenta SET clave_cifrada = 'v1:Wpp8p25Jr6cif+9OrAWOraLPR9vfw4pE6oMxGcmjoaldjQft'
WHERE codigo = 'INALAMBRIA_HABEASDATA';
UPDATE SmsCuenta SET clave_cifrada = 'v1:g04hQjF0uvZaUSCrcNO4OWJLuIMoJrq5D5KUuMWqehGew8U='
WHERE codigo = 'INALAMBRIA_TEMPLATE';
UPDATE SmsCuenta SET clave_cifrada = 'v1:POKjUCKJbM1z/8VwYNPztUKTleO0Kf85gNEi5cG0Eh18bWs='
WHERE codigo = 'INALAMBRIA_PERSONALIZADA';
UPDATE SmsCuenta SET clave_cifrada = 'v1:y8o3E8dikzW62ZxJyJAaUiOhQWHdvwwdYEb/UqlBdwtNumcQNzAo5Iyvnxc='
WHERE codigo = 'GO4CLIENTS_PRINCIPAL';

-- Verificación (no muestra la clave, solo si está cargada)
SELECT codigo, secret_ref, clave_cifrada IS NOT NULL AS tiene_clave_cifrada, LENGTH(clave_cifrada) AS largo
FROM SmsCuenta WHERE codigo IN ('INALAMBRIA_OTP', 'INALAMBRIA_MASIVO', 'INALAMBRIA_HABEASDATA', 'INALAMBRIA_TEMPLATE', 'INALAMBRIA_PERSONALIZADA', 'GO4CLIENTS_PRINCIPAL') ORDER BY codigo;

/* Opcional: usar SOLO la clave cifrada (sin respaldo por variable de entorno)
UPDATE SmsCuenta SET secret_ref = NULL WHERE clave_cifrada IS NOT NULL;
*/

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
