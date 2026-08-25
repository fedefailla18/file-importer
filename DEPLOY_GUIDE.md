# Guía de Despliegue para la Aplicación File Importer

Esta guía te mostrará cómo desplegar tu aplicación en un servicio de bajo costo/gratuito usando Docker. Usaremos **Render.com** como ejemplo porque ofrece un plan gratuito que incluye un servicio web para Docker y una base de datos PostgreSQL.

## Prerrequisitos

1.  **Cuentas Creadas**:
    *   [GitHub](https://github.com/) (o GitLab/Bitbucket). Tu código debe estar en un repositorio.
    *   [Docker Hub](https://hub.docker.com/): Para almacenar tu imagen de Docker.
    *   [Render](https://render.com/): La plataforma donde desplegaremos la aplicación.
2.  **Software Instalado**:
    *   [Git](https://git-scm.com/downloads)
    *   [Docker Desktop](https://www.docker.com/products/docker-desktop/). Asegúrate de que esté corriendo.

---

## Paso 1: Construir la Aplicación

El `Dockerfile` necesita el archivo `.jar` de tu aplicación para funcionar. Este se genera con un comando de Gradle.

Desde la raíz de tu proyecto, ejecuta:

```bash
./gradlew build
```

Esto creará el archivo `file-importer-0.0.1-SNAPSHOT.jar` dentro del directorio `build/libs/`.

## Paso 2: Construir y Subir la Imagen de Docker

Ahora que tienes el `.jar`, vamos a "empaquetar" tu aplicación en una imagen de Docker y subirla a Docker Hub para que Render pueda acceder a ella.

1.  **Inicia sesión en Docker Hub** (te pedirá tu usuario y contraseña):
    ```bash
    docker login
    ```

2.  **Construye la imagen**. Reemplaza `TU_USUARIO_DOCKERHUB` con tu nombre de usuario real. El `.` al final es importante.
    ```bash
    docker build -t TU_USUARIO_DOCKERHUB/file-importer:latest .
    ```

3.  **Sube la imagen a Docker Hub**:
    ```bash
    docker push TU_USUARIO_DOCKERHUB/file-importer:latest
    ```

---

## Paso 3: Configurar la Infraestructura en Render

Vamos a crear la base de datos y el servicio web.

1.  **Crear la Base de Datos PostgreSQL**:
    *   En tu dashboard de Render, ve a **New > PostgreSQL**.
    *   Dale un nombre (ej. `file-importer-db`).
    *   Selecciona el plan **Free**.
    *   Haz clic en **Create Database**.
    *   Una vez creada, busca la sección **Connections** y copia el `Internal Connection URL`. La necesitaremos en un momento.

2.  **Crear el Servicio Web**:
    *   Ve a **New > Web Service**.
    *   Elige la opción **Deploy an existing image from a repository**.
    *   En el campo **Image URL**, pega la ruta a tu imagen en Docker Hub: `docker.io/TU_USUARIO_DOCKERHUB/file-importer:latest`.
    *   Dale un nombre a tu servicio (ej. `file-importer-api`).
    *   Elige el plan **Free**.
    *   No hagas clic en "Create" todavía. Necesitamos configurar las variables de entorno.

---

## Paso 4: Configurar las Variables de Entorno

Esta es la parte más importante. Le dice a tu aplicación cómo conectarse a la base de datos y le proporciona otras configuraciones secretas.

En la misma página de creación del servicio web, ve a la sección **Advanced**, y luego a **Environment Variables**.

Agrega las siguientes variables:

| Clave                          | Valor                                                                                                                                                           | Descripción                                                                                                                            |
| ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`        | Pega aquí el `Internal Connection URL` de tu base de datos de Render.                                                                                           | La dirección de tu base de datos PostgreSQL.                                                                                           |
| `SPRING_DATASOURCE_USERNAME`   | El usuario de la base de datos (lo encuentras en la página de la DB en Render).                                                                                 | Usuario para la conexión a la DB.                                                                                                      |
| `SPRING_DATASOURCE_PASSWORD`   | La contraseña de la base de datos (la encuentras en la página de la DB en Render).                                                                               | Contraseña para la conexión a la DB.                                                                                                   |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`| `validate`                                                                                                                                                      | Buena práctica. Le dice a Hibernate que solo verifique que el esquema de la DB coincida, ya que Liquibase se encarga de las migraciones. |
| `CRYPTOCOMPARE_API_KEY`        | `TU_API_KEY_DE_CRYPTOCOMPARE`                                                                                                                                   | Tu API key para el servicio externo. (El nombre exacto de la variable puede variar, revísalo en tu `application.yml` o en el código). |

**Nota sobre `CRYPTOCOMPARE_API_KEY`**: El nombre exacto de la variable depende de cómo la estés leyendo en tu código (ej. `@Value("${cryptocompare.api.key}")`). Asegúrate de que la clave aquí coincida con la que espera tu aplicación.

---

## Paso 5: Desplegar

1.  Con las variables de entorno configuradas, haz clic en **Create Web Service**.
2.  Render comenzará el despliegue. Ve a la pestaña **Logs** de tu nuevo servicio para ver el progreso en tiempo real.
3.  Verás los logs de Spring Boot. Si todo está bien, la aplicación se iniciará sin errores.
4.  Una vez que el estado diga **"Live"**, tu aplicación estará desplegada. En la parte superior de la página de tu servicio en Render, encontrarás la URL pública para acceder a ella (ej. `https://file-importer-api.onrender.com`).

---

## Próximos Pasos (Opcional)

Para evitar tener que subir la imagen de Docker manualmente cada vez, puedes conectar Render directamente a tu repositorio de GitHub.

*   En la configuración de tu servicio en Render, ve a la pestaña **Build & Deploy**.
*   Conecta tu cuenta de GitHub y elige tu repositorio.
*   Render puede entonces construir y desplegar tu aplicación automáticamente cada vez que haces un `git push` a tu rama principal.

¡Felicidades! Has completado tu primer deploy.
