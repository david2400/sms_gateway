# Scripts SQL — SmsCore

Dos versiones con el mismo contenido, una por motor:

| Carpeta | Motor | Dónde |
|---|---|---|
| `mysql/` | MySQL 8 | perfil `dev` (localhost:3306/notificacion) |
| `sqlserver/` | SQL Server | BD MAESTRA |

> El error `1064 ... near 'EXEC dbo....'` significa que se ejecutó un script de `sqlserver/` en MySQL.

Todos son **idempotentes** (se pueden re-ejecutar) y **no borran datos**.
En DBeaver ejecutar como **script** (Alt+X), no como sentencia (Ctrl+Enter).

| Orden | Script | Contenido |
|---|---|---|
| 1 | `V1__schema.sql` | Tablas `SmsProveedor`, `SmsCuenta`, `SmsRuta`, `SmsRutaPaso`, `SmsEnvio`, `SmsEnvioIntento` e índices (SQL Server: además el helper `dbo.SmsRuta_AgregarPaso`) |
| 2 | `V2__seed_inalambria.sql` | Proveedor INALAMBRIA, 5 cuentas y rutas de `/v1/sms/inalambria/send` |
| 3 | `V3__seed_mock.sql` | Proveedor MOCK (pruebas) — opcional en producción |
| 4 | `V4__seed_go4clients.sql` | Proveedor GO4CLIENTS, cuenta y rutas de `/v1/sms/go4clients/send` |
| 5 | `V5__rutas_centrales.sql` | Rutas del endpoint central `/v1/sms/send` (proveedor_id NULL) |
| — | `consultas/configuracion.sql` | Rutas efectivas, cuentas y variables de entorno requeridas |
| — | `consultas/envios.sql` | Log: cómo se envió cada SMS, intentos, historial, resumen |

## Diferencias MySQL vs SQL Server
- `SmsEnvio.uuid`: `BINARY(16)` en MySQL (leer con `BIN_TO_UUID(uuid)`), `UNIQUEIDENTIFIER` en SQL Server.
- Una sola ruta central por tipo: MySQL permite varios NULL en un UNIQUE, por eso `SmsRuta` tiene la
  columna calculada `proveedor_key = IFNULL(proveedor_id, 0)` con el índice `ux_sms_ruta`. JPA no la usa.
- MySQL no usa procedimientos (evita `DELIMITER` en DBeaver): las rutas se cargan con una tabla temporal.

## Nuevo proveedor
Crear `V6__seed_<proveedor>.sql` en **las dos carpetas**: proveedor → cuentas → rutas.
Para usarlo en el central, agregar sus pasos en un script nuevo (no editar V5 ya ejecutado).

## Credenciales
Nunca en BD. `secret_ref` = **nombre** de la variable de entorno con la contraseña;
`usuario` puede ser `env:NOMBRE_VARIABLE`.

## Notas
- Nombres sincronizados con `Sms*Entity` / `Admin*Entity`. Si cambia una entidad, actualizar ambos `V1`.
- Con `ddl-auto=update` Hibernate puede crear las tablas antes; igual ejecutar `V1` para los índices.
- Las tablas antiguas `sms_proveedor`, `sms_cuenta`, ... (snake_case) ya no las usa la aplicación.
