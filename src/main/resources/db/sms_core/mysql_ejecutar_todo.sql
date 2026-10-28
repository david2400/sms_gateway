/* =====================================================================
   sms_gateway (SmsCore + GestionPlatillas) - MySQL 8 / MariaDB, base "notificacion"
   ---------------------------------------------------------------------
   Tablas PROPIAS del gateway: log de envíos (SmsEnvio, SmsEnvioIntento) y plantillas
   (SmsPlantilla, SmsPlantillaVariable) + 2 plantillas de ejemplo.

   La CONFIGURACIÓN (proveedores, cuentas, tipos de mensaje, servicios, rutas) ya no está aquí: son las tablas
   unificadas Canal* (filas con canal = 'SMS') que crea y administra notificacion:
     notificacion/src/main/resources/db/admin_canales_esquema.sql      (esquema)
     notificacion/src/main/resources/db/admin_canales_datos.sql        (datos iniciales SMS y EMAIL)
     notificacion/src/main/resources/db/migracion_admin_canales.sql    (base con las tablas antiguas Sms*)
   EJECUTAR ESOS PRIMERO: las FK de este script apuntan a CanalProveedor, CanalCuenta y CanalTipoMensaje.

   Los scripts V1..V14 de mysql/ son históricos (crean las tablas antiguas Sms*); no usarlos en bases nuevas.
   Idempotente. En DBeaver ejecutar como SCRIPT (Alt+X).
   ===================================================================== */

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS SmsEnvio (
  id_sms_envio bigint(20) NOT NULL AUTO_INCREMENT,
  uuid binary(16) NOT NULL,
  idempotency_key varchar(100) DEFAULT NULL,
  canal varchar(20) NOT NULL,
  proveedor_solicitado varchar(30) DEFAULT NULL,
  tipo_mensaje varchar(30) NOT NULL,
  destino varchar(20) NOT NULL,
  mensaje varchar(1600) NOT NULL,
  plantilla_codigo varchar(50) DEFAULT NULL,
  estado varchar(20) NOT NULL,
  proveedor_final_id bigint(20) DEFAULT NULL,
  cuenta_final_id bigint(20) DEFAULT NULL,
  http_status_final int(11) DEFAULT NULL,
  total_intentos int(11) NOT NULL DEFAULT 0,
  codigo_error varchar(50) DEFAULT NULL,
  detalle_error varchar(1000) DEFAULT NULL,
  fecha_finalizacion datetime(6) DEFAULT NULL,
  eliminado tinyint(1) NOT NULL DEFAULT 0,
  usr_crea int(11) NOT NULL DEFAULT 1,
  usr_mod int(11) DEFAULT NULL,
  created_at datetime(6) DEFAULT current_timestamp(6),
  updated_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (id_sms_envio),
  UNIQUE KEY uq_sms_envio_uuid (uuid),
  UNIQUE KEY ux_sms_envio_idem (idempotency_key),
  KEY fk_sms_envio_proveedor (proveedor_final_id),
  KEY fk_sms_envio_cuenta (cuenta_final_id),
  KEY ix_sms_envio_destino_created (destino,created_at),
  KEY ix_sms_envio_estado_created (estado,created_at),
  CONSTRAINT fk_sms_envio_cuenta FOREIGN KEY (cuenta_final_id) REFERENCES CanalCuenta (id_cuenta),
  CONSTRAINT fk_sms_envio_proveedor FOREIGN KEY (proveedor_final_id) REFERENCES CanalProveedor (id_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsEnvioIntento (
  id_sms_envio_intento bigint(20) NOT NULL AUTO_INCREMENT,
  envio_id bigint(20) NOT NULL,
  paso_orden int(11) NOT NULL,
  nro_intento int(11) NOT NULL,
  proveedor_id bigint(20) NOT NULL,
  cuenta_id bigint(20) NOT NULL,
  exitoso bit(1) NOT NULL,
  http_status int(11) DEFAULT NULL,
  detalle varchar(1000) DEFAULT NULL,
  duracion_ms int(11) NOT NULL,
  fecha datetime(6) NOT NULL,
  eliminado tinyint(1) NOT NULL DEFAULT 0,
  usr_crea int(11) NOT NULL DEFAULT 1,
  usr_mod int(11) DEFAULT NULL,
  created_at datetime(6) DEFAULT current_timestamp(6),
  updated_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (id_sms_envio_intento),
  KEY fk_sms_intento_proveedor (proveedor_id),
  KEY fk_sms_intento_cuenta (cuenta_id),
  KEY ix_sms_intento_envio (envio_id),
  CONSTRAINT fk_sms_intento_cuenta FOREIGN KEY (cuenta_id) REFERENCES CanalCuenta (id_cuenta),
  CONSTRAINT fk_sms_intento_envio FOREIGN KEY (envio_id) REFERENCES SmsEnvio (id_sms_envio),
  CONSTRAINT fk_sms_intento_proveedor FOREIGN KEY (proveedor_id) REFERENCES CanalProveedor (id_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsPlantilla (
  id_sms_plantilla bigint(20) NOT NULL AUTO_INCREMENT,
  codigo varchar(50) NOT NULL,
  nombre varchar(100) NOT NULL,
  descripcion varchar(255) DEFAULT NULL,
  tipo_mensaje_id bigint(20) DEFAULT NULL,
  contenido varchar(1600) NOT NULL,
  version int(11) NOT NULL DEFAULT 1,
  eliminado tinyint(1) NOT NULL DEFAULT 0,
  usr_crea int(11) NOT NULL DEFAULT 1,
  usr_mod int(11) DEFAULT NULL,
  created_at datetime(6) DEFAULT current_timestamp(6),
  updated_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (id_sms_plantilla),
  UNIQUE KEY uq_sms_plantilla_codigo (codigo),
  KEY fk_sms_plantilla_tipo (tipo_mensaje_id),
  CONSTRAINT fk_sms_plantilla_tipo FOREIGN KEY (tipo_mensaje_id) REFERENCES CanalTipoMensaje (id_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS SmsPlantillaVariable (
  id_sms_plantilla_variable bigint(20) NOT NULL AUTO_INCREMENT,
  plantilla_id bigint(20) NOT NULL,
  nombre varchar(50) NOT NULL,
  tipo_dato varchar(20) NOT NULL DEFAULT 'TEXTO',
  obligatoria bit(1) NOT NULL DEFAULT b'1',
  longitud_max int(11) DEFAULT NULL,
  patron varchar(200) DEFAULT NULL,
  valor_defecto varchar(200) DEFAULT NULL,
  descripcion varchar(255) DEFAULT NULL,
  orden int(11) NOT NULL DEFAULT 0,
  eliminado tinyint(1) NOT NULL DEFAULT 0,
  usr_crea int(11) NOT NULL DEFAULT 1,
  usr_mod int(11) DEFAULT NULL,
  created_at datetime(6) DEFAULT current_timestamp(6),
  updated_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (id_sms_plantilla_variable),
  UNIQUE KEY uq_sms_plantilla_variable (plantilla_id,nombre),
  CONSTRAINT fk_sms_plantilla_variable_plantilla FOREIGN KEY (plantilla_id) REFERENCES SmsPlantilla (id_sms_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* ----------------------------- PLANTILLAS DE EJEMPLO ----------------------------- */

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
