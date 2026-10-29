/* =====================================================================
   V9 - Plantillas SMS con variables (MySQL). Idempotente. Requiere V1 + V6__tipos_mensaje.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   SmsPlantilla.contenido usa variables {{nombre}}; cada una se declara en SmsPlantillaVariable
   con tipo (TEXTO, ENTERO, DECIMAL, MONEDA, FECHA, HORA, URL), obligatoriedad, longitud, patrón
   y valor por defecto. El request de envío trae 'plantilla' + 'variables' y se valida en modo
   estricto (faltantes, desconocidas, tipo, longitud, formato) ANTES de enviar.
   SmsAdmin: /v1/sms/admin/plantillas   Clientes: GET /v1/sms/plantillas/{codigo}, POST .../previsualizar
   ===================================================================== */

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS SmsPlantilla (
  id_sms_plantilla   BIGINT        NOT NULL AUTO_INCREMENT,
  codigo             VARCHAR(50)   NOT NULL,
  nombre             VARCHAR(100)  NOT NULL,
  descripcion        VARCHAR(255)  NULL,
  tipo_mensaje_id    BIGINT        NULL,                 -- tipo por defecto (NULL = lo define el request)
  contenido          VARCHAR(1600) NOT NULL,
  version            INT           NOT NULL DEFAULT 1,  -- sube en cada modificación
  eliminado          BIT(1)        NOT NULL DEFAULT b'0',
  usr_crea           INT           NOT NULL DEFAULT 1,
  usr_mod            INT           NULL,
  created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)   NULL,
  CONSTRAINT pk_sms_plantilla PRIMARY KEY (id_sms_plantilla),
  CONSTRAINT uq_sms_plantilla_codigo UNIQUE (codigo),
  CONSTRAINT fk_sms_plantilla_tipo FOREIGN KEY (tipo_mensaje_id) REFERENCES CanalTipoMensaje (id_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsPlantillaVariable (
  id_sms_plantilla_variable BIGINT       NOT NULL AUTO_INCREMENT,
  plantilla_id              BIGINT       NOT NULL,
  nombre                    VARCHAR(50)  NOT NULL,
  tipo_dato                 VARCHAR(20)  NOT NULL DEFAULT 'TEXTO',
  obligatoria               BIT(1)       NOT NULL DEFAULT b'1',
  longitud_max              INT          NULL,
  patron                    VARCHAR(200) NULL,       -- regex sobre el valor completo
  valor_defecto             VARCHAR(200) NULL,
  descripcion               VARCHAR(255) NULL,
  orden                     INT          NOT NULL DEFAULT 0,
  eliminado                 BIT(1)       NOT NULL DEFAULT b'0',
  usr_crea                  INT          NOT NULL DEFAULT 1,
  usr_mod                   INT          NULL,
  created_at                DATETIME(6)  NULL,
  updated_at                DATETIME(6)  NULL,
  CONSTRAINT pk_sms_plantilla_variable PRIMARY KEY (id_sms_plantilla_variable),
  CONSTRAINT uq_sms_plantilla_variable UNIQUE (plantilla_id, nombre),
  CONSTRAINT fk_sms_plantilla_variable_plantilla FOREIGN KEY (plantilla_id) REFERENCES SmsPlantilla (id_sms_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Log: plantilla con la que se generó cada envío
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND LOWER(table_name) = 'smsenvio' AND column_name = 'plantilla_codigo') = 0,
  'ALTER TABLE SmsEnvio ADD COLUMN plantilla_codigo VARCHAR(50) NULL AFTER mensaje',
  'DO 0');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

/* ----------------------------- EJEMPLOS ----------------------------- */

INSERT INTO SmsPlantilla (codigo, nombre, descripcion, tipo_mensaje_id, contenido, version, eliminado, created_at)
SELECT v.codigo, v.nombre, v.descripcion, t.id_tipo_mensaje, v.contenido, 1, b'0', NOW(6)
FROM (
  SELECT 'RECORDATORIO_CITA' AS codigo, 'Recordatorio de cita' AS nombre, 'Ejemplo: recordatorio con fecha, hora y sede' AS descripcion,
         'NORMAL' AS tipo, 'Hola {{nombre}}, le recordamos su cita el {{fecha}} a las {{hora}} en {{sede}}. Comfenalco Quindio' AS contenido
  UNION ALL
  SELECT 'OTP_VERIFICACION', 'Código de verificación', 'Ejemplo: OTP con vigencia',
         'OTP', 'Comfenalco: su codigo de verificacion es {{codigo}}. Vence en {{minutos}} minutos. No lo comparta.'
) v
LEFT JOIN CanalTipoMensaje t ON t.codigo = v.tipo
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
