# Preparación de despliegue

## Decisión sobre Vercel

Revisión realizada el 24 de agosto de 2026 sobre la documentación oficial:

- [Runtimes oficiales de Vercel](https://vercel.com/docs/functions/runtimes)
  enumera Node.js, Bun, Python, Rust, Go, Ruby, Wasm y Edge; Java no figura como
  runtime oficial.
- [Límites de Vercel Functions](https://vercel.com/docs/functions/limitations)
  describe funciones efímeras, límites de tamaño y duración, y la necesidad de
  administrar cuidadosamente las conexiones a bases de datos.
- [Funcionamiento de Vercel Functions](https://vercel.com/docs/functions)
  indica que las instancias escalan según solicitudes, pueden reducirse a cero y
  no equivalen a un servidor de aplicación permanentemente activo.
- [Configuración de `vercel.json`](https://vercel.com/docs/project-configuration/vercel-json)
  está orientada a funciones y runtimes soportados.

Por ello, Vercel no ofrece actualmente compatibilidad nativa oficial para
desplegar esta aplicación Spring Boot 3.5.7 con Java 21 y Tomcat embebido como
un servidor persistente. No se incluye `vercel.json`, `.vercelignore` ni un
runtime comunitario no verificado.

No existe un comando de build ni un punto de entrada Java oficiales que conviertan
este JAR en una Vercel Function. Forzarlo implicaría adaptar la aplicación a un
runtime comunitario y asumir límites de bundle, duración, arranque en frío y
conexiones por instancia. Eso no constituye compatibilidad nativa verificable.

La aplicación renderiza HTML con Thymeleaf en el servidor. El frontend y el
backend forman una sola unidad desplegable; separarlos exigiría refactorizar las
vistas y crear una API. Si en el futuro existiera un frontend estático o una SPA
independiente, solo esa parte podría evaluarse para Vercel. Esta fase no migra a
React, Next.js ni duplica `index.html`.

## Plataforma recomendada para el backend

Se recomienda evaluar un servicio web persistente compatible con contenedores y
Java, por ejemplo [Render Web Services](https://render.com/docs/web-services).
Su documentación oficial permite ejecutar aplicaciones web dinámicas, definir
comandos de construcción e inicio, usar variables de entorno y exponer el puerto
indicado por `PORT`. La elección final debe considerar costo, región disponible,
latencia hacia Supabase y políticas operativas del proyecto.

No se agrega configuración específica de un proveedor en esta fase. El artefacto
continúa siendo un JAR ejecutable y puede trasladarse a cualquier plataforma con
un proceso Java persistente.

## Construcción y ejecución genéricas

Requisitos del entorno de despliegue:

- JDK/JRE 21.
- Acceso de red seguro a PostgreSQL/Supabase.
- Un proceso persistente que exponga el puerto indicado por la plataforma.

Construcción:

```bash
./mvnw clean package
```

Inicio:

```bash
java -jar target/acmdiproyec-logistica-0.0.1-SNAPSHOT.jar
```

La plataforma debe configurar `SPRING_PROFILES_ACTIVE=prod`. El perfil de
producción toma el puerto desde `PORT` y no ejecuta scripts SQL al iniciar.
Las migraciones continúan aplicándose exclusivamente mediante el flujo aprobado
de Supabase, separado del arranque de la aplicación.

## Variables requeridas

- `SUPABASE_DB_URL`
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `PORT` (normalmente proporcionada por la plataforma)

Los valores se administran como secretos del proveedor y nunca se guardan en el
repositorio. Tras desplegar, compruebe `GET /` y `GET /actuator/health`; el health
check no debe exponer componentes ni detalles internos.
