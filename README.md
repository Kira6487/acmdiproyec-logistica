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

La URL debe incluir `sslmode=require` para conexiones remotas a Supabase.

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

## Migraciones de Supabase

La migración inicial se encuentra en `supabase/migrations/`. Para un entorno
local controlado, inicie Supabase y aplique las migraciones con la CLI:

```bash
supabase start
supabase db reset
```

No ejecute `supabase login`, `supabase link`, `supabase db push` ni despliegues
remotos como parte de esta configuración. Tampoco modifique producción mediante
SQL Editor cuando el cambio ya existe como migración: actualice la migración en
desarrollo y aplíquela mediante el flujo de migraciones revisado.

## Estado actual

La arquitectura por capas, la portada Thymeleaf, el manejo web básico de errores
y la configuración por ambientes están preparados. Permanecen pendientes la
autenticación, los datos maestros, los movimientos, el inventario y los
reportes. Consulte [CONTRIBUTING.md](CONTRIBUTING.md) antes de desarrollar una
funcionalidad.
