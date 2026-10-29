-- =============================================================================
-- V13 · Auditoría + soft delete (AuditInfo) en las tablas de configuración
--       CanalProveedor, CanalCuenta, CanalTipoMensaje, CanalRuta, CanalRutaPaso, CanalProveedorServicio
-- -----------------------------------------------------------------------------
-- AuditInfo mapea `eliminado` con @SoftDelete e insertable=false: Hibernate NUNCA
-- escribe la columna y filtra con `eliminado = 0`. Si queda NULL (columna creada
-- por ddl-auto=update sin DEFAULT), las filas quedan invisibles (404 / RUTA_NO_CONFIGURADA).
--
-- Deja en cada tabla:
--   eliminado  TINYINT(1)  NOT NULL DEFAULT 0     (NULL -> 0)
--   usr_crea   INT         NOT NULL DEFAULT 1     (NULL/0 -> 1)
--   usr_mod    INT         NULL
--   created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)  (backfill desde created_at)
--   updated_at DATETIME(6) NULL
-- La aplicación ya no usa `activo` ni `created_at`; si existen en la base se ignoran.
-- EJECUTAR en una base existente ANTES de los scripts de datos (V2-V5, V8-V12) y otra vez al final.
-- Idempotente. Compatible con MySQL 5.7 y 8.0.
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_v13_columna;
DELIMITER $$
-- Ejecuta p_sql solo si la tabla existe y la columna (no) existe según p_debe_existir
CREATE PROCEDURE sp_v13_columna(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64),
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

DROP PROCEDURE IF EXISTS sp_v13_auditoria$$
CREATE PROCEDURE sp_v13_auditoria(IN p_tabla VARCHAR(64))
BEGIN
  -- eliminado
  CALL sp_v13_columna(p_tabla, 'eliminado', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  CALL sp_v13_columna(p_tabla, 'eliminado', 1,
       CONCAT('UPDATE `', p_tabla, '` SET eliminado = 0 WHERE eliminado IS NULL'));
  CALL sp_v13_columna(p_tabla, 'eliminado', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0'));
  -- usr_crea / usr_mod
  CALL sp_v13_columna(p_tabla, 'usr_crea', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v13_columna(p_tabla, 'usr_crea', 1,
       CONCAT('UPDATE `', p_tabla, '` SET usr_crea = 1 WHERE usr_crea IS NULL OR usr_crea = 0'));
  CALL sp_v13_columna(p_tabla, 'usr_crea', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN usr_crea INT NOT NULL DEFAULT 1'));
  CALL sp_v13_columna(p_tabla, 'usr_mod', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN usr_mod INT NULL'));
  -- created_at / updated_at
  CALL sp_v13_columna(p_tabla, 'created_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v13_columna(p_tabla, 'created_at', 1,
       CONCAT('ALTER TABLE `', p_tabla, '` MODIFY COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)'));
  CALL sp_v13_columna(p_tabla, 'updated_at', 0,
       CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN updated_at DATETIME(6) NULL'));
  CALL sp_v13_columna(p_tabla, 'fecha_creacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET created_at = fecha_creacion WHERE created_at IS NULL'));
  CALL sp_v13_columna(p_tabla, 'fecha_modificacion', 1,
       CONCAT('UPDATE `', p_tabla, '` SET updated_at = fecha_modificacion WHERE updated_at IS NULL'));
END$$
DELIMITER ;

CALL sp_v13_auditoria('CanalProveedor');
CALL sp_v13_auditoria('CanalCuenta');
CALL sp_v13_auditoria('CanalTipoMensaje');
CALL sp_v13_auditoria('CanalRuta');
CALL sp_v13_auditoria('CanalRutaPaso');
CALL sp_v13_auditoria('CanalProveedorServicio');

DROP PROCEDURE IF EXISTS sp_v13_auditoria;
DROP PROCEDURE IF EXISTS sp_v13_columna;

-- Verificación: no debe quedar ningún eliminado NULL
SELECT 'CanalProveedor' AS tabla, SUM(eliminado IS NULL) AS nulos, COUNT(*) AS filas FROM CanalProveedor
UNION ALL SELECT 'CanalCuenta',      SUM(eliminado IS NULL), COUNT(*) FROM CanalCuenta
UNION ALL SELECT 'CanalTipoMensaje', SUM(eliminado IS NULL), COUNT(*) FROM CanalTipoMensaje
UNION ALL SELECT 'CanalRuta',        SUM(eliminado IS NULL), COUNT(*) FROM CanalRuta
UNION ALL SELECT 'CanalRutaPaso',    SUM(eliminado IS NULL), COUNT(*) FROM CanalRutaPaso;
