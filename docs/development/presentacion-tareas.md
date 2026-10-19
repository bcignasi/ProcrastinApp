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

`rememberSaveable(task.id)` sigue guardando el estado de expandir/contraer en el mismo componente, y la lista conserva las claves por identificador de tarea. Los títulos, fechas, prioridades, notas y botones conservaron inicialmente el contenido existente. La cabecera plana se pule en el incremento descrito a continuación.

## Filas de la maqueta original de Serena

Contrato actualizado: casilla a la izquierda, título en seminegrita, una única fecha/hora correspondiente al aviso con campana, y prioridad alta o urgente como texto en la esquina superior derecha, sin icono ni fondo. «Prioridad alta» usa coral `SerenaPriorityHigh` (#F28F87) en oscuro y el color `error` del tema en claro; «Urgente» usa `error`. La tipografía es `labelMedium` (12 sp). El título admite varias líneas. Los identificadores internos desaparecen de la cabecera plana y los separadores existentes permanecen. Este incremento representa tareas reales, sin añadir proyectos ni barras de progreso.

`TaskItemContent` utiliza `FlatTaskHeader` solo en `FLAT`. El modo `CARDS` conserva `ShowUrgentRow`, `ShowTitleRow` y `ShowDatesRow`; no se alteran sus componentes. Las notas, periodicidad, acciones y recursión de subtareas siguen compartidas. La cabecera plana conserva una flecha independiente para desplegar; no necesita la segunda flecha inferior de tarjetas.

`Checkbox(checked = task.completed, onCheckedChange = { onComplete(task) })` reutiliza el callback que ya llegaba desde `TaskListScreen` al ViewModel. La casilla no guarda estado propio: Room actualiza la tarea y la pantalla recibe los datos. La selección múltiple por pulsación larga continúa siendo distinta de completar. Completar un padre no completa sus subtareas.

`FlatTaskDateText` muestra solo `HH:mm` cuando la fecha del aviso es hoy y contiene una hora explícita. Para otros días conserva fecha y hora, con fecha localizada. Una fecha sin hora no inventa las 00:00, y un dato inválido se conserva visible en lugar de sustituirse por la fecha actual. `uiState.date` se pasa como `referenceDate` hasta las tareas y subtareas para renovar también estas etiquetas al cambiar el día. `FlatTaskHeader` ya no muestra la fecha límite ni mezcla prioridad y fecha en una línea; si falta aviso, no hay fecha en la fila.

Se reutiliza la dependencia de pruebas Compose en `testImplementation` para ejecutar interacciones con Robolectric en el ordenador, sin dispositivo. Las pruebas comprueban completar/desmarcar sin abrir edición ni seleccionar, completar una subtarea sin completar al padre, conservar tarjetas sin casillas y refrescar metadatos al cambiar el día. Otras pruebas verifican hoy, otras fechas, medianoche, fechas sin hora y datos inválidos.

Validación del incremento del 19 de octubre: las pruebas de interacción fallaron inicialmente en los dos recorridos que requerían casillas; las de formato fallaron en cuatro casos antes de implementar el formateador. Después pasan las cuatro de interacción y las cinco de formato, junto con la suite completa de 116 pruebas, sin fallos ni omitidas. `assembleDebug`, `lintDebug` (0 errores, 22 advertencias y 1 sugerencia) y `compileDebugAndroidTestKotlin` correctos. Ignasi comprueba el aspecto en el móvil; no se instala ni se ejecuta la app en el dispositivo durante esta validación.

## Comprobación en el móvil

Ignasi realiza la revisión visual. Con `FLAT` activo, ejecutar desde Android Studio y comprobar la lista en claro y oscuro, mantener pulsada una tarea y expandir sus subtareas. El Asistente mantiene las tarjetas. Para revisar tarjetas en Agenda, cambiar únicamente el valor indicado arriba y volver a ejecutar; no hace falta tocar ni restaurar datos.

El futuro selector de Ajustes pasará este mismo valor a los componentes. Las dos presentaciones permanecen activas en el código, sin bloques comentados ni pantallas duplicadas.

Verificación técnica: `assembleDebug`, las 94 pruebas unitarias existentes y `lintDebug` correctos. Lint mantiene 0 errores y las 29 advertencias anteriores. No se añaden pruebas que dupliquen las constantes visuales; la comprobación del aspecto en ambos modos queda a cargo de Ignasi.
