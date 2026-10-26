# Guía breve de desarrollo del equipo

Esta guía define los límites entre capas y módulos. No reemplaza la revisión de
código ni autoriza cambios directos sobre `main` o migraciones ya aplicadas.

## Arquitectura

```text
Controller web/API
        ↓
Service
        ↓
DAO
        ↓
PostgreSQL/Supabase
```

- Los controladores reciben solicitudes, validan la entrada y delegan.
- Los servicios concentran las reglas de negocio y las transacciones.
- Los DAO acceden a PostgreSQL mediante Spring JDBC.
- Los DTO transportan información entre capas; los modelos representan el
  dominio.

## Responsabilidades por módulo

| Módulo | Alcance |
| --- | --- |
| Autenticación y usuarios | Login, sesiones, usuarios y roles `ADMIN`/`LOGISTICA` |
| Datos maestros | Categorías, unidades, productos y motivos |
| Movimientos | Entradas, salidas, validación y stock atómico |
| Consultas y reportes | Stock, historial, anulaciones, alertas y Excel |

Las vistas reservadas para estos módulos permanecen en
`templates/{auth,maestros,movimientos,inventario,reportes}`. No deben conectarse
directamente a Supabase ni contener reglas de acceso a datos.

## Convenciones

- Un controlador no ejecuta SQL.
- Un DAO no contiene reglas visuales ni genera respuestas HTTP.
- Los servicios contienen las reglas de negocio.
- Los DTO transportan información entre capas.
- Las vistas Thymeleaf no se conectan directamente a Supabase.
- No se almacenan costos ni ventas y el sistema maneja un único almacén.
- Ninguna operación puede dejar stock negativo.
- Una migración aplicada no se modifica. Todo cambio futuro de base de datos se
  incorpora mediante una migración nueva.
- Las credenciales y archivos `.env` no se guardan en Git.
- Nombres, mensajes y comentarios se mantienen en español de forma consistente.

Ejemplo de nombres esperados para datos maestros, sin implicar que deban crearse
antes de implementar el módulo:

```text
ProductoController
ProductoService
ProductoDao
JdbcProductoDao
ProductoDto
```

Evite generar clases vacías. Cada clase debe incorporarse con una responsabilidad
concreta y pruebas proporcionales a su alcance.

## Componentes de interfaz disponibles

Los fragmentos compartidos se encuentran en `templates/fragments/`. La hoja
`static/css/styles.css` proporciona tarjetas, botones, formularios, campos de
cantidad, mensajes, insignias, filtros, tablas responsive, estados vacíos e
indicadores de carga. Reutilice esas clases antes de crear variantes nuevas.

## Distribución sugerida

- Integrante 1: autenticación y usuarios.
- Integrante 2: datos maestros.
- Integrante 3: movimientos e inventario.
- Integrante 4: consultas, alertas y reportes.
- Eduardo Contreras: integración, revisión de arquitectura y coordinación.

Esta distribución no crea ni reserva ramas. Cada trabajo futuro debe seguir el
flujo descrito en `CONTRIBUTING.md`.
