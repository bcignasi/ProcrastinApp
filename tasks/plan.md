# Plan: Serena y Refugio élfico como temas seleccionables

Fecha: 14 de octubre de 2026.
Estado: selector claro/oscuro y corrección de cabecera guardados en feature/theme-settings. La rama feature/serena-agenda incluye la base Serena (colores, DM Sans y formas), que Ignasi confirma ver en el móvil, y la barra inferior Agenda / Asistente. La navegación conserva el estado al cambiar de pestaña y distingue retomar el asistente de iniciar una sesión o edición nueva. Compilación de app y pruebas de interfaz correcta; 94 pruebas unitarias aprobadas, ocho nuevas; lint con 0 errores y 29 advertencias anteriores. Revisión visual de navegación a cargo de Ignasi e integración en master pendientes.
Tareas: [todo.md](todo.md).

## Resultado solicitado

**Primer incremento confirmado:** añadir un engranaje en la agenda y una pantalla Ajustes con un interruptor «Tema oscuro». Reutilizar las paletas clara y oscura existentes y guardar la elección. Antes de la primera elección se conserva el comportamiento actual de seguir el teléfono. Serena y Refugio élfico se implementarán después. La selección no debe perder filtros, conversación ni datos. La explicación del código forma parte de este trabajo para que Ignasi pueda aprender y revisarlo en Android Studio.

Conservar dos direcciones visuales: Serena original en oscuro y la propuesta inmersiva de inspiración élfica. Añadir Configuración → Apariencia para alternar entre ellas. El cambio debe aplicarse a la app y conservarse al cerrarla y abrirla.

Serena será la selección inicial propuesta. «Refugio élfico» es el nombre de trabajo del segundo tema. Consola e Imperio quedan fuera de esta implementación.

Las imágenes mostradas son conceptos: en Android se han aplicado los colores oscuros de Serena y DM Sans, conservando la distribución actual. La imagen élfica contiene texto y controles dibujados; sirve como referencia, no como fondo de pantalla directamente utilizable.

## Actualización del orden: Serena «Agenda primero»

Esta secuencia tiene prioridad sobre el orden de temas personalizados descrito más abajo. El trabajo inmediato mantiene una misma estructura: Agenda / Asistente en la barra inferior, Conversación / Propuesta dentro del asistente y Ajustes desde el engranaje superior. La agenda será la pantalla inicial tras el arranque.

0. **Cerrar Ajustes:** verificar y corregir el espacio para filtro y engranaje, ejecutar la prueba de cabecera estrecha y guardar la integración pendiente en `feature/theme-settings`.
1. **Base Serena:** aplicar paleta, tipografía y formas al modo oscuro existente, manteniendo el selector claro/oscuro. No cambiar consultas ni navegación en este paso.
2. **Navegación inferior:** introducir Agenda / Asistente; conservar filtros, conversación, acceso a edición y comportamiento del botón atrás y teclado.
3. **Agenda:** rediseñar filas con datos reales, título principal y fecha/hora/prioridad secundarios. Mantener subtareas, notas y acciones accesibles.
4. **Filtros:** adaptar Pendientes / Todas / Hechas conservando los filtros por fecha, prioridad y notificaciones. La consulta actual `ALL` significa pendientes; «Todas» incluyendo completadas necesita una definición y verificación propias. Separar cualquier cambio de consulta del cambio puramente visual.
5. **Conversación:** pulir mensajes, entrada de texto y estados de carga/error utilizando las acciones existentes.
6. **Propuesta:** presentar pasos, subtareas y fechas con claridad; conservar los ajustes mediante conversación y Guardar en agenda; comprobar el recorrido completo.

Cada incremento termina con comprobación concreta, explicación del código a Ignasi y un commit pequeño. La rama `feature/serena-agenda` se ha creado a partir de `feature/theme-settings`, conservando sus cambios; ninguna de las dos está integrada todavía en `master`.

Desde el 18 de octubre, Ignasi realiza las comprobaciones visuales en el móvil. El agente se encarga de las verificaciones del código y no hace capturas ni interactúa con la app para revisar su aspecto salvo petición expresa.

Decisión del 19 de octubre para la agenda: conservar las Card y mantener dos presentaciones activas en el mismo código, `TaskPresentation.FLAT` y `CARDS`. Agenda utiliza FLAT; los componentes compartidos y el Asistente conservan CARDS por defecto. Cambian fondo, forma, elevación, espaciado y separadores, sin modificar Task ni la relación de subtareas. El selector de presentación en Ajustes queda para otro incremento, independiente del tema claro/oscuro. [Explicación del código](../docs/development/presentacion-tareas.md).

Las agrupaciones de objetivo aprovecharán la relación tarea/subtareas existente. Las barras de la maqueta son ilustrativas: no requieren construir proyectos de vida ni introducir hitos o porcentajes ficticios. El acabado claro y la selección de estilo élfico quedan como evolución posterior.

Referencia elegida: [Serena Agenda primero](../docs/design/references/serena-agenda-primero.png). Tareas y criterios actualizados en [todo.md](todo.md).

## Base técnica y antecedentes

- `ui/theme/Theme.kt` centraliza MaterialTheme: Serena mantiene su paleta oscura fija y los colores dinámicos del sistema siguen disponibles en claro.
- `ui/theme/Type.kt` aplica DM Sans local a los 15 estilos Material, conservando tamaños e interlineados anteriores. `Shape.kt` centraliza las formas; Theme.kt las aplica y TaskItemContent.kt las utiliza en tarjeta y botones. La distribución se trabaja después.
- `MainActivity.kt` aplica el tema alrededor de la navegación: es el punto para observar la apariencia elegida.
- `AppNavigation.kt` contiene el Scaffold con la barra inferior, agenda, splash y Ajustes. `MainNavigation.kt` agrupa las rutas del asistente y las acciones de guardar/restaurar estado. [Explicación del código](../docs/development/navegacion-agenda-asistente.md).
- DataStore Preferences ya está incluido. `ThemePreferences.kt` guarda la luminosidad en `theme_settings`, separada de los mensajes. Sus tres pruebas y las tres del ViewModel pasaron, con 86 pruebas unitarias del conjunto aprobadas.
- `SetContainerColor.kt` ya consulta `LocalAppDarkTheme` para respetar la luminosidad elegida. Sus colores por fecha todavía requieren adaptación a Serena.

En el AQUOS se comprobaron ambos modos y la selección tras cerrar el proceso. El 18 de octubre se reprodujo el fallo de la flecha con la prueba estrecha, se corrigió y pasaron las tres pruebas de interfaz. Compilación y lint correctos: cero errores y 29 advertencias existentes. Se verificó además abrir Ajustes y volver conservando un filtro distinto del inicial, y se restauró la vista original.

## Decisiones de diseño e implementación

1. Una misma aplicación y un mismo comportamiento con dos temas. Compartir composición de tareas, filtros, navegación y acciones; variar paleta, tipografía, fondos, formas y ornamentos mediante la capa visual.
2. Guardar un identificador estable de apariencia (`serena`, `elven`) en DataStore. Un valor ausente o desconocido utiliza Serena. No modificar los datos de tareas ni el almacén de mensajes.
3. Ambas propuestas son oscuras. Los colores dinámicos del teléfono no deben sustituir sus paletas. Evitar depender del modo claro/oscuro del sistema dentro de los componentes del diseño.
4. Configuración tendrá dos opciones con nombre, una previsualización y una selección accesible. Acceso mediante un engranaje en la agenda; volver conserva el estado de la agenda.
5. Serena mantiene la paleta documentada, DM Sans y su lista tranquila. La maqueta orienta la jerarquía; no obliga a crear nuevos proyectos ni a eliminar los controles funcionales actuales.
6. Refugio élfico conserva la ambientación inmersiva: ciudad nocturna, arcos, vegetación, filigranas, oro tenue y superficies oscuras. Preparar ilustración sin textos, adornos reutilizables y fuentes locales; títulos, botones y casillas siguen siendo componentes nativos reales.
7. Ajustar ornamentos y fondos al tamaño disponible. Las tareas deben poder crecer, desplazarse y funcionar con texto ampliado. No estirar una captura completa ni fijar zonas táctiles sobre texto dibujado.
8. Tras la agenda, extender la coherencia visual al asistente, revisión de propuestas, diálogos y Configuración, y revisar el arranque y las barras del sistema.

## Evolución de estilos tras Serena

Una vez terminado el recorrido Agenda primero, se prepararán los recursos élficos y se ampliará el selector para elegir estilo. Se conservarán preferencias y datos existentes. La referencia élfica se llevará a componentes reales y se verificará en el AQUOS, igual que Serena.

## Verificación y aceptación final

- Cambiar de apariencia actualiza las pantallas sin reiniciar ni perder la conversación, las tareas o su completado.
- La selección se recupera después de cerrar el proceso. La primera apertura y un identificador desconocido usan Serena; no aparece un tema ajeno durante la carga.
- Ambas agendas muestran los datos reales y conservan completar, expandir subtareas, editar, borrar y filtrar.
- Los textos siguen siendo legibles con fuente ampliada, los controles son accesibles y los ornamentos no interceptan pulsaciones.
- Recursos incluidos en la app y funcionamiento visual sin red. Comprobar desplazamiento y memoria en el AQUOS real.
- Ejecutar pruebas centradas en lectura, escritura y recuperación de preferencias; probar cambio y navegación con Compose; ejecutar `:app:testDebugUnitTest`, `:app:assembleDebug` y `:app:lintDebug` al cerrar la implementación. Comparar lint con su estado previo.
- Instalar una actualización sobre la app existente para comprobar conservación de datos; Ignasi revisará visualmente ambos temas en el móvil.

## Riesgos concretos

| Riesgo | Tratamiento |
|---|---|
| La maqueta élfica es una ilustración estática; su texto y sus marcos no se adaptan a contenido real. | Separar arte decorativo de componentes interactivos y validar con tareas largas y listas grandes. |
| Colores fijos en componentes y colores dinámicos pueden romper la coherencia. | Usar valores visuales compartidos y revisar las referencias directas a colores/modo del sistema. |
| Fondos grandes y muchas capas pueden afectar al AQUOS. | Optimizar y empaquetar los recursos, limitar efectos y revisar desplazamiento en el dispositivo. |
| Implementar el aspecto cambia accidentalmente las acciones existentes. | Reutilizar ViewModels y repositorios; comprobar las acciones reales en ambos temas. |

## Referencias conservadas

- [Serena original en oscuro](../docs/design/references/serena-original-oscuro.png).
- [Refugio élfico inmersivo](../docs/design/references/refugio-elfico-concepto.png).
- [Dirección de producto y decisiones visuales](../docs/ideas/proyectos-de-vida.md).

## Pendientes de afinar durante el diseño

- Ajustar la densidad de ornamentación élfica en pantallas pequeñas conservando su carácter inmersivo.
- Concretar las fuentes y el recorte adaptable de la ilustración élfica al preparar los recursos.

Estas cuestiones no impiden empezar por la preferencia persistente, la estructura común y el selector.
