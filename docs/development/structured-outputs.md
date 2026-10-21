# Structured Outputs del Asistente

Implementación del 21 de octubre de 2026. La primera entrega añadió un JSON Schema estricto a Chat Completions con `gpt-4o-mini`, sin cambiar la planificación. En el siguiente incremento, Ignasi elige `gpt-6-luna` para el Asistente. El esquema y el comportamiento del prompt siguen siendo los mismos.

`ChatAIService` indica ahora `model = "gpt-6-luna"` en su petición. `ChatRequest` mantiene su valor predeterminado para las frases motivacionales. Luna admite Chat Completions y Structured Outputs; no usamos herramientas ni enviamos parámetros de razonamiento, por lo que se aplica su valor predeterminado, `medium`. La compatibilidad documental no confirma el acceso de una cuenta concreta: la primera respuesta real se comprobará desde el móvil. [Ficha oficial de Luna](https://developers.openai.com/api/docs/models/gpt-6-luna).

## Petición y contrato

`data/ai/RequestResponse.kt` añade `ChatRequest.responseFormat`, serializado como `response_format`. Es opcional: `QuoteAIService` sigue pidiendo texto libre sin enviar ese campo.

`data/ai/AssistantResponseFormat.kt` construye el esquema `assistant_tasks_v1`:

```kotlin
val request = ChatRequest(
    messages = conversationMessages,
    responseFormat = AssistantResponseFormat.responseFormat
)
```

Gson serializa esa configuración como `response_format.type = "json_schema"`, con `json_schema.name = "assistant_tasks_v1"`, `strict = true` y el objeto `schema` completo.

La respuesta conserva la estructura que ya interpreta la app:

```json
{
  "comentario": "Podemos empezar por la teoría.",
  "propuesta": {
    "tasks": [
      {
        "id": 0,
        "title": "Aprobar el teórico",
        "deadline": "",
        "priority": 1,
        "periodicity": "",
        "notes": "",
        "completed": false,
        "notify": "",
        "subtasks": []
      }
    ]
  }
}
```

Todos los campos son obligatorios. Los textos todavía sin definir usan `""`; las listas sin elementos usan `[]`. No se aceptan `null` ni propiedades adicionales. Las prioridades admitidas son 0, 1, 2 y 3.

`$defs.task` define una tarea y `subtasks.items` referencia esa misma definición mediante `$ref`. Así se aplica el mismo contrato a cualquier nivel de subtareas, conservando el modelo `Task` actual.

## Recorrido de la respuesta

1. `ChatAIService.sendMessage` envía el esquema junto al historial. El mensaje de sistema con la hora incluye una aclaración técnica del formato para conversaciones antiguas cuyo prompt permitía `null`.
2. `AIService` recibe DTO de transporte separados de `Message`. Estos DTO admiten los valores nulos que puede devolver la API.
3. Antes de crear un `Message`, comprueba que haya respuesta, que no exista `refusal`, que `finish_reason` sea `stop`, que el rol sea `assistant` y que el contenido no esté vacío. `content_filter` se trata como denegación; `length` u otra finalización no completa producen un error de respuesta incompleta.
4. `AssistantResponseFormat.validate` comprueba el contenido del Asistente: un único documento JSON estricto, campos exactos y tipos sin conversiones implícitas, recursión, títulos no vacíos, ID entero no negativo dentro de `Long` y prioridad entre 0 y 3.
5. Solo después se añade la respuesta al historial y se guarda. `AssistantRepository` utiliza el parser y extractor existentes para mostrar el comentario y la propuesta.

La validación no modifica el JSON recibido. `Message.content` sigue siendo una cadena no nula y no se cambia su formato persistido.

## Errores y compatibilidad

`AIServiceError` distingue respuesta vacía, incompleta, denegada y de estructura inválida, además de HTTP y comunicación. Los errores tipados se conservan: un HTTP 400 ya no se envuelve como un error genérico de comunicación. Las cancelaciones se propagan y se desactiva el indicador de carga.

Conversación y Propuesta muestran los nuevos errores mediante recursos en español e inglés. Ante un fallo no se publica ni se persiste la respuesta fallida; el último plan válido sigue disponible. No hay reintentos automáticos. El mensaje del usuario se muestra durante el intento, como antes.

Una respuesta válida con `"tasks": []` sigue siendo legítima y se acepta. No se confunde con una respuesta mal formada, que se rechaza. La decisión de conservar o modificar las tareas durante una entrevista corresponde al próximo trabajo del prompt.

El esquema se exige solo a respuestas nuevas de la API del Asistente. El historial previo y los contextos de edición inyectados por la app mantienen el parser tolerante existente; no se reescriben conversaciones ni se migra la base de datos.

## Pruebas permanentes

- `AssistantResponseFormatTest`: serialización real de `response_format` y `json_schema`, objetos cerrados con campos obligatorios, referencias recursivas, exclusión del formato en peticiones normales, tareas anidadas válidas y rechazo de campos, tipos, números, títulos y documentos inválidos.
- `AIServiceResponseTest`: ejecuta el servicio real con un cliente externo simulado. Comprueba mensajes completos, texto libre de frases, denegaciones con contenido nulo, finalizaciones incompletas, filtrado, respuestas vacías, rol incorrecto, errores HTTP/red y cancelaciones.
- `StructuredAssistantIntegrationTest`: servicio, repositorio y parser reales; comprueba la petición estricta, comentario con lista vacía, conservación de propuesta e historial ante errores, reintento correcto y fin de carga al cancelar. Mantiene un contexto antiguo para verificar compatibilidad.
- `StructuredAssistantErrorTest`: comprueba en Robolectric los tres errores nuevos en ambas pestañas, en español e inglés, junto al contenido anterior.
- `ChatAIServiceTest`: los ejemplos de respuestas nuevas usan el contrato completo; los ejemplos de historial antiguo permanecen intactos.

No se realizan llamadas reales a OpenAI ni instalaciones en el móvil durante estas pruebas. La aceptación del esquema y la calidad de un plan real se comprueban después desde la app.

Validación local completada: 171 pruebas aprobadas, sin fallos, errores ni omitidas; 23 pruebas nuevas. `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin` y `:app:lintDebug` correctos. Lint mantiene 0 errores, 41 advertencias y 1 sugerencia, como antes del incremento.

## Límites del incremento

Structured Outputs controla la estructura de respuestas completas admitidas, no garantiza la utilidad del plan ni elimina denegaciones o interrupciones. La app añade comprobaciones defensivas antes de utilizar el resultado.

Quedan pendientes el nuevo prompt de entrevista, la comparación de modelos y las validaciones de fechas, plazos, carga de trabajo y coherencia entre metas. Tampoco se modifica la normalización anterior de IDs de `TaskJsonExtractor`, que ya reinicia los IDs al transformar tareas, ni el historial global único de la aplicación.

Referencia: [Structured Outputs, documentación oficial de OpenAI](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=chat). El esquema sigue las reglas de campos obligatorios, `additionalProperties: false`, `$defs` y referencias recursivas de esa guía.
