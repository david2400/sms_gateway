-- =============================================================================
-- V14 · AuditInfo en SmsEnvio, SmsEnvioIntento, SmsPlantilla y SmsPlantillaVariable
-- -----------------------------------------------------------------------------
-- Estas entidades ahora extienden AuditInfo (@SoftDelete en `eliminado`,
-- usr_crea/usr_mod, created_at/updated_at). Requiere V13.
--
--   1. eliminado TINYINT(1) NOT NULL DEFAULT 0 y usr_crea INT NOT NULL DEFAULT 1
--      (si `eliminado` queda NULL, Hibernate no vuelve a encontrar el envío recién
--      creado y no registra intentos ni el resultado final).
--   2. created_at / updated_at si no existen, y backfill desde fecha_creacion /
--      fecha_modificacion.
--   3. SmsEnvio.fecha_creacion con DEFAULT CURRENT_TIMESTAMP(6): la aplicación ya
--      no la escribe (usa created_at) y sin default el INSERT falla.
--   4. Índices de consulta de envíos sobre created_at.
-- Idempotente. Compatible con MySQL 5.7 y 8.0.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_v14_columna;
DELIMITER $$
-- Ejecuta p_sql solo si la tabla existe y la columna (no) existe según p_debe_existir
CREATE PROCEDURE sp_v14_columna(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64),
                                IN p_debe_existir TINYINT, IN p_sql TEXT)
BEGIN
  DECLARE v_tabla INT DEFAULT 0;
  DECLARE v_col INT DEFAULT 0;
  SELECT COUNT(*) INTO v_tabla FROM information_schema.tables
   WHERE table_schema = DATABASE() AND table_name = p_tabla;
  SELECT COUNT(*) INTO v_col FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_tabla AND column_name = p_columna;
  IF v_tabla > 0 AND ((p_debe_existir = 1 AND v_col > 0) OR (p_debe_existir = 0 AND v_col = 0)) THEN
    SET @s = p_sql;
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$

DROP PROCEDURE IF EXISTS sp_v14_auditoria$$
CREATE PROCEDURE sp_v14_auditoria(IN p_tabla VARCHAR(64))
BEGIN
  -- eliminado
  CALL sp_v14_columna(p_tabla, 'eliminado', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  CALL sp_v14_columna(p_tabla, 'eliminado', 1,
       CONCAT('UPDATE `', p_tabla, '` SET eliminado = 0 WHERE eliminado IS NULL'));
  CALL sp_v14_columna(p_tabla, 'eliminado', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  -- usr_crea / usr_mod
  CALL sp_v14_columna(p_tabla, 'usr_crea', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v14_columna(p_tabla, 'usr_crea', 1,
       CONCAT('UPDATE `', p_tabla, '` SET usr_crea = 1 WHERE usr_crea IS NULL OR usr_crea = 0'));
  CALL sp_v14_columna(p_tabla, 'usr_crea', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v14_columna(p_tabla, 'usr_mod', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_mod INT NULL'));
  -- created_at / updated_at
  CALL sp_v14_columna(p_tabla, 'created_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v14_columna(p_tabla, 'created_at', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v14_columna(p_tabla, 'updated_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN updated_at DATETIME(6) NULL'));
  CALL sp_v14_columna(p_tabla, 'fecha_creacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET created_at = fecha_creacion WHERE created_at IS NULL'));
  CALL sp_v14_columna(p_tabla, 'fecha_modificacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET updated_at = fecha_modificacion WHERE updated_at IS NULL'));
END$$
DELIMITER ;

CALL sp_v14_auditoria('SmsEnvio');
CALL sp_v14_auditoria('SmsEnvioIntento');
CALL sp_v14_auditoria('SmsPlantilla');
CALL sp_v14_auditoria('SmsPlantillaVariable');

-- SmsEnvioIntento: created_at desde su columna `fecha`
CALL sp_v14_columna('SmsEnvioIntento', 'fecha', 1,
     'UPDATE SmsEnvioIntento SET created_at = fecha WHERE created_at IS NULL');

-- SmsEnvio.fecha_creacion ya no la escribe la aplicación
CALL sp_v14_columna('SmsEnvio', 'fecha_creacion', 1,
     'ALTER TABLE SmsEnvio MODIFY COLUMN fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');

DROP PROCEDURE IF EXISTS sp_v14_auditoria;
DROP PROCEDURE IF EXISTS sp_v14_columna;

-- Índices de consulta (/v1/sms/envios) sobre created_at
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'SmsEnvio' AND index_name = 'ix_sms_envio_destino_created') = 0,
  'CREATE INDEX ix_sms_envio_destino_created ON SmsEnvio (destino, created_at)', 'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'SmsEnvio' AND index_name = 'ix_sms_envio_estado_created') = 0,
  'CREATE INDEX ix_sms_envio_estado_created ON SmsEnvio (estado, created_at)', 'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Verificación (solo tablas existentes): eliminado debe quedar NOT NULL DEFAULT 0
SELECT table_name AS tabla, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND column_name = 'eliminado'
  AND table_name IN ('SmsEnvio', 'SmsEnvioIntento', 'SmsPlantilla', 'SmsPlantillaVariable');
