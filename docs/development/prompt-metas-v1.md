# Prueba del prompt de metas v1

Fecha: 27 de octubre de 2026. Rama: `feature/prompt-metas-v1`.

## Alcance

Actualización posterior del 28 de octubre: se incorpora `scheduledDate` con contrato `assistant_tasks_v2` y migración Room 1 → 2. Véase [Día de trabajo independiente del aviso](scheduled-date.md). Las notas históricas de las primeras pruebas de esta página describen el contrato anterior, que no podía representar el día de trabajo sin hora.

El Asistente usa los dos bloques de [prompt-metas-consolidado-v1.md](../ideas/prompt-metas-consolidado-v1.md), copiados en `data/ai/GoalPlanningPrompt.kt`. El primer bloque guía la entrevista y el segundo adapta sus respuestas al contrato JSON vigente. Se mantienen Luna y Structured Outputs. La integración inicial conservaba el esquema de tareas; la revisión con `scheduledDate` lo amplía como se indica arriba.

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

### Revisión del 4 de noviembre: sesiones completas y dedicación acordada

Se elimina la autorización para agrupar las sesiones restantes sin enumerarlas. Todas las sesiones del periodo planificado deben existir como subtareas individuales, con fecha y duración; se pueden organizar bajo fases sin sustituirlas por un único bloque de «sesiones restantes». Las fases futuras aún sin concretar conservan su estimación provisional, y las dependencias desconocidas no reciben fechas ficticias.

La lectura, el estudio de un recurso, la preparación y la revisión forman parte del proyecto y de su carga. Si una sesión combina actividades, sus notas describen el reparto y el total. Cuando ese total supera la disponibilidad indicada, el asistente propone el cambio en la conversación y espera la aceptación; mientras tanto conserva el borrador anterior o continúa con `tasks: []` si todavía no hay propuesta. Los tiempos de los ejemplos son ilustrativos, no valores fijos para todas las metas.

Casos de aceptación para evaluar con el modelo real:

1. Una fase de cuatro semanas con frecuencia diaria contiene 28 objetos de sesión, cada uno con su fecha, aunque repitan dinámica. No aparece una tarea que sustituya las 26 últimas. Preparación y revisión adicional no cuentan como sesiones de práctica.
2. Con 20 minutos disponibles y un recurso que requiere lectura, se propone un reparto. Si se sugieren 10 minutos de lectura más 20 de práctica, se consultan los 30 minutos antes de incorporarlos al plan activo.
3. Tras aceptar ese reparto, cada sesión contiene ambas actividades y suma 30 minutos. Las 28 sesiones suman 14 horas, más cualquier preparación o revisión independiente explícita, sin duplicar cargas.
4. Si el usuario mantiene el límite de 20 minutos, se acuerda otro reparto o método dentro de ese límite; no se oculta la lectura como dedicación adicional.
5. Si aún no se dispone del recurso, se incluye conseguirlo y se marca la dependencia sin inventar páginas ni capítulos. Al editar, se conservan los identificadores y estados existentes y el resto de acuerdos válidos.

Esta modificación afecta a las instrucciones del modelo; no añade un generador determinista de recurrencias ni modifica automáticamente las tareas ya guardadas. Las pruebas de integración con respuestas simuladas no validan que el modelo real cumpla estos casos.

Verificación local: los dos bloques del Markdown coinciden exactamente con `GoalPlanningPrompt.text`. La ejecución completa de `:app:testDebugUnitTest` dio 228 pruebas superadas y un timeout de 5 segundos en `AtomicProposalSaveTest.successCommitsHierarchyBeforeUpdatingAnyReminder`, esperando la señal de finalización (línea 169). Las cinco pruebas de esa clase pasan al repetirla aisladamente, sin cambios de código ni de timeout. El test usa un repositorio de asistente simulado y no ejecuta el prompt; el fallo inicial queda documentado, no se considera corregido ni se presenta la suite completa como verde. La evidencia inicial está en `.artifacts/sesiones-completas/initial-timeout.xml`, fuera de Git. No se han realizado llamadas reales a la IA.

`assembleDebug` y `lintDebug` terminan correctamente; lint mantiene 0 errores, 44 advertencias y 1 sugerencia. Se instala el APK mediante `adb install -r` en el SHARP A103SH. Las huellas SHA-256 del DataStore de conversaciones, la base de datos de tareas y su WAL coinciden antes y después de instalar. La conversación y la meta guardada se conservan; el nuevo prompt se aplica a las siguientes peticiones. APK verificado: `fc3f4dea3bd555ffaddbec3d63a06fecb004684cf81ee4acd3de33d6d9677c3e`.

### Revisión del 29 de octubre: alcance y carga total

Pedir «la meta entera» completa el último alcance acordado con el usuario; no incorpora por inferencia fases que habían quedado fuera. El prompt estima el trabajo total por fases y lo traduce a horas, sesiones y semanas con la dedicación conocida. Permite repetir sesiones con el mismo recurso y método, agrupar las lejanas y fechar las próximas como parte del total, sin duplicar su carga. Si faltan materiales o nivel, explica los supuestos de una estimación provisional o el paso necesario para poder dimensionarla.

Los dos bloques del Markdown coinciden con `GoalPlanningPrompt.text`. `:app:testDebugUnitTest`, `:app:assembleDebug` y `:app:lintDebug` terminan correctamente: **221 pruebas, cero fallos, errores u omitidas**; lint mantiene 44 advertencias y 1 sugerencia, sin errores. No se añaden pruebas que busquen frases del prompt: las pruebas existentes verifican su integración y la conservación del historial, no la calidad de la respuesta del modelo.

Se evaluaron tres respuestas reales con `gpt-6-luna` y el mismo contrato JSON, sobre una copia autorizada de la conversación de prueba. La primera conservó el alcance, pero aplazó toda cuantificación. Tras aclarar que se permite una estimación de planificación con supuestos, las siguientes estimaron el recorrido completo en horas, sesiones y semanas. La última aún dejó inconsistente el total con la suma de fases y ambigua la inclusión de sesiones próximas; la precisión aritmética no se considera validada. Las respuestas y el script de evaluación se conservan solo en `.artifacts/`, fuera de Git; estas ejecuciones no modificaron el chat del móvil.

La verificación final se ejecutó en `.artifacts/prompt-workload-isolated-20261029`, actualizada al commit `07bc00b` para incluir también el arreglo de borrado `22f05e8` terminado en paralelo. Esta copia evita interferencias con otras compilaciones. Allí pasan **227 pruebas, cero fallos, errores u omitidas**, `assembleDebug` y `lintDebug` (0 errores, 44 advertencias y 1 sugerencia). Se instala ese APK con `adb install -r` en el SHARP A103SH: su SHA-256 coincide con el artefacto verificado, la app abre correctamente y la huella del DataStore de conversaciones permanece idéntica antes y después. La propuesta ya recibida se conserva; una nueva petición usa el prompt actualizado.

Revisión del prompt del 28 de octubre de 2026: los dos bloques del Markdown coinciden con `GoalPlanningPrompt.text`. Pasan las **9 pruebas de `StructuredAssistantIntegrationTest`**, sin fallos, errores ni omitidas, y `assembleDebug` termina correctamente. No se realizan llamadas reales a la IA. El APK queda preparado; ADB no detecta un móvil conectado y la instalación y evaluación del nuevo plan quedan pendientes.

Las pruebas del servicio cubren envío sin instrucciones añadidas, prompt vigente al restaurar historial, conservación de mensajes, envío durante una lectura pendiente, reinicio durante carga y apertura de tarea durante carga. Una prueba con DataStore real bajo Robolectric verifica que la clave experimental empieza vacía, persiste su conversación y conserva la clave anterior.

Estas pruebas utilizan respuestas simuladas. Verifican la integración y la persistencia; no prueban la calidad del plan generado por el modelo real.

Resultado del 27 de octubre de 2026: las 24 pruebas enfocadas y la suite completa de **177 pruebas** pasan, con cero fallos, errores y omitidas. `:app:assembleDebug` genera el APK correctamente. `:app:lintDebug` termina con 0 errores, 44 advertencias y 1 sugerencia; ninguna señala los archivos modificados en esta integración. Se incorporan seis pruebas nuevas. Cuatro de ellas se ejecutaron primero sobre la implementación anterior y fallaron antes de aplicar el arreglo.

En este Windows, la ejecución local de Gradle necesitó `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/ibuendiac/Documents/ChatGPT/ProcrastinApp/build` para evitar un error de conexión de la JVM al crear su canal interno. Se aplicó solo al proceso de verificación, sin modificar la configuración del proyecto.

Tras corregir la auditoría, `AssistantSessionLifecycleTest` añade **10 pruebas permanentes**: los dos casos originales, cancelación con propuesta histórica, reapertura de otra tarea, bloqueo de envíos mientras carga, cancelación de un envío, reinicio seguido de otro envío, respuesta y error tardíos, y orden de guardados. Las nueve primeras fallaron antes de aplicar el arreglo. La suite completa pasa ahora con **187 pruebas, 0 fallos, errores u omitidas**; `assembleDebug` y `lintDebug` terminan correctamente. La comprobación usa servicios de IA simulados y no instala la app en el móvil.
