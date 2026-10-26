/* =====================================================================
   V7 - sms_ruta.tipo_mensaje -> tipo_mensaje_id (FK a sms_tipo_mensaje).
   Idempotente. Requiere V6 (catálogo poblado). Ejecutar como SCRIPT (Alt+X).

   Pasos: crea tipos faltantes -> agrega columna -> backfill por código ->
   reemplaza el índice único -> agrega FK -> elimina la columna vieja.
   ===================================================================== */

-- 1. Garantiza que todos los códigos usados en sms_ruta existan en el catálogo
--    (solo si la columna vieja todavía existe)
SET @existe = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND column_name = 'tipo_mensaje');
SET @sql = IF(@existe > 0,
  'INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, eliminado)
   SELECT DISTINCT r.tipo_mensaje, CONCAT(''Migrado desde sms_ruta: '', r.tipo_mensaje), ''SMS'', b''0'', b''0'', b''0''
   FROM SmsRuta r
   WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje t WHERE t.codigo = r.tipo_mensaje)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 2. Agrega la columna FK (si no existe)
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND column_name = 'tipo_mensaje_id') = 0,
  'ALTER TABLE SmsRuta ADD COLUMN tipo_mensaje_id BIGINT NULL AFTER proveedor_id',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 3. Backfill por código (solo si la columna vieja sigue existiendo)
SET @sql = IF(@existe > 0,
  'UPDATE SmsRuta r JOIN SmsTipoMensaje t ON t.codigo = r.tipo_mensaje
      SET r.tipo_mensaje_id = t.id_sms_tipo_mensaje WHERE r.tipo_mensaje_id IS NULL',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 4. NOT NULL + FK
SET @sql = IF(@existe > 0,
  'ALTER TABLE SmsRuta MODIFY COLUMN tipo_mensaje_id BIGINT NOT NULL',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta'
                 AND constraint_name = 'fk_sms_ruta_tipo_mensaje') = 0,
  'ALTER TABLE SmsRuta ADD CONSTRAINT fk_sms_ruta_tipo_mensaje
       FOREIGN KEY (tipo_mensaje_id) REFERENCES SmsTipoMensaje (id_sms_tipo_mensaje)',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 5. Índice único (proveedor_key, tipo): lo recrea sobre tipo_mensaje_id
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsruta' AND index_name = 'ux_sms_ruta') > 0,
  'DROP INDEX ux_sms_ruta ON SmsRuta',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = 'CREATE UNIQUE INDEX ux_sms_ruta ON SmsRuta (proveedor_key, tipo_mensaje_id)';
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 6. Elimina la columna vieja (último paso: solo cuando el backfill ya corrió)
SET @sql = IF(@existe > 0,
  'ALTER TABLE SmsRuta DROP COLUMN tipo_mensaje',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
