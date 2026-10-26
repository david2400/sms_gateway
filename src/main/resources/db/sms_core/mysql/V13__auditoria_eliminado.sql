/* =====================================================================
   V13 - Auditoría: activo -> eliminado + columnas de AuditInfo.
   Idempotente. Ejecutar como SCRIPT (Alt+X).

   Por cada tabla:
     1. Agrega eliminado / usr_crea / usr_mod / created_at / updated_at
     2. eliminado = NOT activo (si la columna vieja existe)
     3. Elimina la columna activo

   Las tablas pueden no existir todas según el nivel de la base;
   cada bloque verifica existencia antes de tocar nada.
   ===================================================================== */

DROP PROCEDURE IF EXISTS migra_auditoria;
DELIMITER $$
CREATE PROCEDURE migra_auditoria(tabla VARCHAR(64))
BEGIN
  SET @existe_tabla = (SELECT COUNT(*) FROM information_schema.tables
                       WHERE table_schema = DATABASE() AND table_name = tabla);
  SET @existe_activo = (SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = 'activo');

  -- 1. Columnas de auditoría
  SET @sql = IF(@existe_tabla > 0 AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = 'eliminado') = 0,
    CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN eliminado BIT(1) NOT NULL DEFAULT b''0'''), 'DO 0');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

  SET @sql = IF(@existe_tabla > 0 AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = 'usr_crea') = 0,
    CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN usr_crea INT NOT NULL DEFAULT 1'), 'DO 0');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

  SET @sql = IF(@existe_tabla > 0 AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = 'usr_mod') = 0,
    CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN usr_mod INT NULL'), 'DO 0');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

  SET @sql = IF(@existe_tabla > 0 AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = 'created_at') = 0,
    CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN created_at DATETIME(6) NULL'), 'DO 0');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

  SET @sql = IF(@existe_tabla > 0 AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = 'updated_at') = 0,
    CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN updated_at DATETIME(6) NULL'), 'DO 0');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

  -- 2. Migrar datos: eliminado = NOT activo
  SET @sql = IF(@existe_activo > 0,
    CONCAT('UPDATE ', tabla, ' SET eliminado = NOT activo'), 'DO 0');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

  -- 3. Eliminar la columna vieja
  SET @sql = IF(@existe_activo > 0,
    CONCAT('ALTER TABLE ', tabla, ' DROP COLUMN activo'), 'DO 0');
  PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
END$$
DELIMITER ;

CALL migra_auditoria('SmsProveedor');
CALL migra_auditoria('SmsCuenta');
CALL migra_auditoria('SmsTipoMensaje');
CALL migra_auditoria('SmsRuta');
CALL migra_auditoria('SmsRutaPaso');
CALL migra_auditoria('SmsProveedorServicio');
CALL migra_auditoria('SmsPlantilla');
CALL migra_auditoria('SmsPlantillaVariable');
CALL migra_auditoria('SmsEnvio');
CALL migra_auditoria('SmsEnvioIntento');

DROP PROCEDURE IF EXISTS migra_auditoria;

-- Verificación: no debe quedar ninguna columna 'activo'
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = DATABASE() AND column_name IN ('activo', 'eliminado')
ORDER BY table_name;
