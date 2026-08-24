# ACM Diproyec Logística

Configuración inicial del sistema logístico para Constructora ACM Diproyec. El
backend usa Java 21, Spring Boot, JDBC y PostgreSQL/Supabase. No utiliza JPA ni
Hibernate: las futuras operaciones de datos seguirán el patrón DAO.

## Requisitos

- JDK 21.
- Docker y Supabase CLI, solo para trabajar con la base local y migraciones.
- Acceso a una base PostgreSQL de Supabase únicamente para la verificación
  manual de conexión.

## Estructura

```text
src/main/java/pe/acmdiproyec/logistica/
  config/ controller/ service/ dao/ model/ dto/ exception/
  LogisticaApplication.java
src/main/resources/application.yml
supabase/config.toml
supabase/migrations/
```

Las capas están preparadas pero todavía no tienen módulos ni interfaces.

## Variables de entorno

La aplicación obtiene la conexión exclusivamente desde `SUPABASE_DB_URL`,
`SUPABASE_DB_USER` y `SUPABASE_DB_PASSWORD`. Copie `.env.example` para tomar
como referencia, pero no guarde el archivo `.env` ni credenciales en Git.

En PowerShell, para la sesión actual:

```powershell
$env:SUPABASE_DB_URL = "jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USER = "postgres"
$env:SUPABASE_DB_PASSWORD = "su-secreto-local"
```

La URL debe incluir `sslmode=require` para conexiones a Supabase.

## Ejecutar el backend

En PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

En Bash:

```bash
./mvnw test
./mvnw spring-boot:run
```

Las pruebas unitarias no requieren una base remota ni credenciales. Arrancar la
aplicación y comprobar una conexión real es una verificación manual/integración
opcional y sí requiere las tres variables de entorno.

Con la aplicación iniciada, consulte el estado sin detalles sensibles:

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
