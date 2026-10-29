# Agenda diaria y propuesta compacta

29 de octubre de 2026 · Rama `feature/prompt-metas-v1`.

## Hoy y Agenda

- Hoy muestra actividades pendientes con `scheduledDate` igual al día local: tareas raíz, hijos o descendientes de cualquier nivel.
- Si el día planificado está vacío, se usa el día de `notify` como compatibilidad con tareas antiguas. Si ambos existen, manda `scheduledDate`, aunque el aviso sea de otro día.
- No se incluyen actividades atrasadas, futuras, completadas ni sin ambas fechas. `deadline` no determina el día de trabajo.
- El resultado es una lista plana de coincidencias: no arrastra hijos de otros días ni duplica sesiones dentro de su padre. Los elementos sin hora de aviso del día van primero; después, los avisos de ese día por hora, prioridad e identificador.
- Pendientes conserva la jerarquía completa. Las filas planas de Agenda muestran el día planificado y, si existe, el aviso con su campana, sin inventar horas o alarmas.
- Pulsar una actividad o editar una selección abre sus objetivos raíz completos en el Asistente, una sola vez por objetivo. Esto evita editar y guardar una subtarea como si fuera un objetivo independiente.
- La casilla y la acción de completar la selección solo actualizan `completed` de las actividades elegidas. No reconstruyen el árbol ni modifican el padre o los descendientes.

No se cambia el esquema de Room ni se migran datos. El refresco de día del ViewModel se conserva. No se genera ninguna recurrencia a partir de `periodicity`.

## Propuesta

Las notas se muestran inicialmente con un máximo de dos líneas y puntos suspensivos. Si hay texto que no cabe o metadatos adicionales, aparece «Ver más» / «Show more». Al desplegar se conserva todo el texto original y se muestran repetición, límite y prioridad aplicable. «Ver menos» vuelve a compactarlo. Una nota corta sin otros detalles no necesita botón. La cabecera del objetivo y cada subtarea usan el mismo componente; las casillas siguen siendo de solo lectura.

El prompt pide empezar las notas por la acción, dedicación y recurso, preferir una o dos frases y añadir únicamente detalles necesarios en un párrafo aparte. No se añaden campos JSON ni se borran notas anteriores. La vista previa es un recorte visual, no un resumen semántico ni una garantía de que una respuesta antigua empiece por lo esencial.

El prompt de Kotlin y los dos bloques del Markdown consolidado se mantienen iguales. La calidad y brevedad de nuevas respuestas requieren evaluación con la IA real; las pruebas automáticas no la sustituyen.

## Verificación

Pruebas de base de datos para sesiones anidadas sin aviso, precedencia de fechas, exclusión de otros días/completadas, ausencia de duplicación, conservación de jerarquía al completar y resolución del objetivo para editar. Pruebas Compose para fecha visible sin alarma, vista previa de dos líneas, desplegar/contraer y conservación de los detalles en español e inglés con letra grande. Se conserva la prueba del cambio de día en el ViewModel.

La revisión visual en el móvil corresponde al usuario. No se borra el historial ni se envían solicitudes reales a la IA durante estas comprobaciones.

Resultado: 208 pruebas de la suite permanente pasan, sin fallos, errores ni omitidas (siete pruebas nuevas). Las pruebas iniciales reprodujeron cinco incumplimientos antes de corregir el código. `assembleDebug` y `lintDebug` terminan correctamente; lint conserva las 44 advertencias y una sugerencia anteriores. Se verifica también la igualdad del prompt Kotlin con los dos bloques de Markdown. APK preparado; esta entrega no se instala automáticamente en el móvil.
