-- =============================================================================
-- V13 · Auditoría + soft delete (AuditInfo) en las tablas que usa SmsAdmin
-- -----------------------------------------------------------------------------
-- AuditInfo mapea `eliminado` con @SoftDelete e insertable=false: Hibernate
-- NUNCA escribe la columna y filtra con `eliminado = 0`. Si la columna queda
-- NULL (la creó ddl-auto=update sin DEFAULT), las filas existentes y TODAS las
-- que se creen desde /admin quedan invisibles (404 "No existe ...").
--
-- Este script:
--   1. Crea `eliminado` / `usr_crea` si no existen.
--   2. Pone en 0 los `eliminado` NULL y en 1 los `usr_crea` NULL/0.
--   3. Deja `eliminado TINYINT(1) NOT NULL DEFAULT 0` y `usr_crea INT NOT NULL DEFAULT 1`.
-- Idempotente. Compatible con MySQL 5.7 y 8.0.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_v13_auditoria;
DELIMITER $$
CREATE PROCEDURE sp_v13_auditoria(IN p_tabla VARCHAR(64))
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables
             WHERE table_schema = DATABASE() AND table_name = p_tabla) THEN

    -- eliminado ---------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = p_tabla
                     AND column_name = 'eliminado') THEN
      SET @s = CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0');
      PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    ELSE
      SET @s = CONCAT('UPDATE `', p_tabla, '` SET eliminado = 0 WHERE eliminado IS NULL');
      PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
      SET @s = CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0');
      PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;

    -- usr_crea ----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = p_tabla
                     AND column_name = 'usr_crea') THEN
      SET @s = CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_crea INT NOT NULL DEFAULT 1');
      PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    ELSE
      SET @s = CONCAT('UPDATE `', p_tabla, '` SET usr_crea = 1 WHERE usr_crea IS NULL OR usr_crea = 0');
      PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
      SET @s = CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN usr_crea INT NOT NULL DEFAULT 1');
      PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;

  END IF;
END$$
DELIMITER ;

CALL sp_v13_auditoria('SmsProveedor');
CALL sp_v13_auditoria('SmsCuenta');
CALL sp_v13_auditoria('SmsRuta');
CALL sp_v13_auditoria('SmsRutaPaso');
CALL sp_v13_auditoria('SmsTipoMensaje');
CALL sp_v13_auditoria('SmsProveedorServicio');

DROP PROCEDURE IF EXISTS sp_v13_auditoria;

-- Verificación: no debe quedar ningún eliminado NULL
SELECT 'SmsProveedor' AS tabla, SUM(eliminado IS NULL) AS nulos, COUNT(*) AS filas FROM SmsProveedor
UNION ALL SELECT 'SmsCuenta',      SUM(eliminado IS NULL), COUNT(*) FROM SmsCuenta
UNION ALL SELECT 'SmsRuta',        SUM(eliminado IS NULL), COUNT(*) FROM SmsRuta
UNION ALL SELECT 'SmsRutaPaso',    SUM(eliminado IS NULL), COUNT(*) FROM SmsRutaPaso
UNION ALL SELECT 'SmsTipoMensaje', SUM(eliminado IS NULL), COUNT(*) FROM SmsTipoMensaje
UNION ALL SELECT 'SmsProveedorServicio', SUM(eliminado IS NULL), COUNT(*) FROM SmsProveedorServicio;
