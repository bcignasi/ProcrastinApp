# Prueba del prompt de metas v1

Fecha: 27 de octubre de 2026. Rama: `feature/prompt-metas-v1`.

## Alcance

El Asistente usa los dos bloques de [prompt-metas-consolidado-v1.md](../ideas/prompt-metas-consolidado-v1.md), copiados en `data/ai/GoalPlanningPrompt.kt`. El primer bloque guía la entrevista y el segundo adapta sus respuestas al contrato JSON vigente. Se mantienen Luna, Structured Outputs y el esquema de tareas.

`ChatAIService` deja de añadir a cada mensaje del usuario la orden antigua de generar tareas. Al restaurar una conversación carga las instrucciones actuales y conserva los mensajes de usuario y asistente; descarta las instrucciones y fechas antiguas del sistema.

## Historial separado y arranque

`MessageStorageImpl` admite una clave de historial. `AppModule` selecciona `task_messages_metas_v1` para este experimento dentro del DataStore existente. La clave anterior, `task_messages`, no se lee ni se sobrescribe durante la prueba. No hay migración de tareas ni cambios en Room.

El primer arranque del experimento muestra una conversación vacía. Los siguientes recuperan sus mensajes. El historial anterior permanece almacenado, aunque no hay selector de historial en la interfaz. Volver a la configuración anterior permite leer su clave original.

Se corrige el orden de arranque de AUD-003: cargar antes de guardar, esperar la inicialización al enviar o abrir una tarea, y evitar que una lectura tardía restaure una conversación que el usuario ya ha reiniciado. Las escrituras y solicitudes reciben copias de la lista. Esto no resuelve AUD-004: una respuesta de IA ya pendiente durante un reinicio sigue siendo un problema separado.

## Cómo probar en el móvil

1. Abrir esta carpeta y rama en Android Studio y ejecutar la app sobre la instalación existente. No desinstalar ni borrar datos.
2. Entrar directamente en Asistente, sin abrir una tarea antigua para editarla, y comenzar con una meta nueva.
3. Comprobar que pregunta lo necesario, recomienda una vía y recursos adecuados y acuerda la dedicación antes de cerrar el plan.
4. Revisar la propuesta antes de guardarla. Las tareas nuevas siguen guardándose en la Agenda real: el aislamiento es del historial, no de la base de datos.
5. Para otra simulación, utilizar el menú del Asistente → Reiniciar conversación. Esto reinicia el chat experimental, no recupera el historial antiguo.

La revisión visual y la evaluación de respuestas reales corresponden al usuario. La integración no añade búsqueda web: el modelo no puede verificar cursos, precios o enlaces en directo. Tampoco añade recurrencias de calendario ni cambia los fallos de edición de tareas documentados aparte; empezar con una meta nueva.

## Comprobaciones automatizadas

Las pruebas del servicio cubren envío sin instrucciones añadidas, prompt vigente al restaurar historial, conservación de mensajes, envío durante una lectura pendiente, reinicio durante carga y apertura de tarea durante carga. Una prueba con DataStore real bajo Robolectric verifica que la clave experimental empieza vacía, persiste su conversación y conserva la clave anterior.

Estas pruebas utilizan respuestas simuladas. Verifican la integración y la persistencia; no prueban la calidad del plan generado por el modelo real.

Resultado del 27 de octubre de 2026: las 24 pruebas enfocadas y la suite completa de **177 pruebas** pasan, con cero fallos, errores y omitidas. `:app:assembleDebug` genera el APK correctamente. `:app:lintDebug` termina con 0 errores, 44 advertencias y 1 sugerencia; ninguna señala los archivos modificados en esta integración. Se incorporan seis pruebas nuevas. Cuatro de ellas se ejecutaron primero sobre la implementación anterior y fallaron antes de aplicar el arreglo.

En este Windows, la ejecución local de Gradle necesitó `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/ibuendiac/Documents/ChatGPT/ProcrastinApp/build` para evitar un error de conexión de la JVM al crear su canal interno. Se aplicó solo al proceso de verificación, sin modificar la configuración del proyecto.
