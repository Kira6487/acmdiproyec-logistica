# Diagramas UML — Backend ACM Diproyec Logística

Este directorio contiene los diagramas de arquitectura del backend, generados a
partir de las **clases reales del repositorio** y no de una arquitectura
idealizada.

## 1. Arquitectura del backend

El backend es una aplicación **Spring Boot 3.5.7 sobre Java 21** que sigue una
arquitectura por capas con dependencias dirigidas hacia abajo:

```text
  Presentación   controller.web        → recibe solicitudes HTTP, valida y delega
        ↓
  Aplicación     service               → reglas de negocio y transacciones
        ↓
  Persistencia   dao  /  dao.impl      → interfaz + implementación JdbcTemplate
        ↓
  PostgreSQL     Supabase              → tablas, restricciones, funciones y triggers
```

Capas transversales: `security` (Spring Security), `exception` (errores
centralizados), `dto` (transferencia de datos de formularios) y `model`
(dominio).

### Decisiones de diseño verificadas en el código

| Decisión | Estado |
| --- | --- |
| Acceso a datos con `JdbcTemplate` y consultas parametrizadas | ✅ Sin JPA/Hibernate |
| El SQL vive solo en `dao.impl` | ✅ Ningún controlador contiene SQL |
| Las reglas de negocio viven en `service` | ✅ No duplicadas en controladores |
| Interfaces DAO con implementación JDBC concreta | ✅ 7 interfaces / 7 implementaciones |
| `model` y `dto` sin dependencias de otras capas | ✅ Sin ciclos de dependencias |
| Errores de negocio propios (`ReglaNegocioException`, `RecursoNoEncontradoException`) | ✅ Jerarquía sobre `RuntimeException` |
| Estados de movimiento (`VIGENTE`, `ANULADO`) y tipos (`ENTRADA`, `SALIDA`) | Son **ENUM de PostgreSQL**; Java los transporta como `String` |

### Responsabilidad del stock

El stock **no lo calcula ni lo escribe la aplicación**. Dos triggers de
PostgreSQL son los únicos responsables:

| Trigger | Efecto |
| --- | --- |
| `trg_productos_crear_inventario` | Crea la fila de `inventario` con `stock = 0` al insertar un producto |
| `trg_movimiento_detalle_aplicar_inventario` | Bloquea la fila con `FOR UPDATE`, valida el saldo y actualiza el stock al insertar cada detalle |
| `trg_movimientos_revertir_anulacion` | Revierte el stock al pasar de `VIGENTE` a `ANULADO` |

`JdbcMovimientoDao` **no contiene ninguna escritura sobre `inventario`**; es una
garantía estructural que impide la doble contabilización.

## 2. Diagramas incluidos

| Archivo | Contenido |
| --- | --- |
| `backend-class-diagram.puml` | **Diagrama de clases general.** Todas las clases concretas, interfaces, atributos y métodos públicos relevantes, y relaciones entre capas. |
| `backend-domain-model.puml` | **Modelo de dominio.** Entidades con sus atributos y tipos, asociaciones y **multiplicidades justificadas por las claves foráneas** del esquema. |
| `backend-packages.puml` | **Paquetes y dependencias.** Estructura por capas, dirección de las dependencias y ausencia de ciclos. |
| `backend-movimientos-module.puml` | **Detalle del módulo de movimientos.** Registro y anulación, con la interacción con los triggers de PostgreSQL. |

Si el diagrama general resulta demasiado grande, utilice el de dominio para la
estructura de datos y el del módulo de movimientos para el flujo crítico.

## 3. Cómo generar los diagramas

Los archivos usan **PlantUML**. No se añadió ninguna dependencia al proyecto:
PlantUML se ejecuta de forma externa.

### Opción A — extensión del editor (recomendada)

* **VS Code:** instale *PlantUML* (`jebbs.plantuml`) y pulse `Alt + D` sobre el
  archivo `.puml`.
* **IntelliJ IDEA:** instalé el plugin *PlantUML Integration*; la vista previa
  aparece a la derecha del editor.

### Opción B — línea de comandos con Java

PlantUML requiere Graphviz para el dibujo de algunas flechas. Descargue
`plantuml.jar` desde [plantuml.com](https://plantuml.com/download) y ejecute:

```powershell
# PNG
java -jar plantuml.jar -tpng docs\uml\*.puml -o docs\uml\img

# SVG (vectorial, recomendado para documentación)
java -jar plantuml.jar -tsvg docs\uml\*.puml -o docs\uml\img

# PDF
java -jar plantuml.jar -tpdf docs\uml\*.puml -o docs\uml\img
```

### Opción C — servidor en línea

Suba el contenido de cada `.puml` a
[https://www.plantuml.com/plantuml/uml](https://www.plantuml.com/plantuml/uml)
para obtener PNG o SVG sin instalación. **No incluya credenciales ni datos
reales** en los diagramas.

### Opción D — integración en la documentación

GitHub, GitLab y VS Code renderizan `.puml` de forma nativa o mediante
extensiones, de modo que los diagramas pueden consultarse sin exportar.

## 4. Notas de interpretación

* **Composición (`*--`)** se usa únicamente entre `Movimiento` y
  `MovimientoDetalle`: el detalle no tiene sentido sin su cabecera y la base de
  datos lo declara inmutable. El resto de relaciones son asociaciones (`-->`) o
  dependencias de inyección.
* **Multiplicidades** proceden de las claves foráneas y restricciones de las
  migraciones (`supabase/migrations/`), no de suposiciones.
* **`LoginDto`** aparece en el diagrama de clases marcado como *sin uso*: está
  en el repositorio pero ninguna clase lo referencia. Se conserva por decisión
  de no eliminar código existente.
* **`controller.api`, `config` y `util`** solo contienen `package-info.java`. Se
  representan como paquetes reservados; **no se crearon clases para rellenarlos**.
* **Reportes y exportación a Excel** no están implementados: el alcance actual
  no tiene requisitos verificables ni plantillas asociadas, y la exportación
  requeriría una dependencia nueva (Apache POI).
* **La tabla `auditoria`** se alimenta mediante trigger de PostgreSQL y no tiene
  clase Java porque no existe un caso de uso confirmado de consulta.

## 5. Correspondencia con el código

Los diagramas reflejan el estado del proyecto en la rama
`feature/autenticacion-usuarios`. Cada relación mostrada existe como inyección
de constructor en el código fuente; cada atributo coincide con el campo
declarado en su clase. Si se modifica una clase, actualice el diagrama
correspondiente para mantener la trazabilidad.
