# Presentación de tareas: plana y tarjetas

## Una misma tarea y dos apariencias

Se conservan las `Card` exterior e interior y la misma función que representa el contenido. `TaskPresentation`, en `ui/common/TaskPresentation.kt`, solo describe su presentación visual:

```kotlin
enum class TaskPresentation {
    FLAT,
    CARDS
}
```

No cambia `Task`, sus campos, la relación de subtareas, la base de datos ni las acciones de completar, editar y borrar.

## Dónde se elige

En `ui/tasklist/elements/TaskListContent.kt`, Agenda pasa:

```kotlin
TaskList(
    tasks = uiState.tasks,
    presentation = TaskPresentation.FLAT,
    // El resto de argumentos existentes se conserva.
)
```

Para recuperar la presentación anterior en Agenda, basta con cambiar ese valor a `TaskPresentation.CARDS`. Los componentes reciben `CARDS` como valor por defecto, por lo que el Asistente conserva su aspecto actual. No se añade todavía un selector en Ajustes ni se guarda una preferencia de presentación.

El modo claro/oscuro sigue determinando los colores de MaterialTheme. La presentación plana/tarjetas es una elección visual independiente.

## Cómo se aplica

| Archivo | Responsabilidad |
|---|---|
| `TaskList.kt` | Pasa la presentación a las tareas. En plana usa separación de 0 dp y líneas de 1 dp entre tareas; en tarjetas conserva los huecos de 8 dp. |
| `TaskItemSelectable.kt` | Conserva la `Card`, las pulsaciones y la selección. En plana usa fondo transparente, forma rectangular y elevación de 0 dp; en tarjetas conserva los colores por fecha y las esquinas del tema. |
| `TaskItemContent.kt` | Conserva la `Card` interior y su contenido. Ajusta forma y márgenes según la presentación y pasa el mismo valor al dibujar las subtareas. |

Cada componente calcula `val isFlat = presentation == TaskPresentation.FLAT`. Ese booleano decide los parámetros visuales de la misma `Card`; no selecciona una implementación distinta de la tarea.

Una tarea seleccionada conserva su fondo destacado y borde en ambas presentaciones. La presentación describe el aspecto; la selección sigue indicando sobre qué tareas se aplican acciones.

`rememberSaveable(task.id)` sigue guardando el estado de expandir/contraer en el mismo componente, y la lista conserva las claves por identificador de tarea. Los títulos, fechas, prioridades, notas y botones siguen utilizando el contenido existente. Su jerarquía visual se pulirá en otro incremento.

## Comprobación en el móvil

Ignasi realiza la revisión visual. Con `FLAT` activo, ejecutar desde Android Studio y comprobar la lista en claro y oscuro, mantener pulsada una tarea y expandir sus subtareas. El Asistente mantiene las tarjetas. Para revisar tarjetas en Agenda, cambiar únicamente el valor indicado arriba y volver a ejecutar; no hace falta tocar ni restaurar datos.

El futuro selector de Ajustes pasará este mismo valor a los componentes. Las dos presentaciones permanecen activas en el código, sin bloques comentados ni pantallas duplicadas.

Verificación técnica: `assembleDebug`, las 94 pruebas unitarias existentes y `lintDebug` correctos. Lint mantiene 0 errores y las 29 advertencias anteriores. No se añaden pruebas que dupliquen las constantes visuales; la comprobación del aspecto en ambos modos queda a cargo de Ignasi.
