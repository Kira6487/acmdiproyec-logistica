# Contribuir al proyecto

## Flujo de ramas

- `main` se reserva para versiones estables y no recibe commits directos.
- `develop` es la rama de integración del equipo.
- Cada cambio futuro parte de `develop` y se desarrolla en una rama descriptiva
  `feature/...`, `fix/...` o equivalente.
- La integración se realiza mediante pull requests dirigidos a `develop` y
  revisados por al menos otro integrante.
- Solo los cambios estables y revisados pasan de `develop` a `main` mediante pull
  request.

## Commits

Use mensajes breves en modo imperativo y uno de estos prefijos:

- `feat:` para funcionalidades.
- `fix:` para correcciones.
- `docs:` para documentación.
- `test:` para pruebas.
- `refactor:` para reorganizaciones sin cambio funcional.
- `chore:` para mantenimiento y configuración.

No incluya credenciales, archivos `.env`, tokens ni secretos en commits.

## Pull requests

- Ejecute `./mvnw test` y `./mvnw package` antes de abrir o actualizar un pull
  request.
- Describa el alcance, las pruebas realizadas y cualquier paso manual pendiente.
- Mantenga cada pull request enfocado en un solo módulo o propósito. No mezcle
  autenticación, maestros, movimientos y reportes en el mismo cambio.
- Solicite revisión y atienda las observaciones antes de integrar.
- No modifique migraciones ya aplicadas; agregue una migración posterior.
- Confirme nuevamente que el diff no contiene secretos ni datos personales.

Consulte `docs/GUIA_DESARROLLO_EQUIPO.md` para conocer los límites de cada capa y
la distribución sugerida del trabajo.
