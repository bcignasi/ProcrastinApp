# Propuesta Serena: objetivos y pasos

Incremento del 20 de octubre de 2026. Referencia: pantalla derecha de la maqueta Serena «Agenda primero» compartida por Ignasi.

## Contrato visual

Propuesta permite leer los objetivos y todos sus pasos antes de guardar en Agenda. Cada objetivo con subtareas tiene un título, contador localizado y barra fina de progreso. Debajo aparecen filas con círculos numerados y conexiones verticales discontinuas. Las tareas completadas utilizan una superficie verde del tema; las pendientes conservan el fondo. Se reutilizan DM Sans, colores semánticos y formas de Serena, también en modo claro.

Los pasos se muestran directamente. Los niveles adicionales mantienen su sangría y números como `2.1` y `2.2`. Las tareas sin subtareas usan una fila independiente sin inventar contador ni progreso. El contador considera exclusivamente los hijos directos: `countCompletedSubtasks()` / `countAllSubtasks()`. No calcula esfuerzo, tiempo ni el estado de los nietos.

Las casillas son indicadores de solo lectura (`onCheckedChange = null`). El estado accesible de cada fila incluye «Completada/Pendiente. Solo lectura» con traducción inglesa. Los números y los estados aportan información sin depender únicamente del color. Los títulos y los metadatos permiten varias líneas; no se fija la altura de las filas ni se recortan los textos.

La fila muestra solo la fecha del aviso mediante el formateador ya usado en Agenda. Notas, repetición, fecha límite y prioridad siguen accesibles en un bloque «Detalles» desplegable. La cabecera también conserva ahí su aviso, si lo tiene. Los botones de Detalles tienen etiquetas accesibles con el título de la tarea para distinguirlos. La fecha de referencia procede del mismo `today` de `AssistantScreen`, que se renueva cada minuto.

Este incremento conserva el JSON, `Task.subtasks`, los ViewModels, la base de datos y las acciones de guardar/cancelar. La cabecera general «Revisa tu plan», los comentarios y el acabado de los botones de Propuesta quedan para los siguientes pasos visuales. La reorganización funcional de tareas y subtareas se decidirá después.

## Código

- `AssistantTasksViewListItem`: elige entre fila independiente y cabecera seguida de pasos.
- `ProposalGoalHeader`: título, contador pluralizado y progreso real de los hijos directos.
- `ProposalStepRow`: fila de lectura, aviso, indicador de estado, marcador y presentación recursiva de descendientes.
- `ProposalTaskDetails`: despliegue local de los metadatos existentes; no modifica tareas.
- `AssistantTasksViewList`: mantiene la lista desplazable y separa los objetivos 16 dp.
- `AssistantScreen` y `AssistantTasksView`: propagan la fecha compartida; no cambian acciones ni navegación.
- Recursos españoles e ingleses: plurales del contador, estado de lectura, detalles y etiquetas de fechas/prioridad.

## Verificación

`ProposalTaskPresentationTest` contiene cinco pruebas permanentes Compose/Robolectric, sin servicios externos:

1. Objetivo de diez pasos, accesibles sin expandir; un completado produce `1 de 10 pasos` y progreso `0.1`.
2. Tres niveles con todos los IDs a cero: conserva títulos, numeración y jerarquía; cuenta los hijos directos.
3. Filas completadas y pendientes sin acción de completar; pulsarlas conserva los estados y el contador.
4. Tarea independiente: muestra el aviso, mantiene la fecha límite en Detalles y no crea barra de progreso.
5. Inglés, modo claro y fuente al 150 %: contador singular, títulos largos y acceso a notas, repetición, fecha límite y prioridad mediante Detalles.

Las cinco pruebas fallaron con la presentación anterior. Registros locales: `.artifacts/serena-proposal-rows-red.log`, `.artifacts/serena-proposal-rows-green.log` y `.artifacts/serena-proposal-rows-validation.log`.

Validación final: 148 pruebas aprobadas, sin fallos, errores ni omitidas; `assembleDebug`, `compileDebugAndroidTestKotlin` y `lintDebug` correctos. Lint conserva 41 advertencias y un aviso informativo anteriores, sin errores ni advertencias nuevas. El plural español incluye las categorías `one`, `many` y `other`.

Ignasi realiza la revisión visual en el móvil mediante Run en Android Studio: objetivo real con diez pasos, casillas de lectura, desplazamiento, detalles y ambos temas.
