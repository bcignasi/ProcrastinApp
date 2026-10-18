# Barra inferior: Agenda y Asistente

## Qué cambia

La barra inferior permite volver a la agenda o retomar el asistente. La selección se deriva de la ruta actual; no hay un segundo estado de pestaña que pueda desincronizarse. Los iconos superiores que realizaban estos saltos se retiran. El engranaje de Ajustes permanece en la agenda.

`MainBottomBar.kt` conserva dos parámetros: `selectedRoute` recibe la sección activa y `onSelect` comunica la pulsación a `AppNavigation`. La selección utiliza el acento de Serena en oscuro y la paleta existente en claro.

## Acabado visual de Serena

La primera barra utilizaba `NavigationBarItem`, con icono encima del texto. Para acercarla a la maqueta, se sustituye por una `Surface` plana y una `Row` con dos opciones del mismo ancho mediante `Modifier.weight(1f)`.

La función privada `BottomBarItem` coloca icono y texto en otra `Row`. `clip(MaterialTheme.shapes.medium)` redondea las esquinas a 12 dp y `background` aplica `primaryContainer` a la opción seleccionada. Su contenido utiliza `primary` y seminegrita; la opción inactiva utiliza `onSurfaceVariant` y peso normal.

`HorizontalDivider` dibuja la línea superior de 1 dp con `outlineVariant`. El espaciado exterior es de 16 dp a los lados y 8 dp arriba y abajo. `windowInsetsPadding(NavigationBarDefaults.windowInsets)` conserva el espacio para la navegación del sistema, y las opciones tienen altura mínima de 48 dp y pueden crecer con el texto.

Los iconos son vectores locales de contorno: `res/drawable/ic_agenda_outline.xml` representa una lista con casillas y `ic_assistant_outline.xml` una burbuja de conversación. Ambos usan trazo de 1,5 unidades, escalan sin perder definición y reciben el color del tema mediante `Icon(tint = contentColor)`.

`selectableGroup()` agrupa las opciones y `selectable(selected = selected, role = Role.Tab, onClick = onClick)` conserva la interacción y comunica la pestaña seleccionada a accesibilidad. El icono no repite una descripción porque la etiqueta ya identifica el destino. La selección sigue procediendo de la ruta actual y no añade estado propio.

## Conservar el estado al cambiar de pestaña

`MainNavigation.kt` reúne las rutas y las acciones. `assistantDestinations` agrupa las tres entradas existentes al asistente (conversación actual, sesión nueva y edición) en un grafo llamado `assistant`. Así se guarda y restaura la entrada concreta, con sus argumentos y ViewModels.

Al pulsar una pestaña se ejecuta `openMainDestination`:

```kotlin
if (currentDestination?.mainDestination() == route) return
navigate(route) {
    popUpTo(AGENDA_ROUTE) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

- La primera condición evita navegar si ya estamos en esa sección.
- `popUpTo` vuelve a la entrada de Agenda y `saveState` conserva la sección que se retira.
- `launchSingleTop` evita otra copia de la entrada superior.
- `restoreState` recupera la sección guardada cuando volvemos a ella.

La agenda conserva su filtro con `rememberSaveable`. El asistente utiliza ahora `rememberSaveable` para su modo de vista y el indicador de guardado; el campo de mensaje ya guardaba así su borrador. Navigation conserva estos estados de pantalla y los ViewModels al alternar entre pestañas.

## Abrir una planificación nueva o editar tareas

El botón «+» y las acciones de edición usan `openAssistant`. Esta acción elimina la sección del asistente que estuviera guardada mediante `clearBackStack(ASSISTANT_ROUTE)` y abre la solicitud explícita, sin restaurar el borrador anterior.

`AssistantViewModel.enterFromNavigation` procesa la solicitud inicial una sola vez durante la vida de ese ViewModel. Esto evita que volver a una pestaña repita `startNewSession()` y borre la propuesta, o vuelva a cargar una tarea original por encima de una edición. El botón explícito de borrar la conversación sigue pudiendo iniciar una sesión nueva.

## Distribución, teclado y botón atrás

`AppNavigation.kt` incorpora un `Scaffold` exterior que reserva espacio para la barra. Las pantallas conservan sus cabeceras y su contenido. El relleno se consume con `consumeWindowInsets` para evitar aplicar dos veces el espacio de las barras del sistema.

La barra inferior se muestra solo en Agenda y Asistente. Se oculta en Ajustes, durante el arranque y mientras el teclado está abierto. `imePadding()` reserva el espacio del teclado y el manifiesto declara `adjustResize` para recibir su cambio de tamaño.

Desde el asistente, `BackHandler` vuelve a Agenda con la misma acción que la pestaña, conservando el asistente. Tras guardar una propuesta también se vuelve mediante esa acción. El comportamiento existente de salida desde Agenda se conserva.

## Verificación

Se añaden seis pruebas con un `NavHostController` real sobre Robolectric, sin dispositivo ni red: conservación del estado de las entradas y de los ViewModels, reselección sin duplicados, edición al cambiar de pestaña, sesión nueva desde «+», sustitución de una edición anterior y retorno desde Ajustes.

Se añaden dos pruebas del ViewModel: volver a una sesión no borra su propuesta y volver a editar no sustituye los cambios por la tarea original. Antes de la corrección, seis pruebas fallaron mostrando estos problemas; después pasaron las ocho nuevas y las tres anteriores del ViewModel.

Ignasi realiza la revisión visual y del teclado en el móvil. Recorrido sugerido: elegir un filtro, abrir Asistente, escribir sin enviar, volver a Agenda y retomar el asistente; comprobar también «+», edición, botón atrás y Ajustes. El arranque, las rutas de edición y las acciones de guardado existentes se conservan.

Resultado técnico del incremento: `assembleDebug`, `assembleDebugAndroidTest`, 94 pruebas unitarias (las 86 anteriores y las ocho nuevas) y `lintDebug` correctos. Lint mantiene 0 errores y las 29 advertencias anteriores. Las pruebas de interfaz se han compilado; en este incremento no se han ejecutado en el móvil.

El acabado visual de la barra del 19 de octubre se verifica con `assembleDebug`, las 94 pruebas existentes y `lintDebug`: todo correcto, con 0 errores y las mismas 29 advertencias. No añade pruebas nuevas ni cambia las funciones de navegación. La comprobación de su aspecto en el móvil corresponde a Ignasi.

Referencia oficial: [guardar y restaurar varias pilas de navegación](https://developer.android.com/guide/navigation/backstack/multi-back-stacks).
