# Guía de Despliegue

Cómo desplegar InvestTracker en un servicio de bajo costo/gratuito usando Docker. Usa **Render.com** como ejemplo (plan gratuito con servicio web para Docker + PostgreSQL).

> **Nota (2026-09-12):** esta guía describe un despliegue *posible*, no uno que ya esté corriendo — no hay evidencia en ningún otro doc de que InvestTracker tenga un deploy productivo activo (a diferencia de `wealthtrack`, que sí lo tiene en Vercel). Tratala como un "cómo hacerlo si/cuando lo necesites", no como estado actual.
>
> También: el `Dockerfile` en la raíz del repo ahora es **multi-stage** — compila el jar desde el código fuente dentro del build de Docker. El Paso 1 de abajo (`./gradlew build` manual) **ya no es un prerequisito obligatorio** para `docker build .`, aunque sigue siendo útil para correr tests/verificar localmente antes de construir la imagen.

## Prerrequisitos

1. **Cuentas**: [GitHub](https://github.com/) (o similar), [Docker Hub](https://hub.docker.com/), [Render](https://render.com/).
2. **Software**: [Git](https://git-scm.com/downloads), [Docker Desktop](https://www.docker.com/products/docker-desktop/) corriendo.

---

## Paso 1: Construir la Aplicación (opcional con el Dockerfile actual)

```bash
./gradlew build
```

Esto crea `investracker-0.0.1-SNAPSHOT.jar` en `build/libs/` (nombre actualizado tras el rename del proyecto — antes era `file-importer-0.0.1-SNAPSHOT.jar`).

## Paso 2: Construir y Subir la Imagen de Docker

```bash
docker login
docker build -t TU_USUARIO_DOCKERHUB/investracker:latest .
docker push TU_USUARIO_DOCKERHUB/investracker:latest
```

---

## Paso 3: Configurar la Infraestructura en Render

1. **Base de Datos PostgreSQL**: New > PostgreSQL → nombre (ej. `investracker-db`) → plan Free → Create Database. Copiá el `Internal Connection URL` de la sección **Connections**.
2. **Servicio Web**: New > Web Service → "Deploy an existing image from a repository" → Image URL: `docker.io/TU_USUARIO_DOCKERHUB/investracker:latest` → nombre (ej. `investracker-api`) → plan Free. No hagas clic en "Create" todavía.

---

## Paso 4: Variables de Entorno

En la misma página, sección **Advanced** → **Environment Variables**:

| Clave | Valor | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `Internal Connection URL` de la DB | Conexión a Postgres |
| `SPRING_DATASOURCE_USERNAME` | usuario de la DB en Render | |
| `SPRING_DATASOURCE_PASSWORD` | contraseña de la DB en Render | |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | Liquibase maneja las migraciones; Hibernate solo verifica el esquema |
| `CRYPTOCOMPARE_API_KEY` | tu API key de CryptoCompare | Verificá el nombre exacto de la variable en `application.yml`/el código antes de asumir que coincide |

---

## Paso 5: Desplegar

1. Con las variables configuradas, "Create Web Service".
2. Mirá la pestaña **Logs** — deberías ver el arranque de Spring Boot sin errores.
3. Cuando el estado diga "Live", la URL pública aparece arriba de la página del servicio.

## Próximos Pasos (Opcional)

Conectar Render directamente a tu repo de GitHub (**Build & Deploy** → conectar cuenta → elegir repo) para que cada `git push` a la rama principal dispare un build+deploy automático.
