# Scripts SQL — SmsCore

> **Configuración centralizada en notificacion (tablas `Canal*`).**
> Proveedores, cuentas, tipos de mensaje, servicios y rutas ya no están en `SmsProveedor, SmsCuenta, SmsTipoMensaje, SmsProveedorServicio, SmsRuta, SmsRutaPaso`: el gateway lee las
> tablas unificadas `CanalProveedor`, `CanalCuenta`, `CanalTipoMensaje`, `CanalProveedorServicio`, `CanalRuta` y
> `CanalRutaPaso` (filas con `canal = 'SMS'`), que crea y administra **notificacion**
> (`/api/notificacion/v1/admin/sms/...`).
>
> | Base | Ejecutar (en orden) |
> |---|---|
> | Nueva | `notificacion/.../db/admin_canales_esquema.sql` → `admin_canales_datos.sql` → `mysql_ejecutar_todo.sql` de cada gateway |
> | Con las tablas antiguas | `notificacion/.../db/migracion_admin_canales.sql` (copia los datos, deja respaldo `bak_*` y re-apunta las FK) |
>
> `mysql_ejecutar_todo.sql` y `mysql_datos_prueba.sql` de esta carpeta ya solo tienen las tablas propias del gateway
> (envíos y plantillas). Los scripts versionados `mysql/V*.sql` (y `sqlserver/`) son **históricos**: crean las tablas
> antiguas; no usarlos en bases nuevas.


Dos versiones con el mismo contenido, una por motor:

| Carpeta | Motor | Dónde |
|---|---|---|
| `mysql/` | MySQL 8 | perfil `dev` (localhost:3306/notificacion) |
| `sqlserver/` | SQL Server | BD MAESTRA |

> El error `1064 ... near 'EXEC dbo....'` significa que se ejecutó un script de `sqlserver/` en MySQL.

Todos son **idempotentes** (se pueden re-ejecutar) y **no borran datos**.
**Atajos (en esta carpeta):**
- `mysql_ejecutar_todo.sql`: esquema + datos (V1→V14) en orden. Base vacía o existente.
- `mysql_datos_prueba.sql`: **solo los datos** (tipos, proveedores INALAMBRIA/GO4CLIENTS/MOCK, cuentas, servicios,
  rutas, plantillas de ejemplo) sobre tablas ya creadas (por V1 o por Hibernate `ddl-auto`).

> **Soft delete (AuditInfo):** la aplicación usa `eliminado` (`0` = activo, `1` = inactivo), `usr_crea/usr_mod` y
> `created_at/updated_at`. Los scripts ya no usan `activo` ni `fecha_creacion`.
> Una fila con `eliminado` NULL es invisible para la app: por eso V13/V14 corren antes y después de los datos.
En DBeaver ejecutar como **script** (Alt+X), no como sentencia (Ctrl+Enter).

| Orden | Script | Contenido |
|---|---|---|
| 1 | `V1__schema.sql` | Tablas `SmsProveedor`, `SmsCuenta`, `SmsRuta`, `SmsRutaPaso`, `SmsEnvio`, `SmsEnvioIntento` e índices (SQL Server: además el helper `dbo.SmsRuta_AgregarPaso`) |
| 2 | `V6__tipos_mensaje.sql` | Catálogo `SmsTipoMensaje` (**antes de V2–V5**: sus rutas hacen JOIN con el catálogo; si está vacío no insertan nada y no dan error) — solo `mysql/` |
| 3 | `V7__ruta_tipo_mensaje_fk.sql` | `SmsRuta.tipo_mensaje` → `tipo_mensaje_id` (FK) — solo `mysql/` |
| 3a | `V13__auditoria_soft_delete.sql` | **Base existente**: columnas de `AuditInfo` (`eliminado`, `usr_crea`, `usr_mod`, `created_at`, `updated_at`) con DEFAULT en tablas de configuración; `eliminado` NULL → 0 — solo `mysql/` |
| 3b | `V14__auditoria_envios_plantillas.sql` | **Base existente**: lo mismo en `SmsEnvio`, `SmsEnvioIntento`, `SmsPlantilla*`; `SmsEnvio.fecha_creacion` con DEFAULT (la app usa `created_at`) — solo `mysql/` |
| 4 | `V2__seed_inalambria.sql` | Proveedor INALAMBRIA, 5 cuentas y rutas de `/v1/sms/inalambria/send` |
| 5 | `V3__seed_mock.sql` | Proveedor MOCK (pruebas) — opcional en producción |
| 6 | `V4__seed_go4clients.sql` | Proveedor GO4CLIENTS, cuenta y rutas de `/v1/sms/go4clients/send` |
| 7 | `V5__rutas_centrales.sql` | Rutas del endpoint central `/v1/sms/send` (proveedor_id NULL) |
| 8 | `V6__claves_cifradas.sql` | Solo para BD existentes: agrega `clave_cifrada` y carga las contraseñas cifradas (UPDATE) |
| 9 | `V8__servicios_proveedor.sql` | `SmsProveedorServicio`: tipos de mensaje que atiende cada proveedor. **Obligatorio**: sin filas el proveedor no atiende nada |
| 10 | `V9__plantillas.sql` | `SmsPlantilla`, `SmsPlantillaVariable`, columna `SmsEnvio.plantilla_codigo` y 2 plantillas de ejemplo |
| 11 | `V10__inalambria_masivo.sql` | `massiveUrl` en la cuenta `INALAMBRIA_MASIVO` y rutas MASIVO |
| 12 | `V11__inalambria_whatsapp.sql` | Tipo `WHATSAPP`, cuenta `INALAMBRIA_WHATSAPP` (configurar usuario, clave y remitente), servicio y rutas WhatsApp |
| 13 | `V12__inalambria_bidireccional.sql` | Tipo `BIDIRECCIONAL`, cuenta `INALAMBRIA_BIDIRECCIONAL` (configurar usuario, clave y código corto en `remitente`), servicio y rutas |
| 14 | `V13` + `V14` otra vez | Cubre las tablas creadas por V8/V9 en bases antiguas (idempotentes) |
| — | `consultas/configuracion.sql` | Rutas efectivas, cuentas y variables de entorno requeridas |
| — | `consultas/envios.sql` | Log: cómo se envió cada SMS, intentos, historial, resumen |

> Hay dos scripts `V6__*` (versión duplicada). Si se adopta Flyway hay que renumerarlos.
> SQL Server: falta portar `V6__tipos_mensaje` / `V7` (`V8` crea el catálogo si no existe); `V8`–`V10` sin ejecutar en servidor real.

## Servicios por proveedor (V8)
- El motor rechaza con **422 `SERVICIO_NO_SOPORTADO`** un envío de un tipo que el proveedor no tiene
  (endpoint del proveedor, `proveedor` preferido o `cuenta` en el central) y **salta en el fallback** las cuentas de
  proveedores sin ese mismo tipo.
- Administración: `GET/PUT /v1/sms/admin/proveedores/{id}/servicios` (`{"tipos_mensaje": ["NORMAL","MASIVO"]}`).
  Quitar un tipo que usan rutas con cuentas del proveedor → 409. Agregar a una ruta una cuenta de un proveedor sin el
  servicio → 400.

## Plantillas (V9) — módulo `GestionPlatillas`
- Todo el código de plantillas vive en `modules/GestionPlatillas` (modelo, parser, validación, administración, consulta y caché). SmsCore solo define el contrato `SmsTemplatePort`, que este módulo implementa.
- Contenido con `{{variable}}`; cada variable con `tipo_dato` (TEXTO, ENTERO, DECIMAL, MONEDA, FECHA, HORA, URL),
  `obligatoria`, `longitud_max`, `patron` (regex) y `valor_defecto`.
- Envío: `"plantilla": "RECORDATORIO_CITA", "variables": {...}` en `/v1/sms/send`, `/v1/sms/batch` (variables globales +
  por destinatario) y en los endpoints de cada proveedor. Validación estricta: faltantes, desconocidas, tipo, longitud,
  formato → 400 con la lista de errores (en masivos no se envía nada si un destinatario falla).
- `GET /v1/sms/plantillas/{codigo}` (variables requeridas), `POST /v1/sms/plantillas/{codigo}/previsualizar`.
- Administración: `/v1/sms/admin/plantillas` (la versión sube en cada cambio; desactivar con `PATCH /{id}/estado`).
- Tildes (á, í, ó, ú) pasan el SMS a UCS-2: 70 caracteres por parte en vez de 160. La previsualización lo muestra.

## Masivo Inalambria (V10)
- `POST /v1/sms/inalambria/batch` y `POST /v1/sms/batch` usan `POST https://rest.inalambria.com/mtmessage/massive`
  (Basic con la cuenta) en lotes de `sms.inalambria.massive.chunk-size` (500). Éxito = HTTP 2xx y `Status = 0`.
- Parámetros por cuenta (`parametros_json`): `massiveUrl`, `massiveChunkSize`, `xAuth` (header `x-auth`), `notifyUrl`.

## Diferencias MySQL vs SQL Server
- `SmsEnvio.uuid`: `BINARY(16)` en MySQL (leer con `BIN_TO_UUID(uuid)`), `UNIQUEIDENTIFIER` en SQL Server.
- Una sola ruta central por tipo: MySQL permite varios NULL en un UNIQUE, por eso `SmsRuta` tiene la
  columna calculada `proveedor_key = IFNULL(proveedor_id, 0)` con el índice `ux_sms_ruta`. JPA no la usa.
- MySQL no usa procedimientos (evita `DELIMITER` en DBeaver): las rutas se cargan con una tabla temporal.

## Nuevo proveedor
Crear `V13__seed_<proveedor>.sql` (siguiente versión libre) en **las dos carpetas**: proveedor → cuentas → servicios (`SmsProveedorServicio`) → rutas.
Para usarlo en el central, agregar sus pasos en un script nuevo (no editar V5 ya ejecutado).

## Credenciales
- `clave_cifrada`: contraseña cifrada con AES-256-GCM (`v1:...`). Se descifra al enviar con la llave de
  **`SMS_CRYPTO_KEY`** (variable de entorno, 32 bytes Base64). La llave nunca va en BD ni en git.
- `secret_ref` (opcional): nombre de una variable de entorno con la contraseña; se usa si no hay `clave_cifrada`
  o si no se puede descifrar.
- Para cifrar una clave nueva: `PUT /v1/sms/admin/cuentas/{id}` con `"clave"` (la cifra el servidor), o
  `ClaveCifradaCli cifrar` (clase `SmsCore/shared/crypto/ClaveCifradaCli`, requiere `SMS_CRYPTO_KEY`).
- Si cambia la llave hay que volver a cifrar todas las claves.

## Notas
- Nombres sincronizados con `Sms*Entity` / `Admin*Entity`. Si cambia una entidad, actualizar ambos `V1`.
- Con `ddl-auto=update` Hibernate puede crear las tablas antes; igual ejecutar `V1` para los índices.
- Las tablas antiguas `sms_proveedor`, `sms_cuenta`, ... (snake_case) ya no las usa la aplicación.

## WhatsApp Inalambria (V11)
- `POST /v1/whatsapp/send` (central) y `POST /v1/whatsapp/inalambria/send`: `template_id` (plantilla aprobada por Meta),
  `parameters` (en orden, ninguno vacío), opcionales `source`, `datos_adicionales`, `cuenta`, `proveedor`.
- API: `POST https://multichannel.inalambria.com/MultiChannelInput/SendMessageMetaWhatsApp` (Basic). Éxito = HTTP 2xx,
  `httpStatusCode` 200 y `transactionNumber` ≠ 0. 400/401 son permanentes.
- Cuenta `INALAMBRIA_WHATSAPP`: `usuario`, clave (cifrada o `secret_ref`) y `remitente` = número de origen
  (o `parametros_json.source`). `parametros_json.whatsappUrl` sobrescribe la URL.
- Los endpoints de SMS rechazan tipos de canal WhatsApp y viceversa; el fallback solo usa proveedores con el servicio `WHATSAPP`.
- `parametros_json.canal = WHATSAPP` marca la cuenta: Inalambria rechaza (error permanente, pasa a la siguiente cuenta) usar una cuenta WhatsApp para SMS o una cuenta SMS para WhatsApp.

## SMS bidireccional Inalambria (V12)
- `POST /v1/sms/send` (central) o `/v1/sms/inalambria/send` con `"tipo_mensaje": "BIDIRECCIONAL"`. Código corto: `sender`
  (central) / `short_code` (Inalambria), si no `SmsCuenta.remitente`, si no `parametros_json.shortCode`.
- API: `POST https://rest.inalambria.com/momessage` (Basic, `x-auth` opcional). Éxito = HTTP 2xx y `Status` ≠ false.
- Va siempre uno a uno (la API masiva no se usa para este tipo). Las respuestas del usuario (MO) no están implementadas.
