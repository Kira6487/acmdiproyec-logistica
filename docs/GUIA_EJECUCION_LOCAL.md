# Guía de ejecución local — ACM Diproyec Logística

Guía para ejecutar el aplicativo en un equipo con Windows y revisar la interfaz
antes de continuar con la Fase 2.

Toda la información de esta guía se verificó **ejecutando la aplicación**, no
solo compilándola. Cada afirmación indica qué se comprobó realmente.

---

## 1. Requisitos previos

| Requisito | Versión mínima | Comprobación |
| --- | --- | --- |
| JDK | **Java 21** | `java -version` debe mostrar 21 o superior |
| Maven Wrapper | incluido en el repositorio | `.\mvnw.cmd -v` descarga Maven la primera vez |
| Acceso a PostgreSQL/Supabase | usuario, contraseña y host alcanzables | ver §3 |
| Puerto local | **8080** libre | `Get-NetTCPConnection -LocalPort 8080 -State Listen` |

No es necesario instalar Maven ni Node.js. No se usa JPA/Hibernate ni ningún
framework frontend: el HTML se renderiza con Thymeleaf y los estilos con
Bootstrap 5 (CDN) más `static/css/acm-theme.css`.

---

## 2. Comandos exactos (Windows PowerShell)

Ubíquese primero en la carpeta del repositorio:

```powershell
cd C:\ruta\del\proyecto\acmdiproyec-logistica
```

### 2.1 Ejecutar las pruebas

```powershell
.\mvnw.cmd test
```

### 2.2 Empaquetar

```powershell
.\mvnw.cmd package
```

El resultado es `target\acmdiproyec-logistica-0.0.1-SNAPSHOT.jar`.

### 2.3 Iniciar la aplicación

Opción A — con el JAR empaquetado (recomendada para la demo):

```powershell
java -jar target\acmdiproyec-logistica-0.0.1-SNAPSHOT.jar
```

Opción B — con el wrapper de Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

Con perfil de producción:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

---

## 3. Variables de entorno

La aplicación **no** carga un archivo `.env`. Las variables deben definirse en
el entorno del proceso Java (shell, IDE o servicio de despliegue).

| Variable | Obligatoria | Valor de ejemplo (no sensible) | Descripción |
| --- | :---: | --- | --- |
| `SUPABASE_DB_PASSWORD` | **Sí, para usar la aplicación** | *(nunca se documenta)* | Contraseña de la base de datos. **No tiene valor por defecto.** |
| `SUPABASE_DB_USER` | No | `postgres` | Usuario de la base de datos |
| `SUPABASE_DB_HOST` | No | `db.tqeiokeqjwwdkafsjmoi.supabase.co` | Host de conexión directa |
| `SUPABASE_DB_PORT` | No | `5432` | Puerto PostgreSQL |
| `SUPABASE_DB_NAME` | No | `postgres` | Base de datos |
| `SUPABASE_DB_URL` | No | — | Cadena JDBC completa; alternativa a `HOST`/`PORT`/`NAME`. Debe incluir `sslmode=require` |
| `PORT` | No | `8080` | Puerto HTTP (solo perfil `prod`) |

Definición en PowerShell para la sesión actual:

```powershell
$env:SUPABASE_DB_PASSWORD = "ESCRIBA_AQUI_LA_CONTRASENA_REAL"
$env:SUPABASE_DB_HOST     = "db.tqeiokeqjwwdkafsjmoi.supabase.co"
$env:SUPABASE_DB_PORT     = "5432"
$env:SUPABASE_DB_NAME     = "postgres"
$env:SUPABASE_DB_USER     = "postgres"
```

> **No escriba la contraseña en este archivo ni en ningún archivo versionado.**
> En Linux/macOS use `read -s SUPABASE_DB_PASSWORD; export SUPABASE_DB_PASSWORD`
> para que no quede en el historial.

### ⚠️ Comportamiento verificado si falta la contraseña

Se comprobó arrancando el JAR **sin** definir `SUPABASE_DB_PASSWORD`:

```
Started LogisticaApplication in 6.161 seconds
```

**La aplicación sí arranca.** Spring Boot no valida los placeholders sin
resolver y el valor de `spring.datasource.password` queda como texto literal.
El efecto es que **la primera operación que necesita la base de datos falla**:

```
org.springframework.security.authentication.InternalAuthenticationServiceException:
Failed to obtain JDBC Connection
```

Es decir: **arrancar no equivale a funcionar**. Sin la variable, podrá ver la
portada y el login, pero nadie podrá iniciar sesión.

---

## 4. Cómo iniciar el aplicativo

1. Defina las variables de entorno (§3).
2. Ejecute el JAR (§2.3, opción A).
3. Espere a ver la línea:

   ```
   Started LogisticaApplication in X.XXX seconds
   ```

4. Abra el navegador en la URL de §5.

El arranque tarda entre 5 y 15 segundos y **no escribe nada en la base de
datos**: no hay Flyway/Liquibase ni `spring.sql.init` activo. Solo se abre el
pool de conexiones (HikariCP).

---

## 5. URLs locales

| Pantalla | URL | Acceso |
| --- | --- | --- |
| Portada pública | `http://localhost:8080/` | Público |
| **Inicio de sesión** | `http://localhost:8080/login` | Público |
| Panel logístico | `http://localhost:8080/panel` | Requiere sesión |
| Existencias | `http://localhost:8080/inventario` | Requiere sesión |
| Movimientos | `http://localhost:8080/movimientos` | Requiere sesión |
| Productos | `http://localhost:8080/productos` | Requiere sesión |
| Categorías | `http://localhost:8080/categorias` | Requiere sesión |
| Unidades de medida | `http://localhost:8080/unidades-medida` | Requiere sesión |
| Motivos de movimiento | `http://localhost:8080/motivos` | Requiere sesión |
| Usuarios | `http://localhost:8080/usuarios` | Solo `ADMIN` |

Cualquier URL no autenticada redirige automáticamente a `/login`.

> **Nota sobre `/actuator/health`:** la configuración de seguridad actual exige
> autenticación para todas las rutas no listadas, y `/actuator/**` no está
> incluida en las rutas públicas. Por eso `http://localhost:8080/actuator/health`
> **redirige al login** en lugar de mostrar el estado. Es un comportamiento
> esperado con la configuración vigente.

---

## 6. Cómo cerrar el aplicativo

- En la ventana donde se ejecuta: `Ctrl + C`.
- Si quedó en segundo plano:

  ```powershell
  Get-Process java | Where-Object { $_.CommandLine -like '*acmdiproyec*' } | Stop-Process -Force
  ```

- Si el puerto 8080 queda ocupado:

  ```powershell
  Get-NetTCPConnection -LocalPort 8080 -State Listen
  Stop-Process -Id <PID> -Force
  ```

---

## 7. Revisar los logs y diagnosticar errores

La aplicación escribe los logs por consola (nivel `INFO` y `ERROR`). Para
conservarlos en archivo:

```powershell
java -jar target\acmdiproyec-logistica-0.0.1-SNAPSHOT.jar *> ejecucion.log
```

Mensajes típicos y su significado:

| Mensaje | Causa |
| --- | --- |
| `Started LogisticaApplication in X.XXX seconds` | Arranque correcto |
| `Tomcat started on port 8080` | Servidor web listo |
| `Failed to obtain JDBC Connection` | No hay conexión con PostgreSQL |
| `password authentication failed` | Usuario o contraseña incorrectos |
| `Host desconocido` / `UnknownHostException` | El host de la base no resuelve en esta red |
| `Connection refused` | Puerto cerrado o bloqueado por firewall |
| `Could not resolve placeholder 'SUPABASE_DB_PASSWORD'` | No debería ocurrir: la aplicación arranca igualmente (ver §3) |

El endpoint `/actuator/health` **no** está disponible sin iniciar sesión (ver §5).

---

## 8. Distinguir un problema de arranque de uno de conexión

| Síntoma | Tipo de problema |
| --- | --- |
| La consola **no** llega a `Started LogisticaApplication` | **Arranque.** Revise Java 21, puerto ocupado (`BindException`) o dependencias sin descargar |
| `BindException: Address already in use` | **Arranque.** Otro proceso usa el puerto 8080 |
| La consola muestra `Started LogisticaApplication` pero el login falla | **Conexión a PostgreSQL.** Revise variables de entorno y red |
| `Failed to obtain JDBC Connection` al entrar | **Conexión.** Contraseña ausente/incorrecta o host inalcanzable |
| La portada y el login se ven, pero ninguna consulta muestra datos | **Conexión.** La autenticación consulta la tabla `usuarios` |

**Regla práctica:** si ve la portada y el formulario de login, el arranque
funcionó; el problema restante es siempre de acceso a datos.

---

## 9. Pasos para revisar visualmente la interfaz

1. **Portada** — `http://localhost:8080/`: tarjetas de los seis módulos, acceso
   a «Iniciar sesión». Esta página es pública.
2. **Login** — `http://localhost:8080/login`: tarjeta oscura con la marca
   «ACM Diproyec», campos de correo y contraseña y el botón de acceso.
3. **Panel** — `http://localhost:8080/panel` (tras iniciar sesión): cuatro
   indicadores, «Productos por reponer», «Últimos movimientos» y accesos
   modulares.
4. **Existencias** — `http://localhost:8080/inventario`: filtros de búsqueda,
   categoría y estado de stock, tabla con barra de nivel y estado.
5. **Movimientos** — `http://localhost:8080/movimientos`: filtros por producto,
   tipo, estado y rango de fechas.
6. **Datos maestros** — `/productos`, `/categorias`, `/unidades-medida`,
   `/motivos`: listados con acciones y formularios.
7. **Responsividad** — cambie el ancho de la ventana: el sidebar se colapsa en
   móvil y las rejillas se apilan.

Los estilos se sirven desde `static/css/acm-theme.css` y Bootstrap 5 desde CDN:
**si no hay acceso a Internet, la página se verá sin estilo** (el HTML y la
funcionalidad siguen operativos).

---

## 10. Limitaciones conocidas de la demo

1. **No existe un usuario de demostración documentado.** Los usuarios viven en
   la tabla `usuarios` de Supabase y el repositorio no incluye credenciales de
   prueba. La migración `20260824000000` crea un usuario de sistema
   (`SISTEMA_CARGA_INICIAL`) **inactivo y sin contraseña utilizable**, pensado
   solo como responsable de datos de prueba.
2. **No se puede crear el primer usuario desde la interfaz.** `/usuarios/nuevo`
   exige sesión de `ADMIN`, y obtener esa sesión exige ya existir en la base: es
   un círculo cerrado. El primer usuario debe crearse directamente en la base de
   datos (ver §3.1 de la sección de pendientes del informe).
3. **Sin base de datos no hay demo de módulos.** Todos los listados, formularios
   y el panel consultan PostgreSQL.
4. **`/actuator/health` requiere sesión** con la configuración actual.
5. **El módulo de reportes y la exportación a Excel no están implementados.**
6. **El problema de conectividad de red** descrito en tareas anteriores
   (el endpoint de Supabase resuelve solo a IPv6 y esta red es IPv4) sigue
   vigente y **no se solucionó en esta tarea**, conforme a lo indicado.

---

## 11. Cómo crear el primer usuario (acción manual, fuera de esta tarea)

No se ejecutó en esta tarea porque requiere escritura en la base de datos.
Cuando quiera habilitar el acceso:

1. Genere un hash BCrypt de la contraseña que quiera usar. Puede hacerlo con
   `htpasswd -bnBC 10 "" "su-contrasena" | tr -d ':\n'` o con
   `new BCryptPasswordEncoder().encode("...")` desde una consola Java.
2. Inserte un registro en `public.usuarios` con `id_rol` de un rol existente
   (`ADMIN` o `LOGISTICA`), un `correo` válido, ese `password_hash` y
   `activo = true`.
3. Inicie sesión con ese correo.

**No almacene la contraseña en archivos versionados ni la incluya en commits.**
