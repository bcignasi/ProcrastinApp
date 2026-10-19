# Cabecera Serena del Asistente

`AssistantTopBar` presenta la marca, acceso a Ajustes, menú de acciones, título «Asistente», fecha localizada y pestañas Conversación / Propuesta. Sigue los márgenes, tipografía, colores e insets superiores y laterales de Agenda. Cada pestaña tiene una altura táctil mínima de 48 dp y semántica `Role.Tab` con estado seleccionado.

## Estado y contenido

`AssistantScreen` conserva `currentScreenMode` mediante `rememberSaveable`, con `MESSAGES` como valor inicial. Una entrada nueva empieza en Conversación; volver a una entrada conservada recupera la pestaña elegida. `AssistantScreenType.MESSAGES` y `TASKS` mantienen sus identidades; solo cambian sus títulos localizados.

La condición existente sigue eligiendo `AssistantMessagesView` o `AssistantTasksView`. El campo de escritura permanece fuera de esa condición y mantiene su borrador al cambiar de pestaña. Este incremento no modifica los mensajes, las tarjetas de propuesta, el ViewModel ni el guardado de tareas.

La fecha se calcula en la pantalla con `produceState` y se vuelve a consultar cada minuto para renovar el día sin enviar mensajes ni cargar tareas. La cabecera recibe `LocalDate` y utiliza el mismo formato localizado de Agenda. Cambiar la fecha no reinicia la pestaña elegida.

## Acciones

El engranaje navega a la ruta existente `settings`. Al volver se conserva la entrada del Asistente, su pestaña y el borrador. El menú «Más opciones» contiene «Reiniciar conversación» y reutiliza el callback existente a `startNewSession()`. Reiniciar sigue deshabilitado mientras `isSaving` es verdadero. Abrir el menú, Ajustes o una pestaña no reinicia la conversación.

## Verificación

`AssistantScreenTabsTest` ejecuta la pantalla real con navegación Compose, datos locales y ViewModels simulados, sin llamadas de IA. Comprueba:

- Conversación inicial, cambio a Propuesta y vuelta, contenido visible y borrador conservado.
- Entrada a Ajustes desde Propuesta y regreso con la misma pestaña y borrador, en modo claro.
- Acceso a reiniciar mediante el menú y bloqueo durante el guardado.

Las pruebas usan un contenedor de 320 dp. Ignasi realiza la revisión visual en el móvil mediante Run en Android Studio. El siguiente incremento es el acabado de los mensajes y del campo de escritura; después se pule la presentación de propuestas.

Las tres pruebas nuevas fallaron antes de implementar la cabecera y pasan con el cambio. La suite completa suma 121 pruebas, sin fallos, errores ni omitidas. `assembleDebug`, `compileDebugAndroidTestKotlin` y `lintDebug` terminan correctamente; lint conserva advertencias y no presenta errores. Los registros técnicos se conservan localmente en `.artifacts/assistant-tabs-red.log`, `.artifacts/assistant-tabs-green.log` y `.artifacts/assistant-header-validation.log`.
