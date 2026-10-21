/* =====================================================================
   V6 - Catálogo de tipos de mensaje (MySQL). Idempotente.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   sms_tipo_mensaje.codigo es el valor que viaja en sms_ruta.tipo_mensaje
   y en las peticiones de envío. Las capacidades (canal, otp, masivo) se
   administran aquí por CRUD; no hace falta crear un tipo por proveedor.
   ===================================================================== */

CREATE TABLE IF NOT EXISTS SmsTipoMensaje (
  id_sms_tipo_mensaje BIGINT       NOT NULL AUTO_INCREMENT,
  codigo              VARCHAR(30)  NOT NULL,   -- valor de sms_ruta.tipo_mensaje (OTP, NORMAL, ...)
  descripcion         VARCHAR(255) NULL,
  canal               VARCHAR(20)  NOT NULL,   -- SMS | WHATSAPP
  otp                 BIT(1)       NOT NULL DEFAULT b'0',  -- lleva código/PIN de un solo uso
  masivo              BIT(1)       NOT NULL DEFAULT b'0',  -- apto para envíos masivos
  activo              BIT(1)       NOT NULL DEFAULT b'1',
  CONSTRAINT pk_sms_tipo_mensaje PRIMARY KEY (id_sms_tipo_mensaje),
  CONSTRAINT uq_sms_tipo_mensaje_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semilla: mismos códigos que ya usan sms_ruta y el enum MessageType
INSERT INTO SmsTipoMensaje (codigo, descripcion, canal, otp, masivo, activo)
SELECT v.codigo, v.descripcion, v.canal, v.otp, v.masivo, b'1'
FROM (
  SELECT 'OTP'          AS codigo, 'Código de verificación de un solo uso'      AS descripcion, 'SMS' AS canal, b'1' AS otp, b'0' AS masivo
  UNION ALL SELECT 'HABEAS_DATA',  'Notificación judicial / habeas data',               'SMS', b'0', b'0'
  UNION ALL SELECT 'TEMPLATE',     'Mensaje basado en plantilla del proveedor',         'SMS', b'0', b'0'
  UNION ALL SELECT 'PERSONALIZED', 'Mensaje con contenido personalizado por destino',   'SMS', b'0', b'0'
  UNION ALL SELECT 'NORMAL',       'Mensaje estándar / masivo',                         'SMS', b'0', b'1'
  UNION ALL SELECT 'MASIVO',       'Envío masivo',                                      'SMS', b'0', b'1'
) v
WHERE NOT EXISTS (SELECT 1 FROM SmsTipoMensaje t WHERE t.codigo = v.codigo);
