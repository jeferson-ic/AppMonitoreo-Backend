# AppMonitoreo Backend

API REST para el reporte y monitoreo de incidentes en zonas peligrosas.
Spring Boot 4 · Java 21 · MySQL 8 · JWT.

## Ejecución local

Requiere MySQL con el esquema de `db/script_database.sql`.

```bash
./mvnw spring-boot:run
```

Por defecto se conecta a `localhost:3306` con `root/root`. Para otros valores, definir las variables de entorno de la tabla siguiente.

## Variables de entorno

| Variable | Descripción | Obligatoria en prod |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` activa HTTPS obligatorio y oculta detalles de error | Sí |
| `DB_URL` | URL JDBC de MySQL, por ejemplo `jdbc:mysql://host:puerto/db_zonas_peligrosas?sslMode=REQUIRED` | Sí |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de la base de datos | Sí |
| `JWT_SECRET` | Clave de firma de tokens, mínimo 32 caracteres | Sí |
| `CORS_ALLOWED_ORIGINS` | Orígenes web permitidos, separados por coma | No |
| `PORT` | Puerto HTTP (Render lo define automáticamente) | No |

## Pruebas

```bash
./mvnw verify
```

- Funcionales: `src/test/java/.../funcional`
- No funcionales (seguridad y rendimiento): `src/test/java/.../nofuncional`
- Reporte de cobertura: `target/site/jacoco/index.html`
- Prueba de carga con k6: `k6 run -e BASE_URL=<url> -e CORREO=<correo> -e CONTRASENA=<clave> pruebas/carga.js`

Las pruebas usan H2 en memoria, no necesitan MySQL.

## Despliegue en Render

1. Crear la base de datos MySQL en la nube y cargar `db/script_database.sql`.
2. En Render: **New → Blueprint** y seleccionar este repositorio (usa `render.yaml`).
3. Completar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `CORS_ALLOWED_ORIGINS`. `JWT_SECRET` se genera automáticamente.
4. Verificar el despliegue en `https://<servicio>.onrender.com/actuator/health`.

Cada push a `main` ejecuta el pipeline de GitHub Actions (`.github/workflows/ci.yml`) y Render vuelve a desplegar.
