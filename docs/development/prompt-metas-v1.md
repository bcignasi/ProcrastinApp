# Prueba del prompt de metas v1

Fecha: 27 de octubre de 2026. Rama: `feature/prompt-metas-v1`.

## Alcance

El Asistente usa los dos bloques de [prompt-metas-consolidado-v1.md](../ideas/prompt-metas-consolidado-v1.md), copiados en `data/ai/GoalPlanningPrompt.kt`. El primer bloque guía la entrevista y el segundo adapta sus respuestas al contrato JSON vigente. Se mantienen Luna, Structured Outputs y el esquema de tareas.

`ChatAIService` deja de añadir a cada mensaje del usuario la orden antigua de generar tareas. Al restaurar una conversación carga las instrucciones actuales y conserva los mensajes de usuario y asistente; descarta las instrucciones y fechas antiguas del sistema.

## Historial separado y arranque

`MessageStorageImpl` admite una clave de historial. `AppModule` selecciona `task_messages_metas_v1` para este experimento dentro del DataStore existente. La clave anterior, `task_messages`, no se lee ni se sobrescribe durante la prueba. No hay migración de tareas ni cambios en Room.

El primer arranque del experimento muestra una conversación vacía. Los siguientes recuperan sus mensajes. El historial anterior permanece almacenado, aunque no hay selector de historial en la interfaz. Volver a la configuración anterior permite leer su clave original.

Se corrige el orden de arranque de AUD-003: cargar antes de guardar, esperar la inicialización al enviar o abrir una tarea, y evitar que una lectura tardía restaure una conversación que el usuario ya ha reiniciado. Las escrituras y solicitudes reciben copias de la lista. La corrección posterior de la auditoría cubre también AUD-004, como se describe a continuación.

### Cancelación y reinicio durante la carga

La auditoría independiente reprodujo dos problemas: una edición cancelada reaparecía al terminar la carga y un envío que esperaba al historial sobrevivía al reinicio. Sus reproducciones forman parte de `AssistantSessionLifecycleTest`.

- `ChatAIService` identifica cada apertura de edición con `editRevision`. Cancelar, abrir otra tarea o reiniciar invalida la apertura pendiente. `cancelPendingEdit()` no borra el historial.
- `AssistantRepository.clearTasks()` invalida esa apertura y deja de reconstruir propuestas a partir de emisiones pendientes del historial. Una apertura explícita, un nuevo mensaje o un reinicio vuelven a permitir propuestas. Así tampoco reaparece una propuesta histórica tras cancelar.
- Cada envío tiene un `requestToken`. Reiniciar lo invalida; sus respuestas, errores y finalización ya no modifican la sesión siguiente. La carga se activa antes de esperar al historial y solo se admite un envío activo por sesión.
- `stateLock` protege conjuntamente las comprobaciones y modificaciones en memoria entre IO y Main. No se mantiene durante la llamada a la IA ni durante la escritura en disco. `persistenceMutex` ordena las escrituras y obtiene una copia actualizada después de las anteriores.

El prompt, el modelo, el contrato JSON y las tareas de Agenda mantienen su comportamiento. La invalidación no garantiza cancelar en el servidor una petición que ya haya empezado.

## Cómo probar en el móvil

### Revisión del 28 de octubre: método, opciones y sesiones

El prompt y los dos bloques de su Markdown se actualizan con las instrucciones aprobadas tras la primera prueba del teórico. Se sustituyen indicaciones genéricas por opciones numeradas con respuesta libre, elección del método y recurso antes de cerrar el plan, propuesta de días y horizonte revisable, detalle de las sesiones próximas y una dedicación coherente en toda la propuesta. El práctico queda como contexto futuro cuando solo se acuerda planificar el teórico.

Las opciones siguen siendo texto dentro de `comentario`. Se conservan el modelo, Structured Outputs, el esquema, la clave de historial y las tareas existentes. No se añaden búsqueda web, botones de respuesta ni recurrencias. Los días sin hora se describen en `notes`; no se inventan horas ni avisos para rellenar los campos de fecha.

Para comparar esta revisión, actualizar la app sobre la instalación existente y usar **Reiniciar conversación** desde el menú del Asistente antes de empezar una meta nueva. Esta acción descarta el chat experimental actual; no elimina las tareas de Agenda. Debe realizarla el usuario cuando esté listo, no durante la instalación. Al arrancar, el servicio ya reemplaza las instrucciones de sistema guardadas por el prompt vigente, aunque mantiene el contenido anterior de usuario y asistente hasta ese reinicio.

Comprobar especialmente que la entrevista permite contestar con números, distingue estudiar por cuenta propia de hacerlo con la autoescuela, concreta el recurso sin atribuirse verificaciones inexistentes y produce pasos con método, días y duración. La evaluación de esas respuestas reales sigue pendiente; las pruebas de integración solo comprueban la conexión del prompt y el contrato de la app.

### Recorrido de prueba

1. Abrir esta carpeta y rama en Android Studio y ejecutar la app sobre la instalación existente. No desinstalar ni borrar datos.
2. Entrar directamente en Asistente, sin abrir una tarea antigua para editarla, y comenzar con una meta nueva.
3. Comprobar que pregunta lo necesario, recomienda una vía y recursos adecuados y acuerda la dedicación antes de cerrar el plan.
4. Revisar la propuesta antes de guardarla. Las tareas nuevas siguen guardándose en la Agenda real: el aislamiento es del historial, no de la base de datos.
5. Para otra simulación, utilizar el menú del Asistente → Reiniciar conversación. Esto reinicia el chat experimental, no recupera el historial antiguo.

La revisión visual y la evaluación de respuestas reales corresponden al usuario. La integración no añade búsqueda web: el modelo no puede verificar cursos, precios o enlaces en directo. Tampoco añade recurrencias de calendario ni cambia los fallos de edición de tareas documentados aparte; empezar con una meta nueva.

## Comprobaciones automatizadas

Revisión del prompt del 28 de octubre de 2026: los dos bloques del Markdown coinciden con `GoalPlanningPrompt.text`. Pasan las **9 pruebas de `StructuredAssistantIntegrationTest`**, sin fallos, errores ni omitidas, y `assembleDebug` termina correctamente. No se realizan llamadas reales a la IA. El APK queda preparado; ADB no detecta un móvil conectado y la instalación y evaluación del nuevo plan quedan pendientes.

Las pruebas del servicio cubren envío sin instrucciones añadidas, prompt vigente al restaurar historial, conservación de mensajes, envío durante una lectura pendiente, reinicio durante carga y apertura de tarea durante carga. Una prueba con DataStore real bajo Robolectric verifica que la clave experimental empieza vacía, persiste su conversación y conserva la clave anterior.

Estas pruebas utilizan respuestas simuladas. Verifican la integración y la persistencia; no prueban la calidad del plan generado por el modelo real.

Resultado del 27 de octubre de 2026: las 24 pruebas enfocadas y la suite completa de **177 pruebas** pasan, con cero fallos, errores y omitidas. `:app:assembleDebug` genera el APK correctamente. `:app:lintDebug` termina con 0 errores, 44 advertencias y 1 sugerencia; ninguna señala los archivos modificados en esta integración. Se incorporan seis pruebas nuevas. Cuatro de ellas se ejecutaron primero sobre la implementación anterior y fallaron antes de aplicar el arreglo.

En este Windows, la ejecución local de Gradle necesitó `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/ibuendiac/Documents/ChatGPT/ProcrastinApp/build` para evitar un error de conexión de la JVM al crear su canal interno. Se aplicó solo al proceso de verificación, sin modificar la configuración del proyecto.

Tras corregir la auditoría, `AssistantSessionLifecycleTest` añade **10 pruebas permanentes**: los dos casos originales, cancelación con propuesta histórica, reapertura de otra tarea, bloqueo de envíos mientras carga, cancelación de un envío, reinicio seguido de otro envío, respuesta y error tardíos, y orden de guardados. Las nueve primeras fallaron antes de aplicar el arreglo. La suite completa pasa ahora con **187 pruebas, 0 fallos, errores u omitidas**; `assembleDebug` y `lintDebug` terminan correctamente. La comprobación usa servicios de IA simulados y no instala la app en el móvil.
