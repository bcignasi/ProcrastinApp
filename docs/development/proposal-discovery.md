# Acceso a Propuesta desde Conversación

En `feature/prompt-metas-v1`, Conversación muestra un único botón «Ver propuesta» al final de los mensajes cuando hay tareas y la respuesta ha terminado. El desplazamiento automático incluye el botón. Pulsarlo selecciona la pestaña existente y conserva el borrador de escritura; no guarda tareas ni envía mensajes.

La pestaña Propuesta muestra un punto del color principal de Serena cuando su contenido todavía no se ha consultado. Tiene una descripción accesible y funciona tanto en la cabecera normal como en la compacta. No parpadea ni provoca navegación automática.

`AssistantViewModel` conserva en memoria la última lista de tareas vista. La compara por contenido, incluidos descendientes, fechas y notas, con el plan actual. Un comentario nuevo que no cambia las tareas no reactiva el indicador. El estado se limpia al retirar la propuesta, reiniciar, cancelar o finalizar el guardado que la retira. Una actualización se reconoce como vista cuando la pantalla Propuesta está seleccionada, la respuesta ha terminado y el ciclo de vida está en RESUMED. Un reconocimiento tardío de una versión anterior no reconoce el plan nuevo.

El botón permanece disponible al regresar a Conversación después de consultar el plan. El estado «visto» dura lo que vive el ViewModel: no se persiste entre reinicios del proceso. No se modifican el prompt, el contrato JSON ni la base de datos.

Las pruebas cubren acceso con conservación del borrador, desplazamiento hasta la acción en una conversación larga, indicador de cambios y comentarios sin cambios, reconocimiento desde ambos accesos y mientras Propuesta está abierta, carga, limpieza, guardado, cancelación, reinicio y cabecera compacta en español.

Verificación: la prueba del botón falló antes de implementarlo. La suite completa pasa con **201 pruebas, cero fallos, errores u omitidas**; se añaden siete pruebas permanentes. `assembleDebug` y `lintDebug` terminan correctamente (44 advertencias y 1 sugerencia de lint). La versión se instala con `adb install -r` y se abre correctamente en el Xiaomi M2102J20SG, sin desinstalar ni reiniciar la conversación. Incluye el cambio previo de `scheduledDate`. La revisión visual y la evaluación de respuestas reales corresponden al usuario.
