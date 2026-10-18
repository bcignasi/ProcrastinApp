# Cómo funciona el selector claro/oscuro

Rama de trabajo: `feature/theme-settings`.

Este primer incremento reutiliza las paletas actuales de la aplicación. Serena y Refugio élfico permanecen como trabajo posterior.

## Recorrido en el móvil

Agenda → engranaje de la esquina superior derecha → Ajustes → Apariencia → Tema oscuro.

El interruptor activado selecciona el modo oscuro y desactivado el claro. La flecha de la pantalla vuelve a la agenda. La selección se guarda en el móvil y se recupera al abrir de nuevo la aplicación. Antes de la primera elección se sigue el tema del teléfono, como ocurría antes de añadir Ajustes.

## 1. El engranaje abre una ruta

En `ui/tasklist/elements/TaskListTopBar.kt`:

```kotlin
IconButton(onClick = onOpenSettings) {
    Icon(
        Icons.Default.Settings,
        contentDescription = stringResource(R.string.settings_open)
    )
}
```

El botón muestra un icono y avisa cuando lo pulsas. La pantalla de la agenda decide qué hacer con ese aviso:

```kotlin
onOpenSettings = { navController.navigate("settings") }
```

En `ui/navigation/AppNavigation.kt`, `composable("settings")` relaciona ese nombre con `SettingsScreen`. La flecha de vuelta llama a `popBackStack()`: regresa a la pantalla anterior. El filtro de la agenda utiliza `rememberSaveable` para conservarse durante ese recorrido.

## 2. Ajustes muestra el estado y emite cambios

`ui/settings/SettingsScreen.kt` recibe estos datos:

```kotlin
darkTheme: Boolean,
isSaving: Boolean,
saveFailed: Boolean,
onDarkThemeChange: (Boolean) -> Unit,
onBack: () -> Unit
```

`Boolean` representa verdadero o falso. `(Boolean) -> Unit` es una función que recibe un booleano y no devuelve un resultado. Esta función permite que la pantalla comunique la elección sin encargarse de guardarla.

La fila entera tiene `toggleable`, con el papel accesible `Role.Switch`. El `Switch` representa visualmente el mismo valor. Su `onCheckedChange = null` deja las pulsaciones en manos de la fila para que haya un solo control accesible, con una zona táctil amplia.

Mientras se guarda, el control se desactiva brevemente para evitar escrituras simultáneas. Si el guardado falla, se conserva la selección anterior y aparece un mensaje que permite intentarlo otra vez pulsando el control.

## 3. El ViewModel coordina la elección

`ui/settings/ThemeSettingsViewModel.kt` conecta la pantalla con las preferencias:

- Expone un `StateFlow` con el valor y los estados de carga, guardado y error.
- Observa el valor guardado al abrir la app.
- Llama a `ThemePreferences.setDarkTheme` cuando eliges otro modo.
- Conserva la elección anterior si el almacenamiento devuelve un error de escritura.

El ViewModel vive en la actividad y su estado se comparte con la navegación. Así, cambiar de pantalla no crea un segundo selector independiente.

## 4. DataStore conserva el valor

En `data/preferences/ThemePreferences.kt`:

```kotlin
suspend fun setDarkTheme(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[darkThemeKey] = enabled }
}
```

`suspend` permite ejecutar la operación mediante corrutinas, sin bloquear la interfaz mientras se escribe. `edit` actualiza la preferencia de forma transaccional. El archivo de preferencias se llama `theme_settings` y está separado de los mensajes y de la base de datos de tareas.

La propiedad de lectura tiene tipo `Flow<Boolean?>`. Un `Flow` comunica valores a lo largo del tiempo; el signo `?` permite que el valor sea `null`, que aquí significa «aún no se ha elegido un tema».

## 5. MainActivity aplica el valor a la app

En `MainActivity.kt`:

```kotlin
val settings by themeViewModel.uiState.collectAsStateWithLifecycle()
val darkTheme = settings.darkTheme ?: isSystemInDarkTheme()

ProcAppMVVMTheme(darkTheme = darkTheme) {
    // Superficie y navegación de la aplicación.
}
```

`collectAsStateWithLifecycle` convierte el flujo en estado que Compose puede observar mientras la pantalla está activa. Cuando cambia, Compose vuelve a evaluar las partes afectadas.

El operador `?:` usa el valor de la izquierda si existe; si es `null`, usa el de la derecha. Por tanto, una elección explícita tiene prioridad sobre el ajuste del teléfono.

`ProcAppMVVMTheme`, en `ui/theme/Theme.kt`, selecciona la paleta y la proporciona a los componentes mediante `MaterialTheme`. También comparte `LocalAppDarkTheme`, para que los colores específicos de las tarjetas respeten la elección y no consulten el teléfono por separado.

## 6. Primer paso de Serena: colores oscuros

En `ui/theme/Color.kt` se sustituyen los valores de la paleta oscura. Por ejemplo:

```kotlin
val md_theme_dark_primary = Color(0xFFA8DBC3)
val md_theme_dark_onPrimary = Color(0xFF17382A)
```

`primary` es el acento menta; `onPrimary` es el texto o icono situado encima de ese acento. En `0xFFA8DBC3`, `FF` indica opacidad completa y `A8DBC3` es el color RGB.

| Papel | Valor Serena | Uso |
|---|---|---|
| `background` | `#171E1C` | Fondo general |
| `surface` | `#202A26` | Superficies |
| `onSurface` | `#E8F2ED` | Texto principal sobre superficies |
| `onSurfaceVariant` | `#AFC3B8` | Texto secundario |
| `primary` | `#A8DBC3` | Acento de controles |
| `primaryContainer` | `#2C4035` | Contenedor destacado |
| `outlineVariant` | `#35483E` | Separadores suaves |

También se adaptan los pares secundarios, terciarios e inversos y los niveles `surfaceContainer`, `surfaceDim` y `surfaceBright`. Así los componentes Material que usan distintas superficies reciben tonos de Serena. Los colores de error se conservan.

En `ui/theme/Theme.kt`, `darkColorScheme` conecta cada valor con su papel. El selector evalúa primero `darkTheme -> DarkColors`: en oscuro siempre aplica Serena. Los colores dinámicos del sistema se mantienen disponibles solo en claro, para que Android 12 o posterior no sustituya Serena por una paleta derivada del teléfono. El modo claro y la preferencia guardada mantienen su funcionamiento.

Los componentes que consultan `MaterialTheme.colorScheme` reciben los nuevos colores automáticamente. Las tarjetas con colores propios por fecha, en `SetContainerColor.kt`, siguen pendientes de adaptar en el incremento de las filas de la agenda. Este paso solo aplica colores; la fuente se incorpora en el siguiente incremento.

## 7. Tipografía local DM Sans

Los cuatro archivos `.ttf` están en `app/src/main/res/font/`: `dm_sans_regular`, `dm_sans_medium`, `dm_sans_semibold` y `dm_sans_bold`. Android genera sus identificadores `R.font`, que utilizamos en `ui/theme/Type.kt`:

```kotlin
private val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)
```

`FontFamily` agrupa los archivos de una misma familia. Cada `Font` asocia un recurso con su peso: 400, 500, 600 o 700. Compose elige el archivo correspondiente al peso del texto. La negrita conserva el aspecto de los títulos que ya utilizaban `FontWeight.Bold`.

Los cuatro estilos personalizados conservan sus tamaños, interlineados, espaciados y pesos; solo cambia `fontFamily = DmSans`. Para los otros once estilos se utiliza, por ejemplo, `DefaultTypography.bodyMedium.copy(fontFamily = DmSans)`: `copy` mantiene los valores de Material y sustituye únicamente la familia. Así los 15 estilos comparten DM Sans y los menús o botones no se quedan con la fuente anterior.

`Theme.kt` ya entrega `Typography` a `MaterialTheme`, por lo que no necesita cambios para la fuente. La fuente se aplica tanto en claro como en oscuro y queda incluida en el APK para funcionar sin conexión. Las formas se incorporan en el siguiente incremento; la distribución se trabaja después.

Procedencia: archivos estáticos sin modificar del [repositorio oficial DM Fonts](https://github.com/googlefonts/dm-fonts/tree/d0520ba03bd780f5dccb3024854463d44f699b78/Sans/fonts/ttf), revisión `d0520ba03bd780f5dccb3024854463d44f699b78`. Su licencia SIL OFL 1.1 se incluye también en el APK como `assets/licenses/dm_sans_OFL.txt`. Referencia de implementación: [fuentes locales en Compose](https://developer.android.com/develop/ui/compose/text/fonts).

## 8. Formas compartidas de Serena

`ui/theme/Shape.kt` define `SerenaShapes`: `small` tiene esquinas de 8 dp, `medium` de 12 dp y `large` y `extraLarge` de 16 dp. `extraSmall` conserva el valor predeterminado de Material. `dp` es la unidad de tamaño independiente de la densidad de pantalla.

En `Theme.kt`, la línea `shapes = SerenaShapes` entrega estas formas a `MaterialTheme` en ambos modos. Los componentes que utilizan las formas del tema las reciben automáticamente; por ejemplo, la tarjeta exterior de la agenda utiliza `medium`.

En `TaskItemContent.kt`, la tarjeta interior pasa de `RoundedCornerShape(8.dp)` a `MaterialTheme.shapes.medium` (12 dp). Los botones de completar y borrar reciben `shape = MaterialTheme.shapes.small` (8 dp). Sus acciones, colores y espaciados se conservan. Se elimina el import de `RoundedCornerShape` de ese archivo porque ya no se utiliza.

Este cambio centraliza la escala de redondeos. Las formas escritas explícitamente en otros componentes, como las burbujas del asistente, se revisarán al trabajar sus pantallas.

## Qué revisar al probar

Ignasi realiza las comprobaciones visuales en el móvil. Para ver los incrementos: ejecutar la app desde Android Studio. DM Sans se aplica en ambos modos; Serena se activa con «Tema oscuro» en Ajustes.

1. Abrir Ajustes desde el engranaje y cambiar el interruptor.
2. Volver a la agenda y comprobar tanto el fondo como las tarjetas.
3. Cambiar de filtro, visitar Ajustes y comprobar que se conserva al volver.
4. Cerrar y reabrir la app: la elección sigue guardada.
5. Mantener las mismas tareas y conversaciones en ambos modos.

Las pruebas de preferencias comprueban lectura, cambio y recuperación desde disco. Las del ViewModel incluyen un error de guardado y el reintento. Las de pantalla comprueban el interruptor y la acción de volver.

Documentación oficial: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore), [temas en Compose](https://codelabs.developers.google.com/jetpack-compose-theming).
