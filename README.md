# ACM Diproyec Logística

Sistema web para centralizar el futuro control de entradas, salidas y
existencias del almacén de Constructora ACM Diproyec. El proyecto se encuentra
en una etapa de arquitectura inicial: ofrece una página de presentación y la
estructura sobre la que se implementarán los módulos funcionales.

## Tecnologías

- Java 21 y Maven Wrapper.
- Spring Boot 3.5.7, Spring MVC, Thymeleaf y Actuator.
- Spring JDBC y patrón DAO, sin JPA ni Hibernate.
- PostgreSQL/Supabase como base de datos.
- HTML y CSS responsivo sin frameworks JavaScript.

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
- **Security:** reserva la futura configuración de autenticación y autorización;
  aún no contiene una implementación de seguridad.
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

La aplicación obtiene la conexión exclusivamente desde `SUPABASE_DB_URL`,
`SUPABASE_DB_USER` y `SUPABASE_DB_PASSWORD`. Copie `.env.example` únicamente
como referencia. No guarde archivos `.env`, credenciales, tokens ni secretos en
Git.

En PowerShell, para la sesión actual:

```powershell
$env:SUPABASE_DB_URL = "jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USER = "postgres"
$env:SUPABASE_DB_PASSWORD = "su-secreto-local"
```

En Linux, use valores obtenidos desde la configuración segura del proyecto:

```bash
export SUPABASE_DB_URL='jdbc:postgresql://host-ejemplo:5432/base-ejemplo?sslmode=require'
export SUPABASE_DB_USER='usuario-ejemplo'
read -s SUPABASE_DB_PASSWORD
export SUPABASE_DB_PASSWORD
```

Desde el IDE, configure esas mismas tres variables en la configuración local de
ejecución: **Environment variables** en IntelliJ IDEA o la propiedad `env` de
un archivo local `.vscode/launch.json` en Visual Studio Code. La carpeta
`.vscode/` está ignorada y ningún archivo con valores reales debe confirmarse.

La URL debe incluir `sslmode=require` para conexiones remotas a Supabase.
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

La ejecución normal necesita las tres variables de conexión. Las pruebas usan
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
preparados. Permanecen pendientes la autenticación, los CRUD web, la operación
de movimientos desde la interfaz, el inventario funcional y los reportes.
Consulte [CONTRIBUTING.md](CONTRIBUTING.md) antes de desarrollar una
funcionalidad.
