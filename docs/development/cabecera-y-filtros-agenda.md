# Cabecera Serena y filtros de Agenda

Decisión del 19 de octubre de 2026: mostrar Hoy / Pendientes / Hechas, con Hoy como vista inicial. Los demás filtros se conservan en el código, sin exponerlos en la cabecera.

## De la pestaña a la consulta

- Hoy utiliza `TaskListQueryType.TODAY`: pendientes cuyo aviso (`notify`) está entre hoy, incluido, y mañana, excluido. `TaskDao.getTodayReminderTasks()` selecciona por la misma fecha que muestra la fila.
- Hoy contiene una única lista, sin secciones Hoy/Atrasadas. Las tareas atrasadas, futuras y sin aviso continúan disponibles en Pendientes.
- Pendientes utiliza ALL: todas las tareas principales pendientes, incluidas las que no tienen fecha.
- Hechas utiliza COMPLETED.
- NOW conserva su intervalo original de hoy y mañana. Los filtros de prioridad, aviso y sin fecha no se eliminan.

`TaskRepository.getTasks(filter, date)` permite usar una fecha concreta en consultas y pruebas; por defecto usa la fecha local actual. No cambia el esquema de Room ni requiere migración.

## Jerarquía existente

Las consultas seleccionan tareas principales (`parentTaskId IS NULL`) y el repositorio carga sus subtareas completas. Una subtarea con aviso de hoy cuyo padre no corresponde a Hoy no aparece como tarea independiente. Se conserva ese comportamiento; no se implementa una agenda transversal de subtareas.

## Comprobaciones

Las pruebas locales utilizan Room en memoria bajo Robolectric. La revisión visual de la cabecera, las pestañas y las listas corresponde a Ignasi en el móvil mediante Run de Android Studio.

## Estado y pantalla

`TaskListViewModel` combina la pestaña elegida y la fecha local. Observa una única consulta por pestaña y conserva el resultado en `tasks`; ya no consulta PAST adicionalmente ni transporta `overdueTasks`. `TaskListUiState` transporta también el filtro y la fecha para que cabecera y filas representen la misma consulta.

La fecha se revisa cada minuto mientras hay suscripción a la pantalla. Si no cambia, `distinctUntilChanged()` evita repetir las consultas. Al volver tras una pausa prolongada, la suscripción vuelve a leer la fecha. Se puede proporcionar una fecha controlada al ViewModel para probar medianoche sin esperar tiempo real.

Al cambiar de filtro se limpia la selección, evitando completar o eliminar tareas seleccionadas que ya no están en la lista visible. Volver desde Ajustes o Asistente conserva la pestaña mediante el `rememberSaveable` existente.

La cabecera utiliza la marca, Tu agenda, fecha localizada y engranaje. Las tres pestañas usan semántica de selección y colores/formas compartidos del tema. Las tareas conservan sus acciones, presentación FLAT/CARDS y expansión de subtareas.

Las claves de las filas utilizan el identificador estable de tarea. `TaskList` ya no necesita construir secciones.

## Validación técnica

- Seis pruebas con Room real en memoria: límites del día para avisos, fecha sin hora, exclusión de completadas/sin aviso, subtareas, completar, paso de diciembre a enero y fechas de límite/aviso distintas. NOW y ALL conservan su comportamiento.
- Tres pruebas del ViewModel: inicio en Hoy sin consultar PAST, cambio de pestaña sin selección oculta y actualización tras medianoche sin escrituras en la base de datos.
- Las pruebas de este cambio fallaron primero con el comportamiento anterior: cinco del repositorio, una del ViewModel y dos de las filas.
- Resultado final: `assembleDebug`, 118 pruebas locales sin fallos ni omitidas, `lintDebug` con 0 errores y 22 advertencias y `compileDebugAndroidTestKotlin` correctos. No se ejecuta la app en el móvil durante estas comprobaciones.
- Para ver los cambios en el teléfono, ejecutar Run en Android Studio. La revisión visual permanece pendiente de Ignasi.

## Fecha visible y prioridad

En las filas FLAT de Agenda se muestra solo la fecha/hora del aviso con su campana. Si no existe aviso, no se sustituye por la fecha límite: la tarea sigue accesible en Pendientes. El campo `deadline` permanece guardado y los filtros internos que lo utilizaban se conservan.

Las prioridades alta y urgente se representan como texto en la esquina superior derecha, sin icono ni fondo. «Prioridad alta» usa el recurso localizado `task_priority_high_label` y el coral `SerenaPriorityHigh` (#F28F87) en Serena oscuro; en claro utiliza `error` del tema para mantener contraste. «Urgente» conserva su recurso localizado y usa `error`. Ambas usan `labelMedium` y respetan las mayúsculas del recurso. Las tarjetas del Asistente mantienen su representación anterior.
