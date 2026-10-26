# ACM Diproyec Logística

Sistema web para centralizar el futuro control de entradas, salidas y
existencias del almacén de Constructora ACM Diproyec. El proyecto se encuentra
en una etapa de arquitectura inicial: ofrece una página de presentación y la
estructura sobre la que se implementarán los módulos funcionales.

## Tecnologías

- Java 21 y Maven Wrapper.
- Spring Boot 3.5.7, Spring MVC, Spring Security, Thymeleaf y Actuator.
- Spring JDBC y patrón DAO, sin JPA ni Hibernate.
- PostgreSQL/Supabase como base de datos.
- HTML, CSS mobile-first y JavaScript nativo sin frameworks frontend.

## Arquitectura MVC por capas

El paquete base es `pe.acmdiproyec.logistica` y separa responsabilidades de la
siguiente manera:

- **Controller:** recibe solicitudes HTTP; `web` devuelve vistas Thymeleaf y
  `api` alojará futuros endpoints REST.
- **Service:** concentra la lógica de negocio y coordina los DAO.
- **DAO:** define el acceso a datos mediante JDBC; `dao.impl` contendrá sus
  implementaciones.
- **Model:** representa las entidades y conceptos del dominio logístico.
- **DTO:** transporta datos entre la interfaz y las capas internas.
- **Config:** reúne la configuración técnica y transversal.
- **Security:** configura la autenticación y la autorización por roles con
  Spring Security (`SecurityConfig` y `CustomUserDetailsService`).
- **Exception:** gestiona errores de forma centralizada sin mostrar detalles
  internos al usuario.
- **Templates:** contiene las vistas procesadas en el servidor con Thymeleaf.
- **Static:** contiene CSS, JavaScript e imágenes servidos directamente.

```text
src/main/java/pe/acmdiproyec/logistica/
├── config/
├── controller/{api,web}/
├── dao/impl/
├── dto/
├── exception/
├── model/
├── security/
├── service/
└── util/
src/main/resources/
├── templates/{auth,maestros,movimientos,inventario,reportes}/
└── static/{css,js,img}/
supabase/{config.toml,migrations/}
```

## Requisitos

- JDK 21.
- Docker y Supabase CLI, solo para trabajar con una base local y migraciones.
- Acceso a PostgreSQL/Supabase únicamente cuando se quiera verificar
  manualmente la conexión real.

## Variables de entorno

La conexión a Supabase se configura por completo mediante variables de entorno.
Spring Boot **no** carga un archivo `.env` de forma automática y el proyecto no
incluye dependencias para hacerlo: defina las variables en el entorno real del
proceso (shell, sistema operativo, IDE o servicio de despliegue). Copie
`.env.example` únicamente como referencia. No guarde archivos `.env`,
credenciales, tokens ni secretos en Git.

| Variable | Obligatoria | Descripción |
| --- | --- | --- |
| `SUPABASE_DB_PASSWORD` | **Sí** | Contraseña de la base de datos. No admite valor predeterminado: si falta, la aplicación no arranca. |
| `SUPABASE_DB_USER` | No | Usuario. Predeterminado `postgres`. |
| `SUPABASE_DB_HOST` | No | Host de conexión directa. Predeterminado el del entorno de pruebas. |
| `SUPABASE_DB_PORT` | No | Puerto. Predeterminado `5432`. |
| `SUPABASE_DB_NAME` | No | Base de datos. Predeterminado `postgres`. |
| `SUPABASE_DB_URL` | No | Cadena JDBC completa. Alternativa a `HOST`/`PORT`/`NAME` y compatible con el resto del proyecto. Debe incluir `sslmode=require`. |

La forma recomendada es la desglosada (`SUPABASE_DB_HOST`, `SUPABASE_DB_PORT` y
`SUPABASE_DB_NAME`): construye la URL JDBC con `sslmode=require` de forma
ineludible. La URL nunca lleva usuario ni contraseña embebidos; el usuario se
aporta mediante `SUPABASE_DB_USER` y la contraseña únicamente mediante
`SUPABASE_DB_PASSWORD`.

En PowerShell, para la sesión actual:

```powershell
$env:SUPABASE_DB_PASSWORD = "su-secreto-local"
```

En Linux, use valores obtenidos desde la configuración segura del proyecto:

```bash
read -s SUPABASE_DB_PASSWORD
export SUPABASE_DB_PASSWORD
```

Desde el IDE, configure las variables en la configuración local de ejecución:
**Environment variables** en IntelliJ IDEA o la propiedad `env` de un archivo
local `.vscode/launch.json` en Visual Studio Code. La carpeta `.vscode/` está
ignorada y ningún archivo con valores reales debe confirmarse.

HikariCP usa inicialmente un máximo de 5 conexiones, una conexión mínima y
tiempos acotados para conexión y validación.

## Ejecutar con Maven Wrapper

En PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

En Bash:

```bash
./mvnw test
./mvnw package
./mvnw spring-boot:run
```

La ejecución normal necesita, como mínimo, definir `SUPABASE_DB_PASSWORD`. Las pruebas usan
el perfil `test`, que desactiva únicamente la autoconfiguración del DataSource y
no requiere una base remota ni agrega una base H2. Para previsualizar la portada
sin conectarse a Supabase:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

Abra `http://localhost:8080/` para ver la página inicial. El estado de la
aplicación se consulta sin detalles sensibles en:

```text
http://localhost:8080/actuator/health
```

Existe una prueba opcional de conexión que ejecuta `select 1` y comprueba acceso
de solo lectura a las tablas esperadas. Se omite automáticamente cuando falta
alguna variable y puede ejecutarse de forma explícita con:

```bash
./mvnw -Dtest=SupabaseConnectionIntegrationTest test
```

La prueba no inserta, modifica ni elimina información y evita incluir la URL o
la contraseña en sus mensajes de error.

## Trabajo colaborativo y calidad

- `docs/GUIA_DESARROLLO_EQUIPO.md` define las responsabilidades por capa y la
  distribución sugerida de módulos.
- `CONTRIBUTING.md` describe el flujo futuro desde `develop`, los commits y la
  revisión mediante pull requests.
- `.github/workflows/ci.yml` ejecuta pruebas y empaquetado con Java 21 sin
  conectarse a Supabase.
- `.editorconfig` mantiene formato consistente entre los IDE del equipo.

La interfaz dispone de fragmentos Thymeleaf compartidos y componentes CSS para
formularios, tablas, mensajes y estados. Los accesos de la portada están
deshabilitados hasta que cada módulo tenga backend y pruebas reales.

## Despliegue

El perfil `prod` acepta el puerto de la plataforma y mantiene ocultos los
detalles de errores y health checks. La evaluación de Vercel y el procedimiento
genérico para desplegar el JAR se encuentran en `docs/DESPLIEGUE.md`.

Vercel no ofrece actualmente un runtime Java oficial para ejecutar esta
aplicación Spring Boot completa. No se incluye una configuración de Vercel ni un
runtime comunitario.

## Migraciones de Supabase

Las migraciones se encuentran en `supabase/migrations/`. La migración de datos
de prueba agrega `stock_minimo`, registra 11 prefabricados de concreto y crea el
movimiento de entrada `CARGA-INICIAL-2026`. El stock de 100 unidades se obtiene
mediante los triggers de movimiento existentes, no mediante una actualización
aislada del inventario.

Para un entorno local controlado, inicie Supabase y aplique las migraciones con
la CLI:

```bash
supabase start
supabase db reset
```

No ejecute `supabase login`, `supabase link`, `supabase db push` ni despliegues
remotos como parte de esta configuración. Tampoco modifique producción mediante
SQL Editor cuando el cambio ya existe como migración: actualice la migración en
desarrollo y aplíquela mediante el flujo de migraciones revisado.

Después de aplicar las migraciones, ejecute las consultas de solo lectura de
`docs/sql/validar_datos_iniciales_inventario.sql` para comprobar los 11
productos, el stock mínimo 20, el stock actual 100 y la igualdad entre el saldo
y el historial vigente de entradas y salidas.

## Estado actual

La arquitectura por capas, la portada Thymeleaf, el manejo web básico de errores,
la configuración por ambientes y los datos controlados de inventario están
preparados. Permanecen pendientes los CRUD web de datos maestros, la operación de
  movimientos desde la interfaz, el inventario funcional y los reportes.
Consulte [CONTRIBUTING.md](CONTRIBUTING.md) antes de desarrollar una
funcionalidad.
