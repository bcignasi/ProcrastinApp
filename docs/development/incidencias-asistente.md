# Incidencias conocidas del Asistente

Auditoría del 22 de octubre de 2026 sobre el commit `10249f3`, en `feature/serena-agenda`. Se revisaron historial, sesiones, apertura de tareas, guardado de propuestas y avisos, además del contrato JSON y la selección de modelo. Incluyó una revisión independiente y pruebas locales de reproducción.

**Estado: cinco incidencias confirmadas y pendientes de corrección.** Ignasi decide documentarlas y posponer su arreglo. Las soluciones de este documento son propuestas; no están implementadas ni constituyen un requisito para empezar el siguiente trabajo del prompt.

Los fallos son anteriores a Structured Outputs y al cambio a Luna. No se encontró una regresión concreta en esos dos cambios. La auditoría no hizo llamadas a OpenAI ni accedió al móvil, y no verifica la calidad de planes reales ni la disponibilidad del modelo en la cuenta.

## Seguimiento

P1 indica prioridad alta por riesgo de pérdida de datos. P2 indica prioridad media; el impacto y las condiciones concretas se describen en cada incidencia. Los identificadores se conservan al corregirlas para poder enlazar commits y pruebas.

| ID | Prioridad | Incidencia | Estado |
|---|---|---|---|
| [AUD-001](#aud-001-guardado-de-ediciones-sin-reemplazo-atómico) | P1 | Pérdida de la tarea original si falla su reemplazo | Pendiente |
| [AUD-002](#aud-002-eliminación-de-tareas-que-no-se-pudieron-cargar) | P2 | Guardar una edición parcial elimina tareas no cargadas | Pendiente |
| [AUD-003](#aud-003-el-arranque-puede-sobrescribir-el-historial) | P2 | Escritura inicial antes de recuperar la conversación | Pendiente |
| [AUD-004](#aud-004-respuesta-antigua-dentro-de-una-sesión-nueva) | P2 | Una respuesta pendiente reaparece después del reinicio | Pendiente |
| [AUD-005](#aud-005-avisos-de-subtareas-eliminadas) | P2 | Alarmas de descendientes sin cancelar | Pendiente |

## AUD-001: guardado de ediciones sin reemplazo atómico

**Impacto.** Se pueden perder una tarea y sus subtareas al guardar una edición. También se descarta la propuesta, impidiendo reintentar con el mismo borrador.

**Código implicado.** `ui/assistant/AssistantViewModel.kt`, `commitTasksFromAssistant()`; `data/repository/TaskRepository.kt`, `deleteTasks()` y `saveTask()`. Las referencias de código de este documento parten de `app/src/main/java/com/ibc/procrastinapp/`.

**Causa.** El ViewModel borra los originales antes de insertar sus sustitutos. Cada `saveTask()` tiene una transacción propia que no incluye ese borrado anterior. Después se limpian los IDs y la propuesta incluso si alguna inserción falla. La señal de finalización permite volver a Agenda también en ese caso.

**Cómo reproducirlo.**

1. Abrir una tarea existente para editarla y preparar una propuesta modificada.
2. Provocar un error al insertar el reemplazo. Un posible desencadenante es que colisione con otra tarea por el índice único `(title, deadline, notify)` de `TaskEntity`, con inserción `ABORT` en `TaskDao`.
3. Guardar la propuesta: la original ya se ha eliminado, la sustituta falla y el borrador se vacía.

**Evidencia.** `failedReplacementMustKeepTheOriginalTaskAndTheDraft` reprodujo el rechazo de inserción mediante un repositorio simulado con estado persistido. Observó `original=null` y `draft=[]`. El caso concreto de colisión se identificó por lectura del índice y la política de inserción, no mediante una llamada a la API.

**Corrección propuesta.** Añadir al repositorio una operación que reemplace todos los originales por toda la propuesta, incluidas sus subtareas, dentro de una única transacción de Room. Propagar los errores para que se revierta el conjunto. Limpiar el borrador y emitir la navegación de éxito solo después de confirmar el guardado; ante error, conservar propuesta e IDs para corregir o reintentar y mostrar un mensaje localizado.

Separar las escrituras de base de datos de la programación de alarmas: Room no puede revertir cambios en las alarmas de Android. Actualizar los avisos después de confirmar la transacción y distinguir un fallo al configurar avisos de un fallo al guardar datos. El tratamiento de todos los descendientes se relaciona con AUD-005.

**Criterios de cierre.** Pruebas permanentes con Room en memoria que fuercen un fallo en una tarea raíz y en una subtarea: deben conservar todas las originales, impedir inserciones parciales y mantener el borrador. Una prueba del ViewModel debe comprobar ausencia de navegación de éxito ante error y un reintento correcto. El recorrido satisfactorio debe seguir guardando el conjunto y limpiando la propuesta.

## AUD-002: eliminación de tareas que no se pudieron cargar

**Impacto.** Guardar una edición múltiple puede eliminar una tarea existente que el usuario no llegó a revisar en esa propuesta, aunque las escrituras terminen correctamente.

**Código implicado.** `ui/assistant/AssistantViewModel.kt`, `loadTasksForEditing()`, especialmente `originalTaskIds.addAll(taskIds)`, y `commitTasksFromAssistant()`.

**Causa.** Se registran para borrar todos los IDs solicitados, aunque solo se hayan recuperado algunas tareas. Mostrar el error de carga no impide guardar las tareas que sí llegaron.

**Cómo reproducirlo.** Seleccionar A y B para edición; dejar que A se cargue y provocar un error transitorio al leer B, que sigue existiendo en la base de datos. Guardar la propuesta que contiene únicamente A. Se eliminan A y B, pero solo se reinserta A.

**Evidencia.** `partialLoadMustNeverDeleteAnExistingTaskThatWasNotLoaded` mantuvo B en un almacenamiento simulado y falló únicamente su lectura. Tras guardar A, B había desaparecido.

**Corrección propuesta.** Bloquear el reemplazo mientras la carga sea incompleta, o registrar exclusivamente los IDs realmente cargados si se decide permitir la edición parcial. La decisión debe ser explícita para el usuario. Una transacción por sí sola no soluciona este fallo: el conjunto de IDs a borrar ya es incorrecto.

**Criterios de cierre.** Una prueba permanente con carga parcial debe demostrar que B permanece intacta y que el estado de error tiene el comportamiento acordado; añadir el recorrido con ambas tareas cargadas correctamente.

## AUD-003: el arranque puede sobrescribir el historial

**Impacto.** Al iniciar el servicio puede desaparecer la conversación persistida y recuperarse únicamente el prompt de una sesión vacía.

**Código implicado.** `data/service/ChatAIService.kt`, bloque `init`, `initSession()`, `loadMessages()` y `saveMessages()`; `MessageStorageImpl` para la persistencia.

**Causa.** El constructor llama a `initSession()`, que programa un guardado, antes de programar la carga del historial. Si ese guardado termina primero, la posterior lectura ya encuentra el historial sobrescrito.

**Cómo reproducirlo.** Tener una conversación guardada e iniciar el servicio con un almacenamiento que permita completar el primer guardado antes de la primera lectura. Los mensajes de usuario y asistente desaparecen.

**Evidencia.** `startupMustRecoverThePersistedConversationBeforeWriting` utilizó almacenamiento en memoria cuyas lecturas reflejan las escrituras y reprodujo el historial vacío. El test anterior de recuperación devolvía una lista fija desde un mock, por lo que no detectaba esta interacción.

**Corrección propuesta.** Cargar el historial antes de escribir. Crear y persistir una sesión nueva solo cuando no exista historial. Coordinar la inicialización con las acciones posteriores para que una carga tardía no sobrescriba una sesión más reciente.

**Criterios de cierre.** Pruebas permanentes para historial existente, primer arranque sin historial y carga retrasada. El historial previo debe conservarse sin una escritura inicial que lo sustituya.

## AUD-004: respuesta antigua dentro de una sesión nueva

**Impacto.** Una respuesta que pertenecía a una conversación borrada puede reaparecer en el chat y convertirse en la propuesta de la sesión nueva.

**Código implicado.** `data/service/ChatAIService.kt`, `sendMessage()` e `initSession()`; `ui/assistant/AssistantViewModel.kt`, `startNewSession()`; `ui/assistant/elements/AssistantTopBar.kt`, opción de reinicio.

**Causa.** Se permite reiniciar mientras se espera una respuesta. Al llegar, el servicio la añade al historial global actual sin verificar la sesión de origen. `AssistantRepository.sessionVersion` protege la limpieza posterior al guardado, pero no la llegada de respuestas de IA.

**Cómo reproducirlo.** Enviar un mensaje, reiniciar la conversación mientras el servicio externo todavía está respondiendo y dejar que termine la petición anterior. Su respuesta reaparece y se persiste en la nueva sesión.

**Evidencia.** `aResponseFromBeforeResetMustNotEnterTheNewSession` retrasó una respuesta con `CompletableDeferred`, reinició la sesión y después liberó la respuesta. Tanto el chat como la propuesta nuevos quedaron poblados con el objetivo anterior.

**Corrección propuesta.** Asociar cada petición a la versión de su sesión y a una copia de su historial. Cancelar o descartar resultados de sesiones anteriores, incluidos sus errores y cambios del estado de carga, para que no afecten a una petición más reciente.

**Criterios de cierre.** Pruebas permanentes con una respuesta retrasada tras reiniciar y tras iniciar otra petición: ningún resultado ni error antiguo debe modificar la conversación, propuesta, persistencia o carga de la sesión vigente.

## AUD-005: avisos de subtareas eliminadas

**Impacto.** Pueden seguir llegando avisos de pasos que ya no existen. Al editar una meta y crear nuevos IDs, esos avisos pueden coexistir con los de los pasos sustitutos.

**Código implicado.** `data/repository/TaskRepository.kt`, `deleteTask()` y `saveTaskWithSubtasks()`; cascada de `data/local/TaskEntity.kt`; `data/alarm/AlarmReceiver.kt`.

**Causa.** El repositorio programa alarmas para las subtareas al guardarlas, pero al borrar la raíz solo cancela la alarma de esa raíz. Room elimina descendientes por cascada sin cancelar sus alarmas. El receptor muestra el contenido del aviso sin comprobar que la tarea siga existiendo.

**Cómo reproducirlo.** Crear una meta con una subtarea que tenga aviso futuro y borrar la meta. La subtarea desaparece de la base de datos, pero su aviso sigue programado. Editar y reemplazar la jerarquía puede añadir un nuevo aviso sin retirar el anterior.

**Evidencia.** `deletingAWholeGoalMustCancelTheRemindersOfItsChildren` usó Room en memoria y un programador de alarmas simulado. Comprobó que el descendiente ya no existía en Room y que su alarma seguía registrada. No se ejecutaron avisos en el teléfono.

**Corrección propuesta.** Obtener los IDs de toda la jerarquía afectada y cancelar sus avisos al confirmar la eliminación o el reemplazo. Coordinarlo con la transacción de AUD-001 para no cancelar avisos de tareas que deban conservarse tras un fallo.

**Criterios de cierre.** Pruebas permanentes para borrar y reemplazar jerarquías con varios niveles y avisos, comprobando la retirada de alarmas antiguas, la programación de las nuevas y la conservación de las originales si falla la escritura.

## Verificación y alcance de las pruebas

Las cinco pruebas de reproducción de la auditoría fallaron en sus aserciones de comportamiento seguro, confirmando los efectos descritos. Fueron pruebas temporales: sus copias y resultados se conservaron como evidencia local en `.artifacts/pre-prompt-audit-2026-10-22/`, una carpeta ignorada por Git que no debe ser necesaria para entender este documento. No forman parte de la suite permanente; deberán incorporarse y ampliarse al corregir cada incidencia.

Después de retirar las pruebas temporales, `:app:testDebugUnitTest :app:assembleDebug` terminó correctamente: **171 pruebas permanentes, 0 fallos, 0 errores y 0 omitidas**. Es el resultado de la auditoría del 22 de octubre, no una garantía de que los cinco casos estén cubiertos. La revisión visual y del dispositivo corresponde a Ignasi.

Al cerrar una incidencia, registrar aquí su commit y las pruebas permanentes añadidas. Mantener el historial del hallazgo y actualizar su estado en la tabla y en [tasks/todo.md](../../tasks/todo.md).
