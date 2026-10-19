# Tareas: cerrar Ajustes y construir Serena «Agenda primero»

Plan: [plan.md](plan.md). Orden revisado el 18 de octubre de 2026. Se conserva la evolución élfica al final; el objetivo inmediato es Serena.

## 0. Cerrar el selector claro/oscuro

- [x] Rama feature/theme-settings y dos commits de preferencias y ViewModel.
- [x] Persistencia y error de guardado verificados; 86 pruebas unitarias aprobadas.
- [x] Engranaje, Ajustes y ambos modos comprobados en AQUOS; selección recuperada tras reiniciar el proceso.
- [x] Dos pruebas de pantalla aprobadas y código explicado a Ignasi.
- [x] Corregir espacio de la flecha del filtro junto al engranaje; las tres pruebas de interfaz pasan en AQUOS.
- [x] Comprobar vuelta a la agenda conservando filtro y tema; compilación y lint correctos; guardar el commit de integración.
- [ ] Revisar e integrar los cambios en master. Serena continúa en una rama basada en feature/theme-settings, que conserva los ajustes.

Verificación realizada: narrowAgendaKeepsTheFilterArrowAndSettingsReachable y otras dos pruebas de interfaz, compilación y lint. Ignasi se encarga de las siguientes revisiones visuales en el móvil.
Archivos de la corrección: TitleDropdownMenu.kt y SettingsScreenTest.kt. Integración de Ajustes guardada en commits locales.
Dependencias: ninguna. Alcance de la corrección: pequeño.

## 1. Aplicar la base Serena

- [x] Colores oscuros de Serena en Color.kt y Theme.kt: compilación correcta, 86 pruebas unitarias aprobadas y lint con 0 errores y 29 advertencias.
- [ ] Ignasi revisa los colores de Serena y la alternancia claro/oscuro en el móvil.
- [x] Tipografía local DM Sans en los 15 estilos compartidos: compilación y 86 pruebas unitarias correctas; lint con 0 errores y 29 advertencias. Cuatro fuentes y licencia verificadas dentro del APK; pesos y caracteres del español comprobados.
- [ ] Ignasi revisa DM Sans en títulos, contenido, menús y botones de ambos modos.
- [x] Formas compartidas de Serena en Shape.kt y Theme.kt; tarjeta interior de 12 dp y botones de acción de 8 dp en TaskItemContent.kt. Compilación y 86 pruebas unitarias correctas; lint con 0 errores y 29 advertencias.
- [x] Ignasi confirma ver las formas en el móvil.
- [ ] El selector claro/oscuro funciona y el modo claro existente conserva legibilidad.

Verificación: :app:assembleDebug, pruebas unitarias y lint; Ignasi revisa la agenda real en el móvil.
Archivos de la base: Color.kt, Theme.kt, Type.kt, fuente local, Shape.kt y TaskItemContent.kt.
Dependencia: 0. Alcance: medio.

## 2. Navegar entre Agenda y Asistente

- [x] Barra inferior Agenda / Asistente conectada a la ruta actual; engranaje conservado e iconos superiores duplicados retirados.
- [x] Acabado Serena de la barra: iconos vectoriales de contorno, icono y texto en horizontal, selección rectangular de 12 dp y separador superior de 1 dp. Compilación, 94 pruebas existentes y lint correctos; revisión visual a cargo de Ignasi.
- [x] Seis pruebas de navegación y dos del ViewModel verifican estado, ViewModels retenidos, ausencia de duplicados y solicitudes explícitas de sesión/edición.
- [ ] Ignasi revisa las pestañas, filtro, borrador, edición, botón atrás, Ajustes y teclado en el móvil.

Verificación técnica completada: NavHostController real en Robolectric, 94 pruebas unitarias aprobadas (ocho nuevas), compilación de app y pruebas de interfaz, lint con 0 errores y 29 advertencias existentes. Revisión del resultado y del teclado en AQUOS a cargo de Ignasi.
Archivos: AppNavigation.kt, MainNavigation.kt, MainBottomBar.kt, ambas pantallas y cabeceras, AssistantViewModel.kt, manifiesto, textos y pruebas. [Guía del código](../docs/development/navegacion-agenda-asistente.md).
Dependencia: 1. Alcance: medio.

## Punto de control: base y navegación

- [x] Correcciones de auditoría: volver realmente a Agenda desde «+»/edición, guardado con ámbito de aplicación, botón conectado a isSaving y cabecera con insets superiores y laterales.
- [x] Validación técnica de los arreglos: assembleDebug, 107 pruebas unitarias sin fallos, lintDebug con 0 errores y las 29 advertencias anteriores, y compileDebugAndroidTestKotlin. Pruebas nuevas de navegación, cambio de pantalla durante guardado, finalización sin pantalla y separación de sesiones; revisión independiente sin regresiones nuevas verificables.
- [ ] Ignasi revisa los arreglos en el móvil: volver desde «+»/edición, guardar al cambiar de pantalla, estado del botón y cabecera en horizontal.

- [ ] Revisar en el móvil y explicar los cambios antes de rediseñar las filas.
- [ ] Commit por incremento, manteniendo la app funcional.

## 3. Rediseñar las tareas de la agenda

- [x] Conservar las Card y añadir TaskPresentation.FLAT/CARDS: Agenda activa FLAT, el Asistente conserva CARDS, las subtareas heredan la presentación. Sin selector nuevo en Ajustes. Compilación y 94 pruebas existentes correctas; lint con 0 errores y 29 advertencias anteriores. [Guía del código](../docs/development/presentacion-tareas.md).
- [ ] Ignasi revisa las filas planas, selección y subtareas en el móvil; el contenido y la jerarquía de títulos/fechas se pulen después.
- [x] Filas planas de la maqueta original: FlatTaskHeader con casilla conectada a completar, título en seminegrita, una única fecha de aviso con campana, etiqueta roja de prioridad alta/urgente e identificadores internos ocultos. Tarjetas conservadas; fecha de referencia propagada para actualizar al cambiar el día.
- [x] Validación técnica de filas y casillas: assembleDebug, 116 pruebas locales aprobadas (cuatro de interacción Compose/Robolectric y cinco de fechas nuevas), lintDebug con 0 errores y 22 advertencias, y compileDebugAndroidTestKotlin.
- [ ] Ignasi revisa las casillas, texto, fechas, avisos y subtareas de las filas Serena en el móvil, en claro y oscuro.
- [ ] Subtareas, notas y acciones de completar, editar y borrar siguen accesibles con datos reales, sin nuevos proyectos ni progreso ficticio.

Verificación: tareas largas, subtareas y texto ampliado en el móvil; :app:assembleDebug.
Archivos previstos: TaskItemContent.kt, TaskItemSelectable.kt, ShowTitleRow.kt, ShowDatesRow.kt, SetContainerColor.kt. Separar otros ajustes de metadatos si requieren más componentes.
Dependencia: 2. Alcance: medio por grupo de componentes.

## 4. Cabecera Serena y filtros Hoy / Pendientes / Hechas

- [x] Cabecera con marca, engranaje, Tu agenda y fecha; tres pestañas visibles Hoy / Pendientes / Hechas.
- [x] Hoy como vista inicial: pendientes con aviso de hoy y una única lista sin atrasadas. Pendientes usa ALL (incluye atrasadas y tareas sin aviso) y Hechas usa COMPLETED. Filtros internos anteriores conservados.
- [x] Los demás filtros permanecen en el código sin mostrarse en Agenda. Se conserva el rememberSaveable del filtro al volver de Ajustes o Asistente; cambiar de pestaña limpia la selección de tareas.
- [x] Actualizar fecha y consultas al cambiar el día; no cambiar la estructura ni el filtrado existente de subtareas.
- [x] Seis pruebas con Room en memoria para avisos, fechas distintas de límite/aviso, completado y jerarquía; tres del ViewModel para Hoy sin PAST, cambio de pestaña y cambio de día. Suite local actual: 118 pruebas aprobadas, cero fallos ni omitidas. App y pruebas instrumentadas compiladas; lint con 0 errores y 22 advertencias. Revisión visual en el móvil a cargo de Ignasi.
- [ ] Ignasi revisa cabecera, pestañas y listas en el móvil.

Verificación: datos con tareas pendientes y completadas; pruebas de consultas si cambian y comprobación en el móvil.
Archivos previstos: TaskListTopBar.kt, TaskListScreen.kt, TaskListQueryType.kt, TaskListViewModel.kt, TaskListState.kt, TaskListContent.kt, TaskList.kt, repositorio, textos y pruebas.
Dependencia: 3. Alcance: medio por incremento visual o funcional.

## 5. Pulir la conversación

- [x] Cabecera Serena del Asistente con marca, título, fecha, Ajustes y menú de reinicio; pestañas Conversación / Propuesta con Conversación inicial y selección conservada al volver de Ajustes. [Guía del código](../docs/development/cabecera-asistente.md).
- [x] Cabecera compacta según altura disponible y tamaño de fuente; conserva las pestañas y lleva Ajustes al menú para reservar espacio de escritura con teclado o ventana pequeña.
- [x] Ayudas breves en español e inglés para Conversación y Propuesta vacías; visibles mientras no haya contenido, sin contador al cambiar de pestaña.
- [x] Incorporar a la suite permanente las pruebas de listas vacías independientes y del borrador multilínea durante carga; 128 pruebas aprobadas y lint sin errores.
- [x] Campo de escritura Serena con esquinas de 16 dp, envío de contorno integrado y área táctil de 48 dp; conserva borrador y comportamiento de envío.
- [x] Burbujas Serena: usuario sin avatar y ancho máximo del 85 %, robot de contorno para el asistente, formas de 16 dp, superficies planas, DM Sans y separación uniforme de 12 dp; indicador de escritura alineado.
- [ ] Ignasi revisa las burbujas en el móvil, con mensajes cortos y largos y tamaño de letra ampliado.
- [x] Al abrir una tarea, Conversación muestra texto legible y conserva el JSON interno para Propuesta y el contexto de IA; comentario cuando existe e indicación localizada cuando solo hay JSON. Dos pruebas de reapertura español/inglés y ocho del formateador.
- [ ] Ignasi revisa abrir una tarea desde Agenda, leer el mensaje y volver a Propuesta en el móvil.
- [x] Evitar parejas consecutivas idénticas al reabrir una tarea y compactar las ya guardadas; conservar mensajes reales y JSON. Restaurar Propuesta al reabrir tras cancelar aunque el historial no emita. Cinco pruebas permanentes, incluida una tarea de diez pasos abierta ocho veces.
- [ ] Ignasi revisa que al reabrir la tarea no crezcan las copias idénticas y que sus diez pasos sigan en Propuesta.
- [ ] Ajustar el texto de apoyo cuando `propuesta.tasks` está vacío o contiene tareas rechazadas por el extractor; hallazgo de auditoría separado de las aperturas repetidas.
- [ ] Enviar, cancelar y gestionar carga/error conserva su comportamiento; el teclado deja accesible la escritura.

Verificación: conversación y estados representativos sin llamadas innecesarias a IA; :app:assembleDebug.
Archivos previstos: AssistantScreen.kt, AssistantTopBar.kt, AssistantMessagesViewMessageItem.kt, MessageInputField.kt.
Dependencia: 4. Alcance: medio.

## 6. Pulir la propuesta y verificar el recorrido

- [x] Cabecera del objetivo con contador/progreso real y filas de pasos numeradas, conexiones verticales, niveles anidados y casillas de solo lectura. Tareas independientes sin progreso ficticio; aviso en la fila y metadatos conservados en Detalles. [Guía del código](../docs/development/propuesta-serena.md).
- [ ] Ignasi revisa Propuesta con el objetivo real de diez pasos, desplazamiento, niveles anidados, Detalles, letra ampliada y ambos temas.
- [ ] Acabar la cabecera «Revisa tu plan», los comentarios y los botones de Propuesta siguiendo la maqueta.
- [ ] Pasos, subtareas, fechas y Guardar en agenda claros; volver a la conversación permite pedir ajustes.
- [ ] Objetivo → propuesta → guardado → agenda funciona con datos actuales; Ajustes y diálogos conservan legibilidad.

Verificación: pruebas pertinentes del flujo, :app:testDebugUnitTest, :app:assembleDebug, :app:lintDebug y capturas del AQUOS. Repetir solo comprobaciones afectadas por cambios posteriores.
Archivos previstos: AssistantTasksView.kt, AssistantTasksViewListItem.kt, AssistantTasksViewSaveToAgendaButton.kt, CommentsCard.kt; separar cualquier corrección funcional que aparezca.
Dependencia: 5. Alcance: medio por pantalla o componente.

## Evolución posterior conservada

- [ ] Opción **Ver diagrama** para abrir una representación visual del plan (mapa mental o diagrama de pasos) fuera de los mensajes del chat. [Idea y alcance](../docs/ideas/proyectos-de-vida.md#visualización-del-plan-mediante-un-diagrama--idea-registrada-el-19-de-octubre-de-2026).
- [ ] Completar el acabado claro de Serena.
- [ ] Preparar fondo, ornamentos y fuentes élficos separados de textos y controles.
- [ ] Extender las preferencias y Ajustes para seleccionar Serena / Refugio élfico conservando la elección existente.
- [ ] Aplicar y verificar los dos estilos en toda la app, con datos reales, accesibilidad y recursos sin red.
