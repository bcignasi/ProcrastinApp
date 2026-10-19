# Cabecera Serena del Asistente

`AssistantTopBar` presenta la marca, acceso a Ajustes, menú de acciones, título «Asistente», fecha localizada y pestañas Conversación / Propuesta. Sigue los márgenes, tipografía, colores e insets superiores y laterales de Agenda. Cada pestaña tiene una altura táctil mínima de 48 dp y semántica `Role.Tab` con estado seleccionado.

## Estado y contenido

`AssistantScreen` conserva `currentScreenMode` mediante `rememberSaveable`, con `MESSAGES` como valor inicial. Una entrada nueva empieza en Conversación; volver a una entrada conservada recupera la pestaña elegida. `AssistantScreenType.MESSAGES` y `TASKS` mantienen sus identidades; solo cambian sus títulos localizados.

La condición existente sigue eligiendo `AssistantMessagesView` o `AssistantTasksView`. El campo de escritura permanece fuera de esa condición y mantiene su borrador al cambiar de pestaña. La cabecera no modifica los mensajes, las tarjetas de propuesta ni el guardado de tareas.

## Ayudas cuando no hay contenido

Conversación muestra `assistant_intro_text` mientras no hay mensajes y Propuesta muestra `assistant_proposal_empty_text` mientras no hay tareas. Los dos textos son breves, están centrados y tienen traducción española e inglesa. `AquiNoHayNadaBox` recibe el recurso de texto y conserva las citas motivadoras existentes.

La visibilidad depende únicamente del contenido de cada vista. `QuoteViewModel` gestiona las citas; no cuenta las veces que se compone la ayuda ni decide cuándo ocultarla. Cambiar de pestaña o escribir un borrador no hace desaparecer la explicación.

## Burbujas de conversación

`AssistantMessagesViewMessageItem` conserva `message.isUser` para elegir el autor y prepara el contenido visible mediante `conversationMessageText`. Los mensajes del usuario se alinean a la derecha, sin avatar, en una columna que limita la burbuja al 85 % del ancho disponible; los textos cortos se ajustan a su contenido. Los mensajes del asistente se alinean a la izquierda, con un icono `Outlined.SmartToy` de 20 dp dentro de un círculo de 32 dp y una separación de 8 dp.

Las burbujas utilizan `MaterialTheme.shapes.large` (16 dp), sin elevación tonal, y `typography.bodyLarge` (DM Sans). Usuario usa `primaryContainer` / `onPrimaryContainer`; asistente usa `surfaceVariant` / `onSurfaceVariant`. El texto tiene 16 dp de margen horizontal y 12 dp vertical, permite varias líneas y crece con el tamaño de fuente del sistema. La semántica agrupa cada mensaje con su autor localizado; el robot es decorativo para no duplicar el anuncio.

La lista aplica una única separación de 12 dp entre mensajes. `TypingIndicator` comienza a 40 dp del borde (32 de avatar + 8 de separación) y utiliza la misma forma y superficie plana que los mensajes del asistente. Se conserva la animación de los tres puntos.

La validación del incremento ejecuta las 128 pruebas existentes, `assembleDebug`, `compileDebugAndroidTestKotlin` y `lintDebug`: sin fallos ni omitidas, sin errores de lint y con las 41 advertencias anteriores. El registro está en `.artifacts/serena-message-bubbles-validation.log`. Ignasi revisa en el móvil los mensajes cortos y largos, el ancho de las burbujas y el tamaño de letra ampliado.

## Texto legible al abrir una tarea — 20 de octubre de 2026

Al abrir una tarea de Agenda, `AssistantViewModel.loadTasksForEditing` reconstruye un mensaje JSON con `propuesta.tasks`. Ese mensaje contiene la tarea y sus subtareas; no contiene el comentario de la respuesta original de IA. Este flujo y el JSON interno se conservan.

`conversationMessageText` solo prepara lo que dibuja la burbuja. Reutiliza `AssistantResponseParserImpl` y comprueba que el objeto contiene `propuesta.tasks` como array antes de ocultar el JSON. Muestra el texto anterior, el `comentario` si existe y el texto posterior, retirando las marcas de bloque de código que rodean la propuesta. Sin texto legible muestra el recurso `assistant_proposal_ready`: «Tu plan está disponible en Propuesta. Puedes pedirme cambios aquí», con traducción inglesa. Es una indicación fija de la app, no una nueva respuesta de IA ni una recuperación del comentario original.

Los mensajes normales y del usuario conservan el recorte existente de instrucciones automáticas. Un JSON ajeno al formato de propuestas o que el parser no pueda reconocer conserva la presentación anterior. No se modifica `clipAutomaticText` compartido, el repositorio, el guardado, el contexto enviado a IA ni la estructura de tareas. La burbuja calcula el texto con `remember(message, proposalFallback)`; cambiar el mensaje o el idioma vuelve a calcularlo.

`AssistantEditingConversationTest` abre la pantalla real con sesión, serialización y parser reales, simulando únicamente la lectura de la tarea y los servicios externos. En español e inglés exige texto legible sin JSON visible, subtareas conservadas en Propuesta, el mismo JSON interno tras cambiar de pestaña y ninguna consulta a IA. Las dos pruebas fallaron antes del arreglo porque se mostraba el JSON.

`ConversationMessageTextTest` cubre ocho casos: propuesta solo JSON sin mutar el mensaje, comentario, texto a ambos lados con bloque de código, bloque sin texto, mensaje normal, mensaje de usuario, JSON ajeno o de forma incorrecta y JSON malformado. Los registros locales son `.artifacts/assistant-readable-red.log`, `.artifacts/assistant-readable-green.log` y `.artifacts/assistant-readable-validation.log`.

Validación del arreglo: 138 pruebas aprobadas, sin fallos, errores ni omitidas; `assembleDebug`, `compileDebugAndroidTestKotlin` y `lintDebug` correctos. Lint conserva 41 advertencias y un aviso informativo, sin errores. La comprobación visual en el móvil queda a cargo de Ignasi.

## Campo de escritura

`MessageInputField` utiliza un único `OutlinedTextField` con `MaterialTheme.shapes.large` (16 dp), fondo `surface` y borde `outlineVariant` cuando no tiene foco. El envío es un `IconButton` de 48 dp dentro de `trailingIcon`, con el icono de contorno `AutoMirrored.Outlined.Send`. El campo sigue creciendo hasta cinco líneas y tiene una altura mínima de 56 dp.

El borrador se conserva con `rememberSaveable`. El envío sigue deshabilitado con el campo vacío o durante carga/guardado. Al enviar se ejecuta el mismo callback, se vacía el borrador y se oculta el teclado; durante la carga se muestra el indicador de progreso en el mismo espacio del icono. No hay grabación de voz.

La fecha se calcula en la pantalla con `produceState` y se vuelve a consultar cada minuto para renovar el día sin enviar mensajes ni cargar tareas. La cabecera recibe `LocalDate` y utiliza el mismo formato localizado de Agenda. Cambiar la fecha no reinicia la pestaña elegida.

## Acciones

El engranaje navega a la ruta existente `settings`. Al volver se conserva la entrada del Asistente, su pestaña y el borrador. El menú «Más opciones» contiene «Reiniciar conversación» y reutiliza el callback existente a `startNewSession()`. Reiniciar sigue deshabilitado mientras `isSaving` es verdadero. Abrir el menú, Ajustes o una pestaña no reinicia la conversación.

## Altura disponible

`BoxWithConstraints`, dentro de `Scaffold.topBar`, consulta la altura real disponible. El teclado ya reduce ese espacio mediante `imePadding` en `AppNavigation`; también se detectan ventanas pequeñas y orientación horizontal sin depender de la orientación del dispositivo.

Si `maxHeight < 400.dp * fontScale.coerceAtLeast(1f)`, la cabecera utiliza su presentación compacta. El umbral aumenta con el tamaño de fuente para dejar espacio a controles y contenido más altos. No se guarda ninguna preferencia nueva.

- Con espacio suficiente se muestran marca, engranaje, menú, título, fecha y pestañas.
- Con poca altura se muestran las mismas pestañas junto a «Más opciones». Ajustes pasa a ese menú; reiniciar conserva su bloqueo durante el guardado. Marca, título grande y fecha se ocultan hasta recuperar altura.

`AssistantModeTabs` acepta un `Modifier` para ocupar el espacio restante de la fila compacta mediante `weight(1f)`. La selección sigue en `AssistantScreen` y el campo de escritura permanece en el mismo lugar de la composición: cambiar de tamaño no recrea el borrador ni la sesión.

## Verificación

`AssistantScreenTabsTest` ejecuta la pantalla real con navegación Compose, datos locales y ViewModels simulados, sin llamadas de IA. Comprueba:

- Conversación inicial, cambio a Propuesta y vuelta, contenido visible y borrador conservado.
- Entrada a Ajustes desde Propuesta y regreso con la misma pestaña y borrador, en modo claro.
- Acceso a reiniciar mediante el menú y bloqueo durante el guardado.
- Escritura y envío por pulsación táctil con 240 y 180 dp de altura disponible, exigiendo al menos 48 dp visibles de campo.
- Transición entre 640 y 240 dp con fuente al 150 %, manteniendo Propuesta, borrador y acceso a Ajustes, y recuperando el engranaje al ampliar la ventana.
- Ayudas de las dos pestañas vacías tras cuatro cambios de ida y vuelta y escritura de un borrador, en español/claro e inglés/oscuro, con `QuoteViewModel` real y servicio de citas simulado. Al llegar mensajes o tareas, las ayudas desaparecen y se muestra el contenido.

Dos pruebas adicionales forman parte de la suite permanente y se ejecutan con `:app:testDebugUnitTest`, sin scripts de auditoría:

- `AssistantEmptyStateTest.independentListsAndReturningToEmptyKeepTheCorrectGuidance`: llena solo los mensajes y después solo las tareas. Cada pestaña muestra su ayuda únicamente si su propia lista está vacía. Al volver a vaciar ambas listas, las ayudas reaparecen y permanecen tras cuatro cambios de ida y vuelta. Se simulan cambios de estado; no se prueba aquí la acción de reiniciar del ViewModel.
- `MessageInputFieldTest.busyInputBlocksTouchAndKeepsTheMultilineDraftUntilSending`: con fuente al 150 % y ancho de 320 dp, el envío empieza deshabilitado, se escribe un borrador de dos líneas y se activa la carga. Pulsar físicamente en el lugar del envío no llama al callback ni borra el borrador. Al terminar la carga, una pulsación entrega exactamente el texto original una vez, vacía el texto editable y vuelve a deshabilitar el envío. Usa el campo real con un callback local, sin servicio de IA.

Las pruebas usan un contenedor de 320 dp y una ventana virtual suficientemente alta para permitir los tamaños solicitados. Las tres pruebas de altura reprodujeron el fallo antes de la corrección; la auditoría había medido solo 9 dp visibles para escribir con 240 dp disponibles. Ignasi realiza la revisión visual en el móvil mediante Run en Android Studio. Queda pendiente la presentación de propuestas.

Las tres pruebas de altura fallaron antes de la corrección y pasan con la cabecera adaptable. Las dos pruebas de ayuda vacía también reprodujeron la desaparición del texto antes de retirar el contador. Con las dos pruebas incorporadas desde la auditoría, se aprueban diez pruebas de interfaz del Asistente y la suite completa suma 128 pruebas, sin fallos, errores ni omitidas. `testDebugUnitTest` y `lintDebug` terminan correctamente con la suite permanente; lint conserva 41 advertencias y un aviso informativo, sin errores. La compilación de la app y de las pruebas instrumentadas se validó al implementar los cambios del Asistente.

El registro de incorporación de las pruebas se conserva en `.artifacts/assistant-permanent-tests-verified.log`. Los registros de las ayudas permanecen en `.artifacts/assistant-empty-red.log`, `.artifacts/assistant-empty-green.log` y `.artifacts/assistant-empty-input-verified.log`; los de la cabecera compacta, en `.artifacts/assistant-compact-*.log`.
