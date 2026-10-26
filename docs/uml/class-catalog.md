# Catálogo de clases del backend — ACM Diproyec Logística

Inventario exhaustivo de **todas las clases e interfaces reales** del backend,
contrastado con el código fuente de `src/main/java`.

- **Rama:** `feature/autenticacion-usuarios`
- **Paquete base:** `pe.acmdiproyec.logistica`
- **Fuente de verdad:** archivos `.java` de `src/main/java` (excluidos los `package-info.java`, que solo documentan paquetes y no declaran tipos).

---

## Resumen

| Categoría | Cantidad | Detalle |
| --- | ---: | --- |
| **Clases concretas** | **38** | 1 arranque + 10 controladores + 8 servicios + 7 implementaciones DAO + 9 modelos + 2 seguridad + 1 manejador de excepciones |
| **Interfaces** | **7** | Las 7 interfaces DAO (`CategoriaDao`, `UnidadMedidaDao`, `ProductoDao`, `MotivoMovimientoDao`, `MovimientoDao`, `InventarioDao`, `UsuarioDao`) |
| **DTO** | **10** | 9 con uso confirmado + `LoginDto` sin uso |
| **Excepciones propias** | **2** | `RecursoNoEncontradoException`, `ReglaNegocioException` |
| **Enumeraciones Java** | **0** | Los ENUM son de PostgreSQL (`tipo_movimiento`, `estado_movimiento`) y se transportan como `String` |
| **Clases abstractas** | **0** | — |
| **TOTAL de tipos propios** | **57** | 38 + 7 + 10 + 2 = 57, coincide 1:1 con los 57 ficheros `.java` de `src/main/java` |

> **Nota sobre los tipos externos.** `UserDetailsService`, `PasswordEncoder`, `JdbcTemplate`
> y demás tipos de Spring/JDK aparecen en las relaciones pero **no son clases del
> proyecto**; se listan aparte en la sección 10.

---

## Índice por capas

| Capa | Paquete | Tipos |
| --- | --- | --- |
| Arranque | `pe.acmdiproyec.logistica` | `LogisticaApplication` |
| Presentación | `…controller.web` | `AuthController`, `IndexController`, `HomeController`, `CategoriaController`, `UnidadMedidaController`, `ProductoController`, `MotivoMovimientoController`, `MovimientoController`, `InventarioController`, `UsuarioController` |
| Aplicación | `…service` | `AutenticacionService`, `CategoriaService`, `UnidadMedidaService`, `ProductoService`, `MotivoMovimientoService`, `MovimientoService`, `InventarioService`, `UsuarioService` |
| Persistencia (interfaz) | `…dao` | `CategoriaDao`, `UnidadMedidaDao`, `ProductoDao`, `MotivoMovimientoDao`, `MovimientoDao`, `InventarioDao`, `UsuarioDao` |
| Persistencia (JDBC) | `…dao.impl` | `JdbcCategoriaDao`, `JdbcUnidadMedidaDao`, `JdbcProductoDao`, `JdbcMotivoMovimientoDao`, `JdbcMovimientoDao`, `JdbcInventarioDao`, `JdbcUsuarioDao` |
| Dominio | `…model` | `Rol`, `Usuario`, `Categoria`, `UnidadMedida`, `MotivoMovimiento`, `Producto`, `Inventario`, `Movimiento`, `MovimientoDetalle` |
| Transferencia | `…dto` | `CategoriaFormDto`, `UnidadMedidaFormDto`, `ProductoFormDto`, `MotivoMovimientoFormDto`, `MovimientoFormDto`, `MovimientoDetalleFormDto`, `AnulacionMovimientoFormDto`, `UsuarioFormDto`, `UsuarioResponseDto`, `LoginDto` |
| Seguridad | `…security` | `SecurityConfig`, `CustomUserDetailsService` |
| Excepciones | `…exception` | `GlobalExceptionHandler`, `RecursoNoEncontradoException`, `ReglaNegocioException` |

---

# 1. Capa de arranque

## 1.1 `pe.acmdiproyec.logistica.LogisticaApplication`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Capa** | Arranque / configuración |
| **Responsabilidad** | Punto de entrada de la aplicación Spring Boot. Arranca el contexto, la seguridad, el servidor web y la capa de persistencia. |
| **Atributos** | Ninguno |
| **Constructores** | Ninguno explícito (constructor por defecto) |
| **Métodos públicos** | `public static void main(String[] args) : void` |
| **Implementa / extiende** | — |
| **Dependencias** | Ninguna inyectada. Usa `SpringApplication.run(...)` |
| **Relaciones** | Arranca el contexto que contiene `SecurityConfig`, `GlobalExceptionHandler` y todas las capas |
| **Estado** | Implementado. **Prueba:** `LogisticaApplicationTest` (verifica el paquete base) |

---

# 2. Capa de presentación — `pe.acmdiproyec.logistica.controller.web`

Patrón común de los controladores de datos maestros: constantes `VISTA_LISTA` y `VISTA_FORMULARIO`, dependencia por constructor, métodos `listar`, `nuevo`, `crear`, `editar`, `actualizar`, `eliminar` y un `prepararFormulario` privado. **Ninguno contiene SQL ni lógica de negocio.**

## 2.1 `AuthController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Sirve la página de inicio de sesión. El procesamiento de credenciales lo realiza Spring Security (`formLogin`). |
| **Atributos** | Ninguno |
| **Constructores** | Ninguno explícito |
| **Métodos públicos** | `login() : String` → vista `auth/login` |
| **Implementa / extiende** | — |
| **Dependencias** | Ninguna |
| **Relaciones** | La autenticación real la resuelve `CustomUserDetailsService` vía Spring Security |
| **Estado** | Implementado. **Pruebas:** `SeguridadRolesTest` (login/logout, CSRF, usuario inactivo) |

## 2.2 `IndexController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Portada pública del sistema (`GET /`). |
| **Atributos** | Ninguno |
| **Constructores** | Ninguno explícito |
| **Métodos públicos** | `mostrarInicio() : String` → vista `index` |
| **Implementa / extiende** | — |
| **Dependencias** | Ninguna |
| **Relaciones** | — |
| **Estado** | Implementado. **Pruebas:** `IndexControllerTest` (4 pruebas) |

## 2.3 `HomeController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Panel autenticado tras el login (`GET /panel`), con enlaces por rol y cierre de sesión. |
| **Atributos** | Ninguno |
| **Constructores** | Ninguno explícito |
| **Métodos públicos** | `inicio() : String` → vista `auth/index` |
| **Implementa / extiende** | — |
| **Dependencias** | Ninguna |
| **Relaciones** | — |
| **Estado** | Implementado. **Pruebas:** `SeguridadRolesTest` (acceso, enlaces por rol) |

## 2.4 `CategoriaController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Mantenimiento de categorías de productos: listado, alta, edición y eliminación. |
| **Atributos** | `- static final String VISTA_LISTA = "maestros/categorias/lista"` · `- static final String VISTA_FORMULARIO = "maestros/categorias/formulario"` · `- final CategoriaService categoriaService` |
| **Constructores** | `CategoriaController(CategoriaService categoriaService)` |
| **Métodos públicos** | `listar(Model) : String` · `nuevo(Model) : String` · `crear(CategoriaFormDto, BindingResult, Model, RedirectAttributes) : String` · `editar(Long, Model, RedirectAttributes) : String` · `actualizar(Long, CategoriaFormDto, BindingResult, Model, RedirectAttributes) : String` · `eliminar(Long, RedirectAttributes) : String` |
| **Privados** | `prepararFormulario(Model, CategoriaFormDto, String) : void` |
| **Implementa / extiende** | — |
| **Dependencias** | `CategoriaService` |
| **Relaciones** | `CategoriaController → CategoriaService` (1..1); usa `CategoriaFormDto` en formularios |
| **Estado** | Implementado. **Pruebas:** `MaestrosWebTest`, `SeguridadRolesTest` |

## 2.5 `UnidadMedidaController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Mantenimiento de unidades de medida. |
| **Atributos** | `- final UnidadMedidaService unidadMedidaService` (+ 2 constantes de vista) |
| **Constructores** | `UnidadMedidaController(UnidadMedidaService unidadMedidaService)` |
| **Métodos públicos** | `listar(Model)` · `nuevo(Model)` · `crear(UnidadMedidaFormDto, BindingResult, Model, RedirectAttributes)` · `editar(Long, Model, RedirectAttributes)` · `actualizar(Long, UnidadMedidaFormDto, BindingResult, Model, RedirectAttributes)` · `eliminar(Long, RedirectAttributes)` |
| **Implementa / extiende** | — |
| **Dependencias** | `UnidadMedidaService` |
| **Relaciones** | `→ UnidadMedidaService` (1..1); usa `UnidadMedidaFormDto` |
| **Estado** | Implementado. **Pruebas:** `MaestrosWebTest`, `SeguridadRolesTest` |

## 2.6 `ProductoController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Mantenimiento de productos, con desplegables de categoría y unidad de medida. |
| **Atributos** | `- final ProductoService productoService` · `- final CategoriaService categoriaService` · `- final UnidadMedidaService unidadMedidaService` |
| **Constructores** | `ProductoController(ProductoService, CategoriaService, UnidadMedidaService)` |
| **Métodos públicos** | `listar(Model)` · `nuevo(Model)` · `crear(ProductoFormDto, BindingResult, Model, RedirectAttributes)` · `editar(Long, Model, RedirectAttributes)` · `actualizar(Long, ProductoFormDto, BindingResult, Model, RedirectAttributes)` · `eliminar(Long, RedirectAttributes)` |
| **Implementa / extiende** | — |
| **Dependencias** | `ProductoService`, `CategoriaService`, `UnidadMedidaService` |
| **Relaciones** | `→ ProductoService` (1..1) · `→ CategoriaService` (1..1) · `→ UnidadMedidaService` (1..1); usa `ProductoFormDto` |
| **Estado** | Implementado. **Pruebas:** `MaestrosWebTest`, `SeguridadRolesTest` |

## 2.7 `MotivoMovimientoController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Mantenimiento de motivos de movimiento (datos maestros). |
| **Atributos** | `- final MotivoMovimientoService motivoMovimientoService` |
| **Constructores** | `MotivoMovimientoController(MotivoMovimientoService motivoMovimientoService)` |
| **Métodos públicos** | `listar(Model)` · `nuevo(Model)` · `crear(MotivoMovimientoFormDto, BindingResult, Model, RedirectAttributes)` · `editar(Long, Model, RedirectAttributes)` · `actualizar(Long, MotivoMovimientoFormDto, BindingResult, Model, RedirectAttributes)` · `eliminar(Long, RedirectAttributes)` |
| **Implementa / extiende** | — |
| **Dependencias** | `MotivoMovimientoService` |
| **Relaciones** | `→ MotivoMovimientoService` (1..1); usa `MotivoMovimientoFormDto` |
| **Estado** | Implementado. **Pruebas:** `MaestrosWebTest` (8), `SeguridadRolesTest` |

## 2.8 `MovimientoController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Historial de movimientos, detalle, registro de entradas/salidas y anulación. |
| **Atributos** | `- final MovimientoService movimientoService` · `- final InventarioService inventarioService` (+ 3 constantes de vista) |
| **Constructores** | `MovimientoController(MovimientoService movimientoService, InventarioService inventarioService)` |
| **Métodos públicos** | `listar(Long, String, String, String, String, Model) : String` · `nuevo(String, Model, RedirectAttributes) : String` · `crear(String, MovimientoFormDto, BindingResult, Model, Principal, RedirectAttributes) : String` · `detalle(Long, Model, RedirectAttributes) : String` · `anular(Long, AnulacionMovimientoFormDto, BindingResult, Model, Principal, RedirectAttributes) : String` |
| **Privados** | `prepararFormulario(Model, String) : void` |
| **Implementa / extiende** | — |
| **Dependencias** | `MovimientoService`, `InventarioService` |
| **Relaciones** | `→ MovimientoService` (1..1) · `→ InventarioService` (1..1); usa `MovimientoFormDto`, `AnulacionMovimientoFormDto`, `MovimientoDetalleFormDto` |
| **Estado** | Implementado. **Pruebas:** `MaestrosWebTest`, `MovimientoServiceTest` |

## 2.9 `InventarioController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Consulta de existencias actuales por producto. Solo lectura. |
| **Atributos** | `- final InventarioService inventarioService` |
| **Constructores** | `InventarioController(InventarioService inventarioService)` |
| **Métodos públicos** | `listar(Model) : String` → vista `inventario/lista` |
| **Implementa / extiende** | — |
| **Dependencias** | `InventarioService` |
| **Relaciones** | `→ InventarioService` (1..1) |
| **Estado** | Implementado. **Pruebas:** `MaestrosWebTest`, `SeguridadRolesTest` |

## 2.10 `UsuarioController`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Gestión de usuarios del sistema (alta, edición, consulta y desactivación). |
| **Atributos** | `- final UsuarioService usuarioService` |
| **Constructores** | `UsuarioController(UsuarioService usuarioService)` |
| **Métodos públicos** | `listar(Model)` · `nuevo(Model)` · `crear(UsuarioFormDto)` · `detalle(Long, Model)` · `editar(Long, Model)` · `actualizar(Long, UsuarioFormDto)` · `desactivar(Long)` |
| **Implementa / extiende** | — |
| **Dependencias** | `UsuarioService` |
| **Relaciones** | `→ UsuarioService` (1..1); usa `UsuarioFormDto` |
| **Estado** | Implementado. **Pruebas:** `SeguridadRolesTest` |

---

# 3. Capa de servicios — `pe.acmdiproyec.logistica.service`

## 3.1 `AutenticacionService`

| | |
| --- | --- |
| **Tipo** | Clase concreta (Spring `@Service`) |
| **Responsabilidad** | Recuperar un usuario activo para la autenticación; rechaza usuarios inactivos o sin rol activo. |
| **Atributos** | `- final UsuarioDao usuarioDao` |
| **Constructores** | `AutenticacionService(UsuarioDao usuarioDao)` |
| **Métodos públicos** | `buscarUsuarioActivo(String correo) : Usuario` |
| **Implementa / extiende** | — |
| **Dependencias** | `UsuarioDao` |
| **Relaciones** | Usado por `CustomUserDetailsService` (1..1) |
| **Estado** | Implementado. **Pruebas:** `SeguridadRolesTest` (usuario inactivo rechazado) |

## 3.2 `CategoriaService`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Reglas de negocio de categorías: duplicados de código/nombre, normalización y bloqueo de borrado con productos asociados. |
| **Atributos** | `- final CategoriaDao categoriaDao` · 3 constantes de mensaje (`MENSAJE_NO_ENCONTRADA`, `MENSAJE_DUPLICADO`, `MENSAJE_CON_PRODUCTOS`) |
| **Constructores** | `CategoriaService(CategoriaDao categoriaDao)` |
| **Métodos públicos** | `listarTodas() : List<Categoria>` · `obtenerFormulario(Long) : CategoriaFormDto` · `crear(CategoriaFormDto) : void` · `actualizar(Long, CategoriaFormDto) : void` · `eliminar(Long) : void` · `buscarCategoria(Long) : Categoria` |
| **Privados** | `validarDuplicados`, `normalizar`, `convertir`, `convertirFormulario` |
| **Implementa / extiende** | — |
| **Dependencias** | `CategoriaDao` |
| **Relaciones** | `→ CategoriaDao` (1..1) · `..> Categoria` · `..> CategoriaFormDto` |
| **Estado** | Implementado. **Pruebas:** `CategoriaServiceTest` (17) |

## 3.3 `UnidadMedidaService`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Reglas de negocio de unidades de medida: duplicados de código, nombre y abreviatura; bloqueo de borrado con productos. |
| **Atributos** | `- final UnidadMedidaDao unidadMedidaDao` · 3 constantes de mensaje |
| **Constructores** | `UnidadMedidaService(UnidadMedidaDao unidadMedidaDao)` |
| **Métodos públicos** | `listarTodas() : List<UnidadMedida>` · `obtenerFormulario(Long) : UnidadMedidaFormDto` · `crear(UnidadMedidaFormDto) : void` · `actualizar(Long, UnidadMedidaFormDto) : void` · `eliminar(Long) : void` · `buscarUnidadMedida(Long) : UnidadMedida` |
| **Implementa / extiende** | — |
| **Dependencias** | `UnidadMedidaDao` |
| **Relaciones** | `→ UnidadMedidaDao` (1..1) · `..> UnidadMedida` · `..> UnidadMedidaFormDto` |
| **Estado** | Implementado. **Pruebas:** `UnidadMedidaServiceTest` (15) |

## 3.4 `ProductoService`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Reglas de negocio de productos: validación de categoría y unidad existentes, duplicados y reglas de eliminación (movimientos, existencias, fila de inventario). |
| **Atributos** | `- final ProductoDao productoDao` · `- final CategoriaDao categoriaDao` · `- final UnidadMedidaDao unidadMedidaDao` · 5 constantes de mensaje |
| **Constructores** | `ProductoService(ProductoDao, CategoriaDao, UnidadMedidaDao)` |
| **Métodos públicos** | `listarTodos() : List<Producto>` · `obtenerFormulario(Long) : ProductoFormDto` · `crear(ProductoFormDto) : void` · `actualizar(Long, ProductoFormDto) : void` · `eliminar(Long) : void` · `buscarProducto(Long) : Producto` |
| **Privados** | `validarRelaciones`, `validarDuplicados`, `normalizar`, `convertir`, `convertirFormulario` |
| **Implementa / extiende** | — |
| **Dependencias** | `ProductoDao`, `CategoriaDao`, `UnidadMedidaDao` |
| **Relaciones** | `→ ProductoDao` (1..1) · `→ CategoriaDao` (1..1) · `→ UnidadMedidaDao` (1..1) |
| **Estado** | Implementado. **Pruebas:** `ProductoServiceTest` (20) |

## 3.5 `MotivoMovimientoService`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Reglas de negocio de motivos de movimiento: tipo ∈ {ENTRADA, SALIDA}, unicidad de código y de `nombre+tipo`, bloqueo de borrado con movimientos. |
| **Atributos** | `- final MotivoMovimientoDao motivoMovimientoDao` · 3 constantes de mensaje |
| **Constructores** | `MotivoMovimientoService(MotivoMovimientoDao motivoMovimientoDao)` |
| **Métodos públicos** | `listarTodos() : List<MotivoMovimiento>` · `listarPorTipo(String) : List<MotivoMovimiento>` · `obtenerFormulario(Long) : MotivoMovimientoFormDto` · `crear(MotivoMovimientoFormDto) : void` · `actualizar(Long, MotivoMovimientoFormDto) : void` · `eliminar(Long) : void` · `buscarMotivo(Long) : MotivoMovimiento` |
| **Privados** | `validarTipo`, `validarDuplicados`, `normalizar`, `convertir`, `convertirFormulario` |
| **Implementa / extiende** | — |
| **Dependencias** | `MotivoMovimientoDao` |
| **Relaciones** | `→ MotivoMovimientoDao` (1..1) · `..> MotivoMovimiento` |
| **Estado** | Implementado. **Pruebas:** `MotivoMovimientoServiceTest` (18) |

## 3.6 `MovimientoService`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Registro transaccional de movimientos (cabecera + detalles) y anulación condicional. **No escribe en `inventario`:** el stock lo mantiene el trigger de PostgreSQL. |
| **Atributos** | `+ static final String TIPO_ENTRADA` · `+ static final String TIPO_SALIDA` · `+ static final String ESTADO_VIGENTE` · `+ static final String ESTADO_ANULADO` · `- static final DateTimeFormatter FORMATO_CODIGO` · `- final MovimientoDao movimientoDao` · `- final MotivoMovimientoDao motivoMovimientoDao` · `- final ProductoDao productoDao` · `- final UsuarioDao usuarioDao` |
| **Constructores** | `MovimientoService(MovimientoDao, MotivoMovimientoDao, ProductoDao, UsuarioDao)` |
| **Métodos públicos** | `listarMovimientos(Long, String, String, String, String) : List<Movimiento>` · `obtenerMovimiento(Long) : Movimiento` · `listarMotivosPorTipo(String) : List<MotivoMovimiento>` · `registrar(MovimientoFormDto, String) : Long` *(**@Transactional**)* · `anular(Long, String, String) : void` *(**@Transactional**)* |
| **Privados** | `buscarMotivo`, `buscarUsuario`, `buscarMovimiento`, `normalizarMotivoAnulacion`, `validarLineas`, `validarStockParaSalidas`, `validarTipo`, `normalizar`, `convertirFecha`, `generarCodigo` |
| **Implementa / extiende** | — |
| **Dependencias** | `MovimientoDao`, `MotivoMovimientoDao`, `ProductoDao`, `UsuarioDao` |
| **Relaciones** | `→ MovimientoDao` (1..1) · `→ MotivoMovimientoDao` (1..1) · `→ ProductoDao` (1..1) · `→ UsuarioDao` (1..1); usa `Movimiento`, `MovimientoDetalle`, `MovimientoFormDto` |
| **Estado** | Implementado. **Pruebas:** `MovimientoServiceTest` (38) |

## 3.7 `InventarioService`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Consulta de existencias actuales. Solo lectura: el stock lo calcula la base de datos. |
| **Atributos** | `- final InventarioDao inventarioDao` |
| **Constructores** | `InventarioService(InventarioDao inventarioDao)` |
| **Métodos públicos** | `listarTodo() : List<Inventario>` |
| **Implementa / extiende** | — |
| **Dependencias** | `InventarioDao` |
| **Relaciones** | `→ InventarioDao` (1..1) |
| **Estado** | Implementado. **Pruebas:** `MaestrosWebTest` (consulta de inventario) |

## 3.8 `UsuarioService`

| | |
| --- | --- |
| **Tipo** | Clase concreta |
| **Responsabilidad** | Gestión de usuarios: hash BCrypt de contraseñas, alta, actualización, desactivación y conversión a DTO de respuesta. |
| **Atributos** | `- final UsuarioDao usuarioDao` · `- final PasswordEncoder passwordEncoder` |
| **Constructores** | `UsuarioService(UsuarioDao usuarioDao, PasswordEncoder passwordEncoder)` |
| **Métodos públicos** | `crearUsuario(Usuario, String) : void` · `actualizarUsuario(Usuario, String) : void` · `desactivarUsuario(Long) : void` · `listarUsuarios() : List<UsuarioResponseDto>` · `buscarUsuario(Long) : Usuario` · `listarRolesActivos() : List<Rol>` · `crearDesdeFormulario(UsuarioFormDto) : void` · `actualizarDesdeFormulario(Long, UsuarioFormDto) : void` |
| **Privados** | `convertirEntidad(UsuarioFormDto) : Usuario` · `crearRol(Long) : Rol` · `convertirRespuesta(Usuario) : UsuarioResponseDto` |
| **Implementa / extiende** | — |
| **Dependencias** | `UsuarioDao`, `PasswordEncoder` (Spring Security) |
| **Relaciones** | `→ UsuarioDao` (1..1) · `→ PasswordEncoder` (1..1, externo) · `..> UsuarioResponseDto` |
| **Estado** | Implementado. **Pruebas:** `SeguridadRolesTest` |

---

# 4. Persistencia — interfaces `pe.acmdiproyec.logistica.dao`

Patrón común: interfaz sin implementación, sin SQL ni dependencias. Cada una tiene **exactamente una** implementación en `dao.impl`.

| Interfaz | Métodos públicos | Implementación | Pruebas |
| --- | --- | --- | --- |
| **`CategoriaDao`** | `listarTodas() : List<Categoria>` · `buscarPorId(Long) : Optional<Categoria>` · `guardar(Categoria) : int` · `actualizar(Categoria) : int` · `eliminar(Long) : int` · `contarPorCodigo(String, Long) : int` · `contarPorNombre(String, Long) : int` · `contarProductosAsociados(Long) : long` | `JdbcCategoriaDao` | `CategoriaServiceTest` |
| **`UnidadMedidaDao`** | `listarTodas()` · `buscarPorId(Long)` · `guardar(UnidadMedida)` · `actualizar(UnidadMedida)` · `eliminar(Long)` · `contarPorCodigo(String, Long)` · `contarPorNombre(String, Long)` · `contarPorAbreviatura(String, Long)` · `contarProductosAsociados(Long) : long` | `JdbcUnidadMedidaDao` | `UnidadMedidaServiceTest` |
| **`ProductoDao`** | `listarTodos()` · `buscarPorId(Long)` · `guardar(Producto)` · `actualizar(Producto)` · `eliminar(Long)` · `contarPorCodigo(String, Long)` · `contarPorNombre(String, Long)` · `contarMovimientosAsociados(Long) : long` · `obtenerStockActual(Long) : BigDecimal` | `JdbcProductoDao` | `ProductoServiceTest` |
| **`MotivoMovimientoDao`** | `listarTodos()` · `listarPorTipo(String)` · `buscarPorId(Long)` · `guardar(MotivoMovimiento)` · `actualizar(MotivoMovimiento)` · `eliminar(Long)` · `contarPorCodigo(String, Long)` · `contarPorNombreYTipo(String, String, Long)` · `contarMovimientosAsociados(Long) : long` | `JdbcMotivoMovimientoDao` | `MotivoMovimientoServiceTest` |
| **`MovimientoDao`** | `listarTodos(Long, String, String, OffsetDateTime, OffsetDateTime) : List<Movimiento>` · `buscarPorId(Long) : Optional<Movimiento>` · `listarDetalles(Long) : List<MovimientoDetalle>` · `guardar(Movimiento) : Long` · `guardarDetalle(MovimientoDetalle) : int` · `anular(Long, Long, String) : int` | `JdbcMovimientoDao` | `MovimientoServiceTest` |
| **`InventarioDao`** | `listarTodo() : List<Inventario>` | `JdbcInventarioDao` | `MaestrosWebTest` |
| **`UsuarioDao`** | `buscarPorCorreo(String) : Optional<Usuario>` · `buscarPorId(Long) : Optional<Usuario>` · `listarTodos() : List<Usuario>` · `listarRolesActivos() : List<Rol>` · `guardar(Usuario) : int` · `actualizar(Usuario) : int` · `desactivar(Long) : int` | `JdbcUsuarioDao` | `SeguridadRolesTest` |

---

# 5. Persistencia — implementaciones `pe.acmdiproyec.logistica.dao.impl`

Patrón común: clase `@Repository` que implementa su interfaz DAO, recibe `JdbcTemplate` por constructor y ejecuta **consultas parametrizadas** (`?`). Ninguna contiene SQL concatenado con valores de usuario.

| Clase | Interfaz | Atributos | Constructores | Métodos públicos |
| --- | --- | --- | --- | --- |
| **`JdbcCategoriaDao`** | `CategoriaDao` | `- static final String SELECT_BASE` · `- final JdbcTemplate jdbcTemplate` | `JdbcCategoriaDao(JdbcTemplate)` | Los 8 de `CategoriaDao` |
| **`JdbcUnidadMedidaDao`** | `UnidadMedidaDao` | `- static final String SELECT_BASE` · `- final JdbcTemplate jdbcTemplate` | `JdbcUnidadMedidaDao(JdbcTemplate)` | Los 9 de `UnidadMedidaDao` |
| **`JdbcProductoDao`** | `ProductoDao` | `- static final String SELECT_BASE` · `- final JdbcTemplate jdbcTemplate` | `JdbcProductoDao(JdbcTemplate)` | Los 9 de `ProductoDao` |
| **`JdbcMotivoMovimientoDao`** | `MotivoMovimientoDao` | `- static final String SELECT_BASE` · `- final JdbcTemplate jdbcTemplate` | `JdbcMotivoMovimientoDao(JdbcTemplate)` | Los 9 de `MotivoMovimientoDao` |
| **`JdbcMovimientoDao`** | `MovimientoDao` | `- static final String SELECT_MOVIMIENTO` · `- static final String SELECT_DETALLE` · `- final JdbcTemplate jdbcTemplate` | `JdbcMovimientoDao(JdbcTemplate)` | Los 6 de `MovimientoDao` |
| **`JdbcInventarioDao`** | `InventarioDao` | `- static final String SELECT_BASE` · `- final JdbcTemplate jdbcTemplate` | `JdbcInventarioDao(JdbcTemplate)` | `listarTodo()` |
| **`JdbcUsuarioDao`** | `UsuarioDao` | `- static final String SELECT_BASE` · `- final JdbcTemplate jdbcTemplate` | `JdbcUsuarioDao(JdbcTemplate)` | Los 7 de `UsuarioDao` |

Métodos privados comunes: `contar(String, String, Long) : int` y `mapear(ResultSet)` (o `mapearMovimiento` / `mapearDetalle` en `JdbcMovimientoDao`, `mapearUsuario` en `JdbcUsuarioDao`).

> **Garantía estructural:** `JdbcMovimientoDao` **no contiene ninguna escritura sobre `inventario`**. El stock lo actualiza el trigger `trg_movimiento_detalle_aplicar_inventario`, y la reversión `trg_movimientos_revertir_anulacion`.

---

# 6. Dominio — `pe.acmdiproyec.logistica.model`

Modelos POJO con atributos privados, getters y setters. Sin anotaciones JPA. Detalle de multiplicidades en `backend-domain-model.puml`.

| Clase | Atributos (visibilidad · tipo) | Constructores | Relaciones justificadas |
| --- | --- | --- | --- |
| **`Rol`** | `- idRol : Long` · `- codigo : String` · `- nombre : String` · `- descripcion : String` · `- activo : boolean` | Ninguno explícito | `Rol "1" ← * Usuario` (FK `usuarios.id_rol` NOT NULL) |
| **`Usuario`** | `- idUsuario : Long` · `- rol : Rol` · `- codigo : String` · `- nombres : String` · `- apellidos : String` · `- correo : String` · `- passwordHash : String` · `- activo : boolean` | Ninguno explícito | `→ Rol` (1) · `Usuario "1" ← * Movimiento` (responsable) · `Usuario "0..1" ← * Movimiento` (anulación) |
| **`Categoria`** | `- idCategoria : Long` · `- codigo : String` · `- nombre : String` · `- descripcion : String` · `- activo : boolean` | Ninguno explícito | `Categoria "1" ← * Producto` (FK NOT NULL) |
| **`UnidadMedida`** | `- idUnidadMedida : Long` · `- codigo : String` · `- nombre : String` · `- abreviatura : String` · `- activo : boolean` | Ninguno explícito | `UnidadMedida "1" ← * Producto` (FK NOT NULL) |
| **`MotivoMovimiento`** | `+ static final String TIPO_ENTRADA` · `+ static final String TIPO_SALIDA` · `- idMotivoMovimiento : Long` · `- codigo : String` · `- nombre : String` · `- tipo : String` · `- descripcion : String` · `- activo : boolean` | Ninguno explícito | `MotivoMovimiento "1" ← * Movimiento` (FK NOT NULL) |
| **`Producto`** | `- idProducto : Long` · `- categoria : Categoria` · `- unidadMedida : UnidadMedida` · `- codigo : String` · `- nombre : String` · `- descripcion : String` · `- stockMinimo : BigDecimal` · `- activo : boolean` | Ninguno explícito | `→ Categoria` (1) · `→ UnidadMedida` (1) · `Producto "1" ← 0..1 Inventario` (FK UNIQUE) · `Producto "1" ← * MovimientoDetalle` |
| **`Inventario`** | `- idInventario : Long` · `- producto : Producto` · `- stock : BigDecimal` | Ninguno explícito | `→ Producto` (1) |
| **`Movimiento`** | `- idMovimiento : Long` · `- codigo : String` · `- motivoMovimiento : MotivoMovimiento` · `- usuario : Usuario` · `- fechaMovimiento : OffsetDateTime` · `- tipo : String` · `- estado : String` · `- observacion : String` · `- usuarioAnulacion : Usuario` · `- fechaAnulacion : OffsetDateTime` · `- motivoAnulacion : String` · `- detalles : List<MovimientoDetalle>` | Ninguno explícito | `→ MotivoMovimiento` (1) · `→ Usuario` (1 responsable, 0..1 anulación) · `Movimiento "1" ◆— * MovimientoDetalle` (composición: detalle inmutable y sin cabecera no existe) |
| **`MovimientoDetalle`** | `- idMovimientoDetalle : Long` · `- idMovimiento : Long` · `- producto : Producto` · `- cantidad : BigDecimal` · `- stockActual : BigDecimal` | Ninguno explícito | `→ Producto` (1) · `stockActual` **no se persiste** |

**Métodos públicos:** todos los modelos exponen getters y setters de sus campos, salvo `Movimiento`, que además declara `getTotalCantidad() : BigDecimal`.

---

# 7. Transferencia — `pe.acmdiproyec.logistica.dto`

DTO de formularios con validación Jakarta (`@NotBlank`, `@NotNull`, `@Size`, `@DecimalMin`, `@Digits`). **Ninguno declara constructor explícito** (usan el por defecto) ni implementa interfaces.

| DTO | Atributos y validación | Métodos | Uso | Estado |
| --- | --- | --- | --- | --- |
| **`CategoriaFormDto`** | `idCategoria : Long` · `codigo : String` `@NotBlank @Size(max=30)` · `nombre : String` `@NotBlank @Size(max=100)` · `descripcion : String` `@Size(max=250)` · `activo : boolean` | 10 (get/set) | `CategoriaController`, `CategoriaService` | Implementado. **Pruebas:** `ValidacionFormulariosTest` |
| **`UnidadMedidaFormDto`** | `idUnidadMedida : Long` · `codigo` `@NotBlank @Size(max=20)` · `nombre` `@NotBlank @Size(max=100)` · `abreviatura` `@NotBlank @Size(max=20)` · `activo : boolean` | 10 | `UnidadMedidaController`, `UnidadMedidaService` | Implementado. **Pruebas:** `ValidacionFormulariosTest` |
| **`ProductoFormDto`** | `idProducto` · `idCategoria` `@NotNull` · `idUnidadMedida` `@NotNull` · `codigo` `@NotBlank @Size(max=50)` · `nombre` `@NotBlank @Size(max=150)` · `descripcion` `@Size(max=500)` · `stockMinimo` `@NotNull @DecimalMin("0") @Digits(11,3)` · `activo` | 16 | `ProductoController`, `ProductoService` | Implementado. **Pruebas:** `ValidacionFormulariosTest` |
| **`MotivoMovimientoFormDto`** | `idMotivoMovimiento` · `codigo` `@NotBlank @Size(max=30)` · `nombre` `@NotBlank @Size(max=100)` · `tipo` `@NotBlank` · `descripcion` `@Size(max=250)` · `activo` | 12 | `MotivoMovimientoController`, `MotivoMovimientoService` | Implementado. **Pruebas:** `MotivoMovimientoServiceTest` |
| **`MovimientoFormDto`** | `idMotivoMovimiento` `@NotNull` · `observacion` `@Size(max=500)` · `lineas : List<MovimientoDetalleFormDto>` `@Valid` | 6 | `MovimientoController`, `MovimientoService` | Implementado. **Pruebas:** `MovimientoServiceTest`, `MaestrosWebTest` |
| **`MovimientoDetalleFormDto`** | `idProducto` `@NotNull` · `cantidad` `@NotNull @DecimalMin("0.001") @Digits(11,3)` | 4 | `MovimientoFormDto`, `MovimientoService` | Implementado. **Pruebas:** `MovimientoServiceTest` |
| **`AnulacionMovimientoFormDto`** | `motivoAnulacion : String` `@NotBlank @Size(max=500)` | 2 | `MovimientoController`, `MovimientoService` | Implementado. **Pruebas:** `MovimientoServiceTest`, `MaestrosWebTest` |
| **`UsuarioFormDto`** | `idRol : Long` · `codigo` · `nombres` · `apellidos` · `correo` · `password` · `activo` *(sin anotaciones de validación)* | 14 | `UsuarioController`, `UsuarioService` | Implementado. **Pruebas:** `SeguridadRolesTest` |
| **`UsuarioResponseDto`** | `idUsuario` · `codigo` · `nombres` · `apellidos` · `correo` · `rolCodigo` · `rolNombre` · `activo` | 16 | `UsuarioService.listarUsuarios()` | Implementado. **Pruebas:** `SeguridadRolesTest` |
| **`LoginDto`** ⚠️ | `correo : String` · `password : String` | 4 | **NINGUNO** | **SIN USO** — ver nota |

> ⚠️ **`LoginDto` es código sin uso.** Se comprobó que **ningún archivo `.java` ni `.html` del proyecto lo referencia** (única mención: su propia declaración). El formulario de login envía `username`/`password` directamente al procesador de Spring Security, sin DTO. **Se conserva sin eliminarlo**, conforme a la instrucción de no borrar clases existentes. Aparece marcado como *sin uso* en `backend-class-diagram.puml`.

> ⚠️ **`UsuarioFormDto` no tiene validación Jakarta**, a diferencia del resto de formularios. Es una asimetría heredada del módulo de usuarios; se documenta sin modificarla.

---

# 8. Seguridad — `pe.acmdiproyec.logistica.security`

## 8.1 `SecurityConfig`

| | |
| --- | --- |
| **Tipo** | Clase concreta (`@Configuration`) |
| **Responsabilidad** | Define el `PasswordEncoder` (BCrypt) y el `SecurityFilterChain`: rutas públicas, reglas por rol y método HTTP, formulario de login y logout. CSRF **habilitado**. |
| **Atributos** | Ninguno |
| **Constructores** | Ninguno explícito |
| **Métodos públicos** | `passwordEncoder() : PasswordEncoder` *(bean)* · `securityFilterChain(HttpSecurity) : SecurityFilterChain` *(bean)* |
| **Implementa / extiende** | — |
| **Dependencias** | `HttpSecurity` (Spring Security) |
| **Relaciones** | `..> PasswordEncoder` (define bean, consumido por `UsuarioService`) |
| **Estado** | Implementado. **Pruebas:** `SeguridadRolesTest` (19), `MaestrosWebTest` |

## 8.2 `CustomUserDetailsService`

| | |
| --- | --- |
| **Tipo** | Clase concreta (`@Service`) |
| **Responsabilidad** | Convierte un `Usuario` de la base de datos en un `UserDetails` de Spring Security, con la autoridad `ROLE_<codigo>` generada por `User.Builder.roles(...)`. |
| **Atributos** | `- final AutenticacionService autenticacionService` |
| **Constructores** | `CustomUserDetailsService(AutenticacionService autenticacionService)` |
| **Métodos públicos** | `loadUserByUsername(String correo) : UserDetails` *(lanza `UsernameNotFoundException`)* |
| **Implementa / extiende** | `implements UserDetailsService` (Spring Security) |
| **Dependencias** | `AutenticacionService` |
| **Relaciones** | `→ AutenticacionService` (1..1) · `..|> UserDetailsService` |
| **Estado** | Implementado. **Pruebas:** `SeguridadRolesTest` (conversión a `ROLE_ADMIN`/`ROLE_LOGISTICA`, hash BCrypt) |

---

# 9. Excepciones — `pe.acmdiproyec.logistica.exception`

## 9.1 `GlobalExceptionHandler`

| | |
| --- | --- |
| **Tipo** | Clase concreta (`@ControllerAdvice`) |
| **Responsabilidad** | Manejo centralizado de errores web: traduce cada excepción a su código HTTP y a la vista `error`, sin filtrar detalles internos. |
| **Atributos** | `- static final Logger LOGGER` · `- static final String VISTA_ERROR = "error"` |
| **Constructores** | Ninguno explícito |
| **Métodos públicos** | `manejarRecursoNoEncontrado(NoResourceFoundException, HttpServletResponse) : ModelAndView` → **404** · `manejarRegistroNoEncontrado(RecursoNoEncontradoException, …) : ModelAndView` → **404** · `manejarReglaDeNegocio(ReglaNegocioException, …) : ModelAndView` → **400** · `manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException, …) : ModelAndView` → **405** · `manejarArgumentoConTipoInvalido(MethodArgumentTypeMismatchException, …) : ModelAndView` → **400** · `manejarParametroAusente(MissingServletRequestParameterException, …) : ModelAndView` → **400** · `manejarErrorInesperado(Exception, …) : ModelAndView` → **500** |
| **Implementa / extiende** | — |
| **Dependencias** | `HttpServletResponse` (Jakarta) |
| **Relaciones** | `..> RecursoNoEncontradoException` · `..> ReglaNegocioException` |
| **Estado** | Implementado. **Pruebas:** `GlobalExceptionHandlerTest` (6) |

## 9.2 `RecursoNoEncontradoException`

| | |
| --- | --- |
| **Tipo** | Excepción propia |
| **Responsabilidad** | Señala un identificador de registro inexistente; permite responder 404 sin error de servidor. |
| **Atributos** | `- static final long serialVersionUID` |
| **Constructores** | `RecursoNoEncontradoException(String message)` |
| **Métodos públicos** | Ninguno declarado (hereda de `RuntimeException`) |
| **Implementa / extiende** | `extends RuntimeException` |
| **Relaciones** | Lanzada por servicios; capturada por controladores y `GlobalExceptionHandler` |
| **Estado** | Implementado. **Pruebas:** `CategoriaServiceTest`, `ProductoServiceTest`, `MovimientoServiceTest`, `MotivoMovimientoServiceTest`, `UnidadMedidaServiceTest`, `GlobalExceptionHandlerTest` |

## 9.3 `ReglaNegocioException`

| | |
| --- | --- |
| **Tipo** | Excepción propia |
| **Responsabilidad** | Señala una regla de negocio incumplida (duplicados, stock insuficiente, registros asociados, datos inválidos). |
| **Atributos** | `- static final long serialVersionUID` |
| **Constructores** | `ReglaNegocioException(String message)` |
| **Métodos públicos** | Ninguno declarado |
| **Implementa / extiende** | `extends RuntimeException` |
| **Relaciones** | Lanzada por servicios; capturada por controladores y `GlobalExceptionHandler` |
| **Estado** | Implementado. **Pruebas:** todas las clases de servicio + `GlobalExceptionHandlerTest` |

---

# 10. Tipos externos usados (no propios del proyecto)

Se listan porque aparecen en las relaciones del UML, pero **no son clases del repositorio**:

| Tipo | Procedencia | Uso |
| --- | --- | --- |
| `JdbcTemplate` | Spring JDBC | Inyectado en las 7 implementaciones DAO |
| `PasswordEncoder` / `BCryptPasswordEncoder` | Spring Security | Bean definido en `SecurityConfig`; usado por `UsuarioService` |
| `UserDetailsService` / `UserDetails` / `User` | Spring Security | Interfaz implementada por `CustomUserDetailsService` |
| `HttpSecurity` / `SecurityFilterChain` | Spring Security | Configuración de `SecurityConfig` |
| `RuntimeException` | JDK | Superclase de las excepciones propias |
| `Model`, `ModelAndView`, `RedirectAttributes`, `BindingResult` | Spring MVC | Parámetros de controladores |
| `Principal` | Jakarta Servlet | Usuario autenticado en `MovimientoController` |

---

# 11. Correspondencia con pruebas

| Clase de prueba | Cubre | Pruebas |
| --- | --- | ---: |
| `LogisticaApplicationTest` | `LogisticaApplication` | 1 |
| `IndexControllerTest` | `IndexController`, vistas y recursos estáticos | 4 |
| `SeguridadRolesTest` | `SecurityConfig`, `CustomUserDetailsService`, `AutenticacionService`, `UsuarioService`, `UsuarioController`, `AuthController`, `HomeController` | 19 |
| `MaestrosWebTest` | Controladores de categorías, unidades, productos, motivos, movimientos e inventario; permisos, CSRF, validación y errores HTTP | 61 |
| `CategoriaServiceTest` | `CategoriaService`, `CategoriaDao` | 17 |
| `UnidadMedidaServiceTest` | `UnidadMedidaService`, `UnidadMedidaDao` | 15 |
| `ProductoServiceTest` | `ProductoService`, `ProductoDao` | 20 |
| `MotivoMovimientoServiceTest` | `MotivoMovimientoService`, `MotivoMovimientoDao` | 18 |
| `MovimientoServiceTest` | `MovimientoService`, `MovimientoDao` | 38 |
| `ValidacionFormulariosTest` | Validación de los DTO | 16 |
| `GlobalExceptionHandlerTest` | `GlobalExceptionHandler` | 6 |
| `SupabaseConnectionIntegrationTest` | Esquema real (solo lectura) | 1 · **omitida** |
| **TOTAL** | | **216** |

> Las pruebas de servicios y web utilizan **dobles** de los DAO (`@MockitoBean` / `mock()`). No existe ninguna prueba que haya ejecutado SQL real contra PostgreSQL: `SupabaseConnectionIntegrationTest` se omite por falta de credenciales.

---

# 12. Verificación del catálogo

| Comprobación | Resultado |
| --- | --- |
| Ficheros `.java` en `src/main/java` (sin `package-info`) | **57** |
| Tipos documentados en este catálogo | **57** |
| Clases presentes en el código pero ausentes del catálogo | **0** |
| Clases inventadas o inexistentes en el código | **0** |
| Atributos / métodos / constructores | Extraídos del código fuente, sin inferencias |

### Correcciones aplicadas al UML existente

Al contrastar `backend-class-diagram.puml` con el código se detectaron **omisiones reales**, ya corregidas:

| Omisión detectada | Corrección |
| --- | --- |
| Faltaban los **10 DTO** (`CategoriaFormDto`, `UnidadMedidaFormDto`, `ProductoFormDto`, `MotivoMovimientoFormDto`, `MovimientoFormDto`, `MovimientoDetalleFormDto`, `AnulacionMovimientoFormDto`, `UsuarioFormDto`, `UsuarioResponseDto`, `LoginDto`) | Añadido el paquete `dto` con sus atributos y relaciones |
| Faltaba **`LogisticaApplication`** | Añadido el paquete de arranque |
| Los modelos no se declaraban en el diagrama general | Añadida nota que remite a `backend-domain-model.puml`, donde sí están completos con multiplicidades |
| No se indicaba que `LoginDto` está sin uso | Añadida nota explícita en el diagrama |

Tras la corrección, `backend-class-diagram.puml` declara **48 de 57** tipos directamente; los **9 modelos de dominio** se documentan íntegros en `backend-domain-model.puml`. **Entre ambos diagramas, las 57 clases están representadas.** El diagrama fue re-renderizado con PlantUML: `exit = 0`, sin errores de sintaxis.

### Confirmación

El catálogo **coincide exactamente con el código fuente**: 57 ficheros `.java` con tipo propio, 57 secciones documentadas, sin atributos, métodos, relaciones ni requisitos inventados. No se modificó la lógica de negocio ni la base de datos; no se realizó ningún commit ni push.
