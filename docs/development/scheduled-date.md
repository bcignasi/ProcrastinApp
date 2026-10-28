# Día de trabajo independiente del aviso

28 de octubre de 2026. Rama: `feature/prompt-metas-v1`.

## Contrato y alcance

`Task.scheduledDate` contiene un día ISO `yyyy-MM-dd`, sin hora, o `""` cuando no está planificado. Es independiente de `deadline` (fecha límite) y `notify` (recordatorio). El contrato de respuestas nuevas pasa a `assistant_tasks_v2`: exige el campo en cada tarea y valida fechas reales, también en descendientes. El lector tolerante del historial conserva mensajes antiguos y normaliza la ausencia del campo a vacío.

El prompt propone días concretos según la disponibilidad y representa las sesiones próximas como subtareas. Las fechas del borrador son propuestas corregibles, no eventos guardados. Las fases condicionadas a sucesos desconocidos pueden seguir sin fecha. Los dos bloques del Markdown del prompt se mantienen iguales al texto Kotlin.

Propuesta muestra el día junto al título de las filas y bajo la cabecera del objetivo, con traducciones en español e inglés. Los avisos mantienen su presentación independiente. No se añaden calendario, selector manual de fecha, duración estructurada ni cambios en el filtro Hoy: este último sigue consultando avisos de tareas raíz. Para modificar una fecha en este paso, se solicita el cambio al Asistente.

## Persistencia y compatibilidad

Room pasa de versión 1 a 2 mediante `AppDatabase.MIGRATION_1_2`, registrada en `AppModule`. Se añade la columna con valor vacío y se elimina el fallback destructivo. No se infieren días a partir de notas ni se reescriben conversaciones o tareas antiguas.

El índice único incorpora `scheduledDate`, para permitir sesiones con el mismo título y fechas de trabajo distintas aunque no tengan límite ni aviso. Conserva la restricción anterior cuando el día está vacío. No permite dos sesiones con idéntica combinación de título, límite, aviso y día; cambiar esa regla general queda fuera de este paso.

## Comprobaciones

- Contrato: día vacío y fechas válidas, incluidos años bisiestos; rechazo de fechas imposibles, formatos imprecisos y horas dentro de `scheduledDate`.
- Historial: ausencia o null del campo antiguo se convierten a vacío, conservando días presentes en subtareas.
- Migración: esquema v1 congelado con padre e hijo, estados, notas y avisos; apertura con Room v2, actualización y reapertura sin perder datos.
- Persistencia: guardar y recuperar sesiones anidadas con el mismo título en días distintos; no programar alarmas por tener solo un día de trabajo.
- Interfaz: día visible sin desplegar detalles y coexistencia con un aviso, en español e inglés.

La prueba real del móvil requiere una respuesta nueva: la propuesta anterior no recibe fechas automáticamente al actualizar. Puede pedirse al Asistente que distribuya las próximas sesiones de esa propuesta con fechas concretas, o comenzar otra conversación. No se reinicia el historial durante la instalación.

Resultado de la verificación: la prueba de aceptación del campo falló con el contrato anterior. Tras el cambio pasan las 40 pruebas enfocadas y la suite completa de **194 pruebas**, con cero fallos, errores u omitidas; se añaden siete pruebas permanentes. `assembleDebug` y `lintDebug` terminan correctamente (44 advertencias y 1 sugerencia de lint). Se comprueba la igualdad del prompt Kotlin con los dos bloques de Markdown. No se realizan llamadas reales a la IA. ADB no detecta un móvil al finalizar, por lo que la instalación y la evaluación de respuestas reales quedan pendientes.

Actualización posterior: instalada sobre la copia existente del Xiaomi M2102J20SG junto con [el acceso a Propuesta](proposal-discovery.md). La app se abre correctamente; la evaluación del plan con fechas sigue siendo una prueba manual del usuario.
