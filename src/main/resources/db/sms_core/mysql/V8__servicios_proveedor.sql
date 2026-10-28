/* =====================================================================
   V8 - Servicios por proveedor (MySQL). Idempotente. Requiere V1 + V6__tipos_mensaje.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   SmsProveedorServicio = (proveedor, tipo de mensaje) que el proveedor SÍ atiende.
   El motor usa esta tabla para:
     - Responder 422 SERVICIO_NO_SOPORTADO si se pide un tipo a un proveedor que no lo tiene
       (endpoint del proveedor, 'proveedor' preferido o 'cuenta' en el central).
     - Saltar en el fallback las cuentas de proveedores sin el MISMO tipo de envío.
   SmsAdmin: GET/PUT /v1/sms/admin/proveedores/{id}/servicios

   IMPORTANTE: sin filas, el proveedor no atiende NINGÚN tipo. Ejecutar antes de desplegar.
   ===================================================================== */

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS SmsProveedorServicio (
  id_sms_proveedor_servicio BIGINT      NOT NULL AUTO_INCREMENT,
  proveedor_id              BIGINT      NOT NULL,
  tipo_mensaje_id           BIGINT      NOT NULL,
  eliminado                 BIT(1)      NOT NULL DEFAULT b'0',
  usr_crea                  INT         NOT NULL DEFAULT 1,
  usr_mod                   INT         NULL,
  created_at                DATETIME(6) NULL,
  updated_at                DATETIME(6) NULL,
  fecha_creacion            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_sms_proveedor_servicio PRIMARY KEY (id_sms_proveedor_servicio),
  CONSTRAINT uq_sms_proveedor_servicio UNIQUE (proveedor_id, tipo_mensaje_id),
  CONSTRAINT fk_sms_prov_servicio_proveedor FOREIGN KEY (proveedor_id)    REFERENCES SmsProveedor (id_sms_proveedor),
  CONSTRAINT fk_sms_prov_servicio_tipo      FOREIGN KEY (tipo_mensaje_id) REFERENCES SmsTipoMensaje (id_sms_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semilla. Go4Clients NO tiene OTP: un OTP por Go4Clients responde
-- "El proveedor GO4CLIENTS no cuenta con el servicio OTP" (su ruta GO4CLIENTS/OTP de V4 queda sin uso).
INSERT INTO SmsProveedorServicio (proveedor_id, tipo_mensaje_id, eliminado, fecha_creacion)
SELECT p.id_sms_proveedor, t.id_sms_tipo_mensaje, b'0', NOW(6)
FROM (
  SELECT 'INALAMBRIA' AS proveedor, 'OTP' AS tipo
  UNION ALL SELECT 'INALAMBRIA', 'HABEAS_DATA'
  UNION ALL SELECT 'INALAMBRIA', 'TEMPLATE'
  UNION ALL SELECT 'INALAMBRIA', 'PERSONALIZED'
  UNION ALL SELECT 'INALAMBRIA', 'NORMAL'
  UNION ALL SELECT 'INALAMBRIA', 'MASIVO'
  UNION ALL SELECT 'GO4CLIENTS', 'NORMAL'
  UNION ALL SELECT 'GO4CLIENTS', 'MASIVO'
  UNION ALL SELECT 'MOCK', 'OTP'
  UNION ALL SELECT 'MOCK', 'HABEAS_DATA'
  UNION ALL SELECT 'MOCK', 'TEMPLATE'
  UNION ALL SELECT 'MOCK', 'PERSONALIZED'
  UNION ALL SELECT 'MOCK', 'NORMAL'
  UNION ALL SELECT 'MOCK', 'MASIVO'
) v
JOIN SmsProveedor   p ON p.codigo = v.proveedor
JOIN SmsTipoMensaje t ON t.codigo = v.tipo
WHERE NOT EXISTS (SELECT 1 FROM SmsProveedorServicio s
                  WHERE s.proveedor_id = p.id_sms_proveedor AND s.tipo_mensaje_id = t.id_sms_tipo_mensaje);

-- Verificación
SELECT p.codigo AS proveedor, GROUP_CONCAT(t.codigo ORDER BY t.codigo) AS servicios
FROM SmsProveedorServicio s
JOIN SmsProveedor p ON p.id_sms_proveedor = s.proveedor_id
JOIN SmsTipoMensaje t ON t.id_sms_tipo_mensaje = s.tipo_mensaje_id
WHERE s.eliminado = b'0'
GROUP BY p.codigo ORDER BY p.codigo;

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
