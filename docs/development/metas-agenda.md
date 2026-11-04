# Metas en la agenda

4 de noviembre de 2026. Rama: `codex/metas-agenda`.

La pestaña «Hechas» se sustituye por «Metas». Cada tarea raíz es una meta, incluidas las completadas y las que todavía no tienen subtareas. Se muestran primero las pendientes, manteniendo el orden de creación dentro de cada estado. No se migra ni se reclasifica la base de datos.

Tocar el título despliega a la vez la descripción completa, las fechas y frecuencia disponibles y las subtareas. Las fases anidadas mantienen su propio despliegue. «Editar meta» abre el objetivo completo en el asistente; mantener pulsado conserva las acciones de selección existentes. Completar una sesión no completa automáticamente la meta.

«Pendientes» incorpora los filtros «Por hacer» y «Completadas», y recuerda el filtro elegido al cambiar de pestaña. «Por hacer» conserva la consulta anterior de pendientes. «Completadas» consulta tareas a cualquier profundidad y las presenta como filas independientes, sin duplicar descendientes ni incluir hijos pendientes. Desde su casilla se pueden devolver a pendientes. «Hoy» mantiene su comportamiento.

El diseño conserva la cabecera, tipografía, colores y navegación Serena existentes. La vista sirve para consultar y operar sobre los objetivos, con filas plegables y los componentes Material de la app, en español e inglés.

## Verificación

- Se reprodujo primero el problema del filtro anterior con Room real: dos subtareas completadas bajo una raíz pendiente devolvían una lista vacía. La prueba `completedFilterIncludesNestedTasksOnceWithoutPendingDescendants` falló antes del cambio.
- `AgendaFiltersTest` cubre raíces, jerarquía completa, metas sin pasos, estados de completado independientes y filtro plano de completadas sin duplicados.
- `GoalListTest` cubre descripción completa, acceso a las 28 sesiones, edición separada del despliegue, metas sin subtareas, restauración del despliegue y completar/reabrir sesiones sin completar la meta.
- `GoalsNavigationTest` cubre las tres pestañas y el filtro de completadas dentro de Pendientes.

Las capturas de revisión y otros artefactos locales se conservan en `.artifacts/metas-agenda/`, fuera de Git.

Resultado: **235 pruebas superadas**, sin fallos, errores ni omitidas; `assembleDebug` y `lintDebug` correctos (0 errores, 44 advertencias y 1 sugerencia de lint). Tras la revisión visual se añade margen inferior para que el botón flotante no tape las últimas notas; las cuatro pruebas de Metas/navegación vuelven a pasar, junto con compilación y lint.

Revisado en el SHARP A103SH con la meta real: desplegar la descripción, recorrer las 30 sesiones existentes y consultar el filtro vacío de completadas. Se comprueban el tema oscuro y el claro con fuente al 130 %, y se restauran tema oscuro y escala 1.0. El APK definitivo se instala con `adb install -r`; las huellas del historial, la base de datos y su WAL coinciden antes y después. No se modifican tareas ni mensajes durante la revisión. La prueba instrumentada de la cabecera se actualiza, pero no se ejecuta como suite de instrumentación en el móvil.
