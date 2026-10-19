# Cabecera Serena y filtros de Agenda

Decisión del 19 de octubre de 2026: mostrar Hoy / Pendientes / Hechas, con Hoy como vista inicial. Los demás filtros se conservan en el código, sin exponerlos en la cabecera.

## De la pestaña a la consulta

- Hoy utiliza el nuevo `TaskListQueryType.TODAY`: pendientes cuya fecha límite está entre hoy, incluido, y mañana, excluido. Reutiliza `TaskDao.getNowTasks()` con límites distintos a los de NOW.
- Atrasadas se muestra como sección de Hoy utilizando PAST, con la misma fecha de referencia.
- Pendientes utiliza ALL: todas las tareas principales pendientes, incluidas las que no tienen fecha.
- Hechas utiliza COMPLETED.
- NOW conserva su intervalo original de hoy y mañana. Los filtros de prioridad, aviso y sin fecha no se eliminan.

`TaskRepository.getTasks(filter, date)` permite usar una fecha concreta en consultas y pruebas; por defecto usa la fecha local actual. No cambia el esquema de Room ni requiere migración.

## Jerarquía existente

Las consultas seleccionan tareas principales (`parentTaskId IS NULL`) y el repositorio carga sus subtareas completas. Una subtarea con fecha de hoy cuyo padre no corresponde a Hoy no aparece como tarea independiente. Este incremento conserva ese comportamiento; no implementa una agenda transversal de subtareas.

## Comprobaciones

Las pruebas locales utilizan Room en memoria bajo Robolectric. La revisión visual de la cabecera, las pestañas y las listas corresponde a Ignasi en el móvil mediante Run de Android Studio.

## Estado y pantalla

`TaskListViewModel` combina la pestaña elegida y la fecha local. En Hoy observa dos consultas (TODAY y PAST) y conserva sus resultados en `tasks` y `overdueTasks`; en las demás pestañas solo observa la lista correspondiente. `TaskListUiState` transporta también el filtro y la fecha para que la cabecera y las secciones representen la misma consulta.

La fecha se revisa cada minuto mientras hay suscripción a la pantalla. Si no cambia, `distinctUntilChanged()` evita repetir las consultas. Al volver tras una pausa prolongada, la suscripción vuelve a leer la fecha. Se puede proporcionar una fecha controlada al ViewModel para probar medianoche sin esperar tiempo real.

Al cambiar de filtro se limpia la selección, evitando completar o eliminar tareas seleccionadas que ya no están en la lista visible. Volver desde Ajustes o Asistente conserva la pestaña mediante el `rememberSaveable` existente.

La cabecera utiliza la marca, Tu agenda, fecha localizada y engranaje. Las tres pestañas usan semántica de selección y colores/formas compartidos del tema. Las tareas conservan sus acciones, presentación FLAT/CARDS y expansión de subtareas.

Las claves de las filas combinan sección e identificador de tarea, evitando colisiones durante las emisiones independientes de ambas consultas.

## Validación técnica

- Cinco pruebas con Room real en memoria: límites del día, fecha sin hora, exclusión de completadas/sin fecha/solo aviso, subtareas, completar y paso de diciembre a enero. NOW y ALL conservan su comportamiento.
- Tres pruebas del ViewModel: inicio en Hoy, sección de atrasadas aun sin tareas para hoy, cambio de pestaña sin selección oculta y actualización tras medianoche sin escrituras en la base de datos.
- Las pruebas se ejecutaron primero con el comportamiento ausente: tres fallos del repositorio y tres del ViewModel. Tras implementar, la suite completa pasó: 102 pruebas, cero fallos y cero errores.
- App y pruebas instrumentadas compiladas; estas últimas no se ejecutaron en el móvil. Lint sin errores y con las 29 advertencias anteriores.
- Para ver los cambios en el teléfono, ejecutar Run en Android Studio. La revisión visual permanece pendiente de Ignasi.
