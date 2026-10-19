# Cabecera Serena del Asistente

`AssistantTopBar` presenta la marca, acceso a Ajustes, menú de acciones, título «Asistente», fecha localizada y pestañas Conversación / Propuesta. Sigue los márgenes, tipografía, colores e insets superiores y laterales de Agenda. Cada pestaña tiene una altura táctil mínima de 48 dp y semántica `Role.Tab` con estado seleccionado.

## Estado y contenido

`AssistantScreen` conserva `currentScreenMode` mediante `rememberSaveable`, con `MESSAGES` como valor inicial. Una entrada nueva empieza en Conversación; volver a una entrada conservada recupera la pestaña elegida. `AssistantScreenType.MESSAGES` y `TASKS` mantienen sus identidades; solo cambian sus títulos localizados.

La condición existente sigue eligiendo `AssistantMessagesView` o `AssistantTasksView`. El campo de escritura permanece fuera de esa condición y mantiene su borrador al cambiar de pestaña. La cabecera no modifica los mensajes, las tarjetas de propuesta ni el guardado de tareas.

## Ayudas cuando no hay contenido

Conversación muestra `assistant_intro_text` mientras no hay mensajes y Propuesta muestra `assistant_proposal_empty_text` mientras no hay tareas. Los dos textos son breves, están centrados y tienen traducción española e inglesa. `AquiNoHayNadaBox` recibe el recurso de texto y conserva las citas motivadoras existentes.

La visibilidad depende únicamente del contenido de cada vista. `QuoteViewModel` gestiona las citas; no cuenta las veces que se compone la ayuda ni decide cuándo ocultarla. Cambiar de pestaña o escribir un borrador no hace desaparecer la explicación.

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

Las pruebas usan un contenedor de 320 dp y una ventana virtual suficientemente alta para permitir los tamaños solicitados. Las tres pruebas de altura reprodujeron el fallo antes de la corrección; la auditoría había medido solo 9 dp visibles para escribir con 240 dp disponibles. Ignasi realiza la revisión visual en el móvil mediante Run en Android Studio. Quedan pendientes el acabado de las burbujas de mensajes y la presentación de propuestas.

Las tres pruebas de altura fallaron antes de la corrección y pasan con la cabecera adaptable. Las dos pruebas de ayuda vacía también reprodujeron la desaparición del texto antes de retirar el contador. Se aprueban las ocho pruebas de pantalla del Asistente y la suite completa suma 126 pruebas, sin fallos, errores ni omitidas. `assembleDebug`, `compileDebugAndroidTestKotlin` y `lintDebug` terminan correctamente; lint conserva 41 advertencias y un aviso informativo, sin errores. Los registros de las ayudas se conservan en `.artifacts/assistant-empty-red.log`, `.artifacts/assistant-empty-green.log` y `.artifacts/assistant-empty-input-verified.log`. Los registros anteriores de la cabecera compacta permanecen en `.artifacts/assistant-compact-*.log`.
