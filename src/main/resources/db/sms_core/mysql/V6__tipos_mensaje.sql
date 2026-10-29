/* =====================================================================
   V6 - Catálogo de tipos de mensaje (MySQL). Idempotente.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   CanalTipoMensaje.codigo es el valor que viaja en las peticiones de envío
   y referencia CanalRuta.tipo_mensaje_id. Las capacidades (canal_mensaje,
   otp, masivo) se administran aquí por CRUD; no hace falta crear un tipo
   por proveedor. Las filas del gateway llevan canal = 'SMS'.
   ===================================================================== */

CREATE TABLE IF NOT EXISTS CanalTipoMensaje (
  id_tipo_mensaje BIGINT       NOT NULL AUTO_INCREMENT,
  canal           VARCHAR(10)  NOT NULL DEFAULT 'SMS',   -- canal de configuración (SMS | EMAIL)
  codigo          VARCHAR(30)  NOT NULL,                 -- valor de tipo_mensaje en las APIs (OTP, NORMAL, ...)
  descripcion     VARCHAR(255) NULL,
  canal_mensaje   VARCHAR(20)  NOT NULL,                 -- canal del mensaje: SMS | WHATSAPP
  otp             BIT(1)       NULL,                     -- lleva código/PIN de un solo uso
  masivo          BIT(1)       NOT NULL DEFAULT b'0',    -- apto para envíos masivos
  eliminado       BIT(1)       NOT NULL DEFAULT b'0',
  usr_crea        INT          NOT NULL DEFAULT 1,
  usr_mod         INT          NULL,
  created_at      DATETIME(6)  NULL,
  updated_at      DATETIME(6)  NULL,
  CONSTRAINT pk_canal_tipo_mensaje PRIMARY KEY (id_tipo_mensaje),
  CONSTRAINT uq_canal_tipo_mensaje UNIQUE (canal, codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semilla: mismos códigos que usa el enum MessageType
INSERT INTO CanalTipoMensaje (codigo, descripcion, canal_mensaje, otp, masivo, eliminado)
SELECT v.codigo, v.descripcion, v.canal_mensaje, v.otp, v.masivo, b'0'
FROM (
  SELECT 'OTP'          AS codigo, 'Código de verificación de un solo uso'      AS descripcion, 'SMS' AS canal_mensaje, b'1' AS otp, b'0' AS masivo
  UNION ALL SELECT 'HABEAS_DATA',  'Notificación judicial / habeas data',               'SMS', b'0', b'0'
  UNION ALL SELECT 'TEMPLATE',     'Mensaje basado en plantilla del proveedor',         'SMS', b'0', b'0'
  UNION ALL SELECT 'PERSONALIZED', 'Mensaje con contenido personalizado por destino',   'SMS', b'0', b'0'
  UNION ALL SELECT 'NORMAL',       'Mensaje estándar / masivo',                         'SMS', b'0', b'1'
  UNION ALL SELECT 'MASIVO',       'Envío masivo',                                      'SMS', b'0', b'1'
) v
WHERE NOT EXISTS (SELECT 1 FROM CanalTipoMensaje t WHERE t.codigo = v.codigo AND t.canal = 'SMS');
