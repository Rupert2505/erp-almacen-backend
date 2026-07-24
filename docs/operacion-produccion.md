# Operacion Productiva Backend

Guia minima para ejecutar el backend del ERP Almacen en perfil `prod` y ajustar el soporte multiusuario.

## Perfil Productivo

El backend debe ejecutarse con:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

En produccion no se debe usar `ddl-auto=update`. El perfil `prod` usa:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

Esto obliga a que la estructura de base de datos exista y coincida con las entidades JPA. Los cambios de esquema deben hacerse solo con migraciones Flyway versionadas.

## Variables Obligatorias

| Variable | Uso |
| --- | --- |
| `PORT` | Puerto HTTP del backend. Default: `8080`. |
| `DB_URL` | URL JDBC de PostgreSQL. |
| `DB_USERNAME` | Usuario de base de datos. |
| `DB_PASSWORD` | Password de base de datos. |
| `DB_SCHEMA` | Schema de PostgreSQL. Default: `erp_almacen`. |
| `JWT_SECRET` | Clave secreta JWT. Debe ser larga y privada. |
| `CORS_ALLOWED_ORIGINS` | Origenes permitidos para el frontend. |

Ejemplo local de validacion productiva:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:PORT="8080"
$env:DB_URL="jdbc:postgresql://localhost:5432/erp_almacen"
$env:DB_USERNAME="erp_user"
$env:DB_PASSWORD="cambiar_en_servidor"
$env:DB_SCHEMA="erp_almacen"
$env:JWT_SECRET="cambiar-por-una-clave-larga-y-privada"
$env:CORS_ALLOWED_ORIGINS="https://erp.midominio.com"
```

## Pool de Conexiones

El backend usa HikariCP. En produccion los parametros son configurables:

| Variable | Default | Recomendacion inicial |
| --- | ---: | --- |
| `DB_POOL_NAME` | `erp-almacen-prod-pool` | Nombre identificable por ambiente. |
| `DB_POOL_MAX_SIZE` | `20` | Iniciar con 10-20; subir solo con medicion. |
| `DB_POOL_MIN_IDLE` | `5` | Mantener menor que `DB_POOL_MAX_SIZE`. |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | `30000` | 30s evita esperas indefinidas por conexiones. |
| `DB_POOL_VALIDATION_TIMEOUT_MS` | `5000` | 5s para validar conexiones. |
| `DB_POOL_IDLE_TIMEOUT_MS` | `600000` | 10 min para cerrar conexiones inactivas. |
| `DB_POOL_MAX_LIFETIME_MS` | `1800000` | 30 min; debe ser menor al timeout del servidor/proxy. |
| `DB_POOL_LEAK_DETECTION_THRESHOLD_MS` | `0` | Activar temporalmente en diagnostico, por ejemplo `60000`. |

Regla practica:

- Si hay pocos usuarios concurrentes, mantener `DB_POOL_MAX_SIZE` entre 10 y 20.
- Si el pool se agota, revisar primero consultas lentas, transacciones largas y bloqueos antes de aumentar el maximo.
- No configurar `DB_POOL_MIN_IDLE` igual al maximo salvo que exista una razon operativa clara.
- El maximo del pool debe ser menor que el limite de conexiones disponible en PostgreSQL, dejando margen para administracion, backups y otros servicios.

## Concurrencia de Inventario

El sistema protege operaciones criticas con transacciones y bloqueos pesimistas:

- Confirmar movimiento.
- Anular movimiento.
- Cancelar movimiento.
- Actualizar `stock_actual`.

Reglas operativas esperadas:

- Un movimiento `BORRADOR` solo puede ser confirmado una vez.
- Un movimiento `BORRADOR` puede cancelarse, pero no confirmarse despues.
- Un movimiento `CONFIRMADO` puede anularse, pero no cancelarse.
- Las salidas concurrentes contra el mismo producto/almacen no deben dejar stock negativo.
- Si una operacion falla, no debe dejar stock ni kardex parcial.

## Timeouts de Base de Datos

No se definen `lock_timeout` ni `statement_timeout` por defecto en la aplicacion.

Motivo:

- Un timeout demasiado bajo puede cortar confirmaciones de inventario o reportes validos.
- Deben definirse despues de medir carga real y tiempos promedio.

Recomendacion posterior a pruebas de carga:

- Evaluar `lock_timeout` para evitar esperas excesivas ante bloqueos.
- Evaluar `statement_timeout` para consultas de reportes pesadas.
- Aplicarlos primero en ambiente de staging.

## Validacion de Arranque

Comandos recomendados antes de desplegar:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

Validar health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"status":"UP"}
```

## Checklist Antes de Produccion

- Flyway valida todas las migraciones.
- Schema actual coincide con entidades JPA.
- `ddl-auto` esta en `validate`.
- `JWT_SECRET` no usa valores de desarrollo.
- `CORS_ALLOWED_ORIGINS` apunta solo al dominio real del frontend.
- Pool Hikari definido por ambiente.
- Logs no exponen passwords ni tokens.
- Pruebas de concurrencia pasan.
- Backup de PostgreSQL probado antes de migrar datos reales.
