/* =====================================================================
   V8 - Servicios por proveedor (MySQL). Idempotente. Requiere V1 + V6__tipos_mensaje.
   Ejecutar como SCRIPT en DBeaver (Alt+X).

   ProveedorServicio = (proveedor, tipo de mensaje) que el proveedor SÍ atiende.
   El motor usa esta tabla para:
     - Responder 422 SERVICIO_NO_SOPORTADO si se pide un tipo a un proveedor que no lo tiene
       (endpoint del proveedor, 'proveedor' preferido o 'cuenta' en el central).
     - Saltar en el fallback las cuentas de proveedores sin el MISMO tipo de envío.
   SmsAdmin: GET/PUT /v1/sms/admin/proveedores/{id}/servicios

   IMPORTANTE: sin filas, el proveedor no atiende NINGÚN tipo. Ejecutar antes de desplegar.
   ===================================================================== */

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ProveedorServicio (
  id_proveedor_servicio BIGINT      NOT NULL AUTO_INCREMENT,
  canal                     VARCHAR(10) NOT NULL DEFAULT 'SMS',
  proveedor_id        BIGINT      NOT NULL,
  tipo_mensaje_id           BIGINT      NOT NULL,
  eliminado                 BIT(1)      NOT NULL DEFAULT b'0',
  usr_crea                  INT         NOT NULL DEFAULT 1,
  usr_mod                   INT         NULL,
  created_at                DATETIME(6) NULL,
  updated_at                DATETIME(6) NULL,
  CONSTRAINT pk_proveedor_servicio PRIMARY KEY (id_proveedor_servicio),
  CONSTRAINT uq_proveedor_servicio UNIQUE (proveedor_id, tipo_mensaje_id),
  CONSTRAINT fk_ps_proveedor FOREIGN KEY (proveedor_id) REFERENCES Proveedor (id_proveedor),
  CONSTRAINT fk_ps_tipo      FOREIGN KEY (tipo_mensaje_id)    REFERENCES TipoMensaje (id_tipo_mensaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semilla. Go4Clients NO tiene OTP: un OTP por Go4Clients responde
-- "El proveedor GO4CLIENTS no cuenta con el servicio OTP" (su ruta GO4CLIENTS/OTP de V4 queda sin uso).
INSERT INTO ProveedorServicio (canal, proveedor_id, tipo_mensaje_id, eliminado, created_at)
SELECT 'SMS', p.id_proveedor, t.id_tipo_mensaje, b'0', NOW(6)
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
JOIN Proveedor   p ON p.codigo = v.proveedor AND p.canal = 'SMS'
JOIN TipoMensaje t ON t.codigo = v.tipo AND t.canal = 'SMS'
WHERE NOT EXISTS (SELECT 1 FROM ProveedorServicio s
                  WHERE s.proveedor_id = p.id_proveedor AND s.tipo_mensaje_id = t.id_tipo_mensaje);

-- Verificación
SELECT p.codigo AS proveedor, GROUP_CONCAT(t.codigo ORDER BY t.codigo) AS servicios
FROM ProveedorServicio s
JOIN Proveedor p ON p.id_proveedor = s.proveedor_id
JOIN TipoMensaje t ON t.id_tipo_mensaje = s.tipo_mensaje_id
WHERE s.eliminado = b'0'
GROUP BY p.codigo ORDER BY p.codigo;

-- Si la aplicación está corriendo: POST /api/sms_gateway/v1/sms/admin/routing-cache/refresh
