/* =====================================================================
   sms_gateway - DATOS DE PRUEBA (MySQL 8 / MariaDB)
   ---------------------------------------------------------------------
   Solo las 2 plantillas de ejemplo (RECORDATORIO_CITA, OTP_VERIFICACION). Requiere las tablas (mysql_ejecutar_todo.sql
   o Hibernate ddl-auto).
   Tipos de mensaje, proveedores, cuentas, servicios y rutas viven en notificacion (tablas Canal*):
     notificacion/src/main/resources/db/admin_canales_datos.sql
   Idempotente. En DBeaver ejecutar como SCRIPT (Alt+X).
   ===================================================================== */

SET NAMES utf8mb4;

INSERT INTO SmsPlantilla (codigo, nombre, descripcion, tipo_mensaje_id, contenido, version, eliminado, usr_crea, created_at)
SELECT v.codigo, v.nombre, v.descripcion, t.id_tipo_mensaje, v.contenido, 1, 0, 1, NOW(6)
FROM (
  SELECT 'RECORDATORIO_CITA' AS codigo, 'Recordatorio de cita' AS nombre, 'Ejemplo: recordatorio con fecha, hora y sede' AS descripcion,
         'NORMAL' AS tipo, 'Hola {{nombre}}, le recordamos su cita el {{fecha}} a las {{hora}} en {{sede}}. Comfenalco Quindio' AS contenido
  UNION ALL
  SELECT 'OTP_VERIFICACION', 'Código de verificación', 'Ejemplo: OTP con vigencia',
         'OTP', 'Comfenalco: su codigo de verificacion es {{codigo}}. Vence en {{minutos}} minutos. No lo comparta.'
) v
LEFT JOIN CanalTipoMensaje t ON t.canal = 'SMS' AND t.codigo = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM SmsPlantilla p WHERE p.codigo = v.codigo);

INSERT INTO SmsPlantillaVariable (plantilla_id, nombre, tipo_dato, obligatoria, longitud_max, patron, valor_defecto, descripcion, orden, eliminado, usr_crea, created_at)
SELECT p.id_sms_plantilla, v.nombre, v.tipo_dato, v.obligatoria, v.longitud_max, v.patron, v.valor_defecto, v.descripcion, v.orden, 0, 1, NOW(6)
FROM (
  SELECT 'RECORDATORIO_CITA' AS plantilla, 'nombre' AS nombre, 'TEXTO' AS tipo_dato, b'1' AS obligatoria, 30 AS longitud_max,
         NULL AS patron, NULL AS valor_defecto, 'Primer nombre del afiliado' AS descripcion, 0 AS orden
  UNION ALL SELECT 'RECORDATORIO_CITA', 'fecha', 'FECHA', b'1', NULL, NULL, NULL, 'yyyy-MM-dd o dd/MM/yyyy', 1
  UNION ALL SELECT 'RECORDATORIO_CITA', 'hora',  'HORA',  b'1', NULL, NULL, NULL, 'HH:mm (24 h)', 2
  UNION ALL SELECT 'RECORDATORIO_CITA', 'sede',  'TEXTO', b'0', 40,   NULL, 'Sede principal', 'Sede de atención', 3
  UNION ALL SELECT 'OTP_VERIFICACION',  'codigo',  'TEXTO',  b'1', 8,  '[0-9]{4,8}', NULL, 'PIN de 4 a 8 dígitos', 0
  UNION ALL SELECT 'OTP_VERIFICACION',  'minutos', 'ENTERO', b'0', 3,  NULL, '5', 'Vigencia en minutos', 1
) v
JOIN SmsPlantilla p ON p.codigo = v.plantilla
WHERE NOT EXISTS (SELECT 1 FROM SmsPlantillaVariable x WHERE x.plantilla_id = p.id_sms_plantilla AND x.nombre = v.nombre);

-- Verificación
SELECT p.codigo, p.version, t.codigo AS tipo, COUNT(v.id_sms_plantilla_variable) AS variables, p.contenido
FROM SmsPlantilla p
LEFT JOIN CanalTipoMensaje t ON t.id_tipo_mensaje = p.tipo_mensaje_id
LEFT JOIN SmsPlantillaVariable v ON v.plantilla_id = p.id_sms_plantilla
GROUP BY p.id_sms_plantilla, p.codigo, p.version, t.codigo, p.contenido ORDER BY p.codigo;
