# ProcrastinApp: metas acotadas y planificación conversada

Creado: 12 de octubre de 2026. Última revisión de dirección: 21 de octubre de 2026.

Estado: dirección de producto confirmada por Ignasi; experimento de prompt propuesto y pendiente de implementación. Esta revisión sustituye las prioridades anteriores de centrarse solo en el rediseño visual y mantener una aplicación genérica de tareas. No fija todavía el esquema de datos ni la nueva navegación.

## Dirección confirmada — 21 de octubre de 2026

El núcleo será ayudar a concretar una meta significativa, acordar un plan realista con el asistente y avanzar mediante acciones cotidianas. El usuario inicial es Ignasi; la utilidad personal y una demostración completa para el porfolio son los criterios de valor. La calidad del plan y su seguimiento tienen prioridad sobre continuar el rediseño visual.

| Nivel conceptual | Función | Ejemplo |
|---|---|---|
| Objetivo general, opcional | Agrupar metas relacionadas y conservar el propósito mayor. | Obtener el carné de conducir. |
| Meta, unidad principal | Conseguir un resultado acotado mediante varias acciones. | Aprobar el examen teórico. |
| Acción | Saber qué hacer en una sesión o momento concreto. | Consultar una autoescuela; decidir cómo preparar la teoría; estudiar un tema y revisar un test. |

- Se busca un alcance manejable, habitualmente de tamaño intermedio, sin imponer una duración ni un número fijo de tareas. Importan un resultado reconocible, varias acciones y la posibilidad de concretar cómo empezar.
- Una meta puede trabajarse de forma independiente. Aprobar la teoría debe poder tener plan, conversación y progreso propios aunque la práctica siga sin definir y aunque no se haya creado el objetivo general.
- Se conserva la opción futura de agrupar teoría y práctica bajo el carné. La jerarquía técnica, la presentación de esa agrupación y su progreso agregado siguen abiertos.
- La vista Metas recogerá las metas activas y Hoy sus acciones programadas. Los nombres y la distribución exacta de pantallas todavía se diseñarán.
- La barra debe explicar qué mide, por ejemplo acciones completadas del plan. Completar sesiones de estudio no equivale a aprobar el examen; el éxito de la meta se contrasta con su condición final. El cálculo y su presentación todavía deben especificarse.
- Las tareas sueltas genéricas, como recordatorios sin meta, dejan de ser el núcleo. Miscelánea se conserva como idea anterior, pendiente de decidir, sin condicionar esta primera versión.

### Cómo debe ayudar el asistente

1. Aprovechar lo que el usuario ya ha explicado. Si la meta es clara, reconocer su condición de éxito y permitir corregirla, sin volver a pedir la misma información por sistema.
2. Si el objetivo es amplio, proponer sus partes y acordar cuál trabajar primero. El usuario puede convertir solo una de ellas en una meta activa.
3. Avanzar con propuestas concretas que el usuario pueda aceptar, corregir o ralentizar. Preguntar cuando falten datos que cambien de forma relevante el plan.
4. Recomendar una fecha objetivo, aclarando si es orientativa o fija, y una condición final de éxito lo más concreta y comprobable posible. La fecha es opcional: ayudar a proponerla sin bloquear a quien todavía no la sabe.
5. Acordar punto de partida, disponibilidad, ritmo y restricciones. Para Ignasi, el ritmo relajado debe traducirse en carga asumible y margen, no solo en un tono amable.
6. Concretar por fases. Mantener reconocibles los acuerdos, las estimaciones provisionales y las decisiones pendientes; cambiar una fase no debe hacer olvidar lo acordado en otra.
7. Detallar primero las próximas acciones y mantener revisables las partes lejanas. Si una parte no está clara, poder dejarla para después y cerrar una meta más pequeña que ya permita empezar.
8. Explicar las estimaciones. Una propuesta de plazo no es una media estadística. Para afirmar qué es habitual, usar referencias verificadas y distinguir preparación, esperas externas y variación personal; si faltan datos fiables, reconocerlo y acordar una previsión provisional.
9. Mostrar un resumen corregible antes de preparar el borrador del plan. Revisar el borrador y guardarlo son pasos distintos; la app conserva la confirmación de guardado del usuario.

### Primer caso: aprobar el teórico

Resultado deseado: aprobar el examen teórico del permiso que corresponda al usuario. País, tipo de permiso y situación inicial deben aclararse cuando afecten al contenido; no se presuponen por el idioma.

- Primera propuesta de arranque: esta semana informarse en una autoescuela y decidir si preparar la teoría por cuenta propia o con la autoescuela. El usuario puede modificarla o indicar que ya lo ha hecho.
- Después, acordar recursos, dedicación y un horizonte de preparación. «Un mes» es una posibilidad para discutir, no una media comprobada ni una duración impuesta.
- Crear acciones concretas de estudio según lo acordado, con un punto de revisión. La fecha real del examen puede depender de información que todavía no se conoce.
- La práctica queda pendiente y no debe introducirse como un conjunto de tareas dentro de esta meta salvo que el usuario amplíe explícitamente el alcance.

La aspiración es completar un recorrido pequeño y funcional en aproximadamente una semana de trabajo con disponibilidad completa de Ignasi. Es un objetivo de alcance, no una fecha de publicación comprometida. Primero se evaluará la planificación con sus metas reales; después se especificarán los cambios de datos y presentación necesarios.

## Experimento de prompt propuesto: `metas-conversadas-v1`

Estado: borrador para revisar antes de implementar. El prompt de producción, la base de datos, la interfaz y el modelo de IA no se cambian con esta documentación. La prueba evaluará conversación y planificación de una meta; no implementa todavía Metas, conversaciones separadas ni agrupaciones.

### Bloque de comportamiento que sustituiría las instrucciones genéricas

```text
Ayuda al usuario a convertir una meta significativa y acotada en un plan
realista que pueda empezar y revisar contigo. Usa el idioma del usuario.

ALCANCE
- Trabaja la meta activa. Si el objetivo es amplio, propón sus partes y
  acuerda con el usuario cuál concretar primero.
- Una parte puede ser una meta independiente. Conserva el contexto mayor,
  pero deja fuera del plan las partes que todavía no se hayan acordado.

CONVERSACIÓN
- Aprovecha la información ya conocida y avanza con una propuesta breve
  que el usuario pueda aceptar o modificar.
- Aclara solo los datos que cambien el plan. Como pauta inicial, haz una
  o dos preguntas relevantes por turno, sin repetir las ya contestadas.
- Confirma el resultado que significa éxito. Recomienda una fecha objetivo
  y aclara si es flexible o fija; no obligues a elegirla para poder empezar.
- Acordad punto de partida, disponibilidad, restricciones y ritmo.
- Si faltan decisiones o recursos, propón una acción para averiguarlos.
- Antes del borrador de tareas, resume el alcance, los acuerdos y lo que
  sigue provisional. Espera la conformidad del usuario y acepta correcciones.

CALIDAD DEL PLAN
- Acuerda una fase cada vez cuando eso facilite la planificación.
- Detalla primero lo próximo; deja el resto como previsión revisable.
- Cada acción debe describir algo ejecutable y cómo reconocer su final.
- Ajusta la carga y el margen al ritmo acordado. Si la fecha y la dedicación
  no encajan, explica la tensión y ofrece alternativas sin prometer resultados.
- Distingue esfuerzo de preparación y esperas externas.
- Una estimación tuya es una propuesta. Solo presenta medias o cifras como
  verificadas si dispones de fuentes comprobadas en el contexto; no inventes
  estadísticas, citas, requisitos ni enlaces.
- Registra en notes los acuerdos y la condición de éxito que ayuden a
  entender la meta; no copies en notes la lista de subtasks.
- No marques completed por deducción: requiere confirmación del usuario.
- Usa deadline para fechas límite y notify solo para recordatorios acordados;
  no inventes horas ni notificaciones para rellenar campos.
```

Este bloque sería general para distintas metas. El carné se usaría como caso de evaluación, sin convertir todo el prompt en una plantilla exclusiva de autoescuelas. La pauta de una o dos preguntas es una propuesta inicial que se ajustará según las pruebas.

### Formato interno para la primera prueba

Se propone conservar `comentario` y `propuesta.tasks`, el formato que ya interpreta la app. Durante la conversación inicial el texto útil irá en `comentario`, con la lista vacía mientras no haya borrador. La burbuja actual muestra ese comentario y oculta el JSON reconocido.

```json
{
  "comentario": "Podemos centrarnos primero en aprobar el teórico. Te propongo empezar esta semana informándote en una autoescuela y decidir cómo prepararlo. ¿Ya has hecho alguna gestión y qué dedicación semanal te resultaría cómoda?",
  "propuesta": { "tasks": [] }
}
```

Después de acordar alcance y condiciones, se produciría un borrador con una tarea raíz «Aprobar el examen teórico» y sus acciones en `subtasks`, conservando los campos actuales de `Task`. Esta es una representación transitoria para probar el enfoque, no el nuevo modelo de metas. Las tareas nuevas llevarán `id=0`; al editar se pedirá conservar los identificadores originales, aunque esa regla de prompt no sustituye la conservación de identidad en el código.

Si ya hay un borrador y se está conversando sobre un ajuste, la respuesta debe mantener ese borrador sin cambios hasta proponer su revisión, en lugar de enviar una lista vacía y hacerlo desaparecer. El envío de un nuevo borrador sigue siendo una propuesta; la IA no guarda datos por sí misma. Se requiere un único objeto JSON válido, sin comentarios de código ni texto externo; el contenido conversacional va en `comentario`.

### Cambios de código que se revisarían antes de implementar

- `ChatAIService.buildTaskPrompt()`: sustituir el rol genérico, la acumulación indiscriminada de tareas y la duplicación en notas por el flujo anterior, manteniendo el contrato JSON e IDs. Añadir un ejemplo corto de entrevista y otro de borrador.
- `ChatAIService.sendMessage()`: retirar el recordatorio añadido a cada entrada del usuario que exige devolver todas las tareas actualizadas. Las reglas de salida deben vivir en el prompt, sin convertir cada intervención en una orden de generar un plan.
- Inicialización y `loadMessages()`: el historial persistido incluye el prompt anterior. Verificar el prompt realmente enviado y preparar una prueba aislada, preservando el historial y las tareas originales. Reiniciar la app no garantiza usar las instrucciones nuevas. La forma concreta de aislar la prueba está pendiente de implementación.
- Pruebas del servicio y de presentación: comprobar el formato enviado, la restauración de mensajes y que un comentario con `tasks: []` se muestra legible y sin una propuesta ficticia. Las pruebas con respuestas simuladas no demuestran la calidad de la IA real.

Límites observados: existe un único historial global y no hay búsqueda web conectada al asistente; el prompt no añade esa capacidad. Hoy selecciona actualmente por fecha de aviso, de modo que programar una acción y configurar un recordatorio aún no son conceptos separados en los datos. Esa separación se especificará al diseñar el nuevo seguimiento, sin forzar notificaciones desde el prompt. También debe verificarse la conservación de identidad al editar: el normalizador actual de `Task` restablece los IDs a cero, aunque el prompt pida preservarlos. El ensayo inicial usará una meta nueva y no sustituirá el plan guardado del carné.

### Cómo evaluar el experimento

- Partir de «Quiero aprobar el teórico y prefiero ir con calma». Debe proponer un arranque, reconocer el alcance y acordar la dedicación sin generar de inmediato un plan completo.
- Responder con disponibilidad y pedir un plazo orientativo. Debe negociar una propuesta y distinguirla de una media verificada. El ejemplo de un mes no debe convertirse en una regla fija.
- Indicar que todavía no se conoce la fecha final. Debe permitir avanzar con las primeras acciones y una revisión, dejando explícita esa incertidumbre.
- Aceptar el resumen y pedir el borrador. Debe contener solo la teoría, acciones ejecutables y acuerdos sin repetir las subtareas en las notas.
- Pedir después un ritmo más lento. Debe conservar el contexto, proponer los ajustes pertinentes y mantener accesible el borrador mientras se discuten.
- Comparar con el comportamiento anterior y comprobar durante el uso si las primeras acciones resultan útiles y asumibles. Probar otras dos metas antes de considerar generalizado el resultado.

Guardar para cada prueba el contexto, el prompt y modelo efectivos, la respuesta original, lo interpretado por la app y la valoración de Ignasi. Los registros personales se conservan localmente y fuera de Git; este documento solo registra las decisiones y criterios.

Referencia metodológica: [guía oficial de prompting](https://developers.openai.com/api/docs/guides/prompt-engineering) y [evaluación de respuestas](https://developers.openai.com/api/docs/guides/evaluation-best-practices). Son apoyo para instrucciones, ejemplos y comparación de resultados; no aportan medias de preparación del carné ni garantizan que el plan sea útil.

## Contrato JSON y comparación de modelos — propuesta del 21 de octubre de 2026

Estado actualizado el 21 de octubre: implementado el contrato de Structured Outputs del Asistente, manteniendo `gpt-4o-mini`. El nuevo prompt de entrevista y la comparación de modelos siguen pendientes. [Código, compatibilidad y pruebas](../development/structured-outputs.md).

### Separar comportamiento, estructura y coherencia

| Capa | Responsabilidad propuesta |
|---|---|
| Prompt | Guiar la entrevista, las propuestas, la negociación y la calidad del plan. |
| Structured Outputs con JSON Schema y `strict: true` | Exigir los campos, tipos, valores admitidos y estructura de la respuesta. |
| Validación de la app | Comprobar fechas, identidad de tareas, coherencia del plan y conservación de datos y borradores. |

La petición del Asistente ya envía `response_format` con JSON Schema y `strict: true`, conservando `comentario`, `propuesta.tasks` y los campos de las tareas y subtareas. Todos los campos son obligatorios y las subtareas reutilizan la definición de tarea. Las respuestas nuevas se validan antes de publicarlas o guardarlas; las frases motivacionales siguen usando texto libre y el historial antiguo conserva su lector tolerante. El modo JSON simple asegura JSON válido; Structured Outputs añade el cumplimiento de un esquema admitido.

El esquema debe expresar, entre otras reglas, texto para `comentario`, listas para tareas y subtareas, booleanos para `completed`, prioridades permitidas y rechazo de propiedades inesperadas. Las respuestas incompletas y las denegaciones requieren tratamiento específico antes de interpretar el contenido como una propuesta. Si una respuesta falla, debe mantenerse el último borrador válido; una respuesta inválida no debe confundirse con una lista vacía legítima durante la entrevista.

Un esquema correcto no garantiza un plan útil, una estadística real ni fechas compatibles con la vida del usuario. Estas cuestiones siguen necesitando contexto, validación y evaluación. Fuente: [Structured Outputs, documentación oficial](https://developers.openai.com/api/docs/guides/structured-outputs).

### Modelos a comparar

El modelo configurado actualmente es `gpt-4o-mini`. Los fallos del caso del carné no demuestran por sí solos una limitación del modelo: las instrucciones actuales favorecen crear tareas inmediatamente y ordenan repetir las subtareas en notas.

Recomendación a evaluar: utilizar `gpt-6.1-sol` como primera referencia de calidad para la planificación conversada y compararlo con `gpt-6-luna` como candidato económico para el uso habitual. Si Luna mantiene la calidad requerida en las pruebas, sería preferible por coste. Esta es una hipótesis de selección, no una superioridad demostrada en nuestra aplicación ni una decisión de migración ya tomada.

Precios consultados el 21 de octubre de 2026: USD por millón de tokens de texto, tarifas estándar de entrada sin caché y salida. Son una referencia fechada; deben revisarse al implementar y no equivalen al coste de una conversación.

| Modelo | Entrada | Salida | Papel propuesto en la evaluación |
|---|---|---|---|
| [GPT-4o mini](https://developers.openai.com/api/docs/models/gpt-4o-mini) | $0,15 | $0,60 | Referencia del comportamiento anterior. |
| [GPT-6 Luna](https://developers.openai.com/api/docs/models/gpt-6-luna) | $0,10 | $0,50 | Candidato económico para el uso habitual. |
| [GPT-6.1 Sol](https://developers.openai.com/api/docs/models/gpt-6.1-sol) | $2,00 | $10,00 | Primera referencia propuesta para evaluar calidad. |

La [guía oficial de modelos](https://developers.openai.com/api/docs/guides/latest-model) sitúa Sol como opción para trabajo complejo con equilibrio entre capacidad y coste, y Luna para tareas acotadas de alto volumen. No aporta una evaluación específica de nuestro planificador personal.

OpenAI ha anunciado la retirada de GPT-4o mini (`gpt-4o-mini-2024-07-18`) de la API para el 14 de enero de 2027, con `gpt-6-luna` como reemplazo recomendado. Fuente: [aviso oficial de retirada](https://developers.openai.com/api/docs/deprecations).

### Orden acordado y comparación pendiente

Ignasi ha autorizado implementar primero Structured Outputs. Después se recomienda probar el nuevo prompt de planificación conversada con la meta de aprobar el teórico y, con el mismo contrato y casos, comparar los modelos. La elección final sigue pendiente y debe basarse en calidad, tiempo de respuesta y coste medidos.

Para comparar modelos, usar el mismo prompt, esquema, casos y respuestas de usuario, registrando también la configuración efectiva de razonamiento cuando corresponda. Observar si preguntan lo necesario, mantienen acuerdos, respetan el ritmo relajado, proponen acciones ejecutables y justifican los plazos sin inventar medias. Registrar respuestas, errores, tiempo y consumo de tokens; el historial reenviado también influye en el coste. Cambiar el modelo no añade por sí solo búsqueda web ni fuentes verificadas.

## Antecedentes y exploraciones — 12 a 19 de octubre de 2026

Las secciones siguientes conservan decisiones y propuestas anteriores como contexto. Sus prioridades y valoraciones de implementación corresponden a esas fechas y quedan subordinadas a la dirección del 21 de octubre descrita arriba. En particular, Miscelánea, la organización global de toda la vida, los porcentajes por hitos y la navegación anterior no son compromisos nuevos de este experimento.

### Prioridad anterior: pulir un objetivo

Hay dos niveles distintos de la misma idea:

| Nivel | Ejemplo de entrada | Resultado esperado | Prioridad |
|---|---|---|---|
| Un objetivo concreto | «Quiero sacarme el carnet de conducir». | La IA desglosa el objetivo en tareas y subtareas y propone fechas u horas que el usuario revisa antes de guardarlas. | Actual: pulir la base que ya existe. |
| Organización global de la vida | «Estoy preparando entrevistas, el porfolio y el carnet; estos son mis objetivos y mi situación». | La IA identifica y clasifica varios proyectos y propone los pasos de cada uno. | Futuro: conservar la idea y desarrollarla después. |

**La siguiente versión se centra en el primer nivel.** Tener nuevos espacios por proyecto, conversaciones separadas, barras por hitos y una vista global no es una condición para terminar de pulir el flujo actual.

## Punto de partida visual confirmado — 14 de octubre de 2026

Ignasi elige la **primera propuesta Serena en oscuro** como punto de partida. El trabajo inmediato se centra en el aspecto visual: colores, tipografía, espaciado, jerarquía y componentes. El flujo funcional existente sigue siendo la referencia.

- Conservar la configuración inicial de Serena, incluida la fila de filtros **Pendientes, Todas y Hechas** y la navegación **Agenda y Asistente**.
- Mantener su paleta oscura: fondo `#171e1c`, superficies `#202a26`, texto `#e8f2ed`, texto secundario `#afc3b8`, separadores `#35483e`, selección `#2c4035` y acento `#a8dbc3`.
- Mantener la tipografía DM Sans, la lista compacta, los bordes suaves y la jerarquía tranquila de esa propuesta.
- La variante con pestañas Carnet, Trabajo IT y Ejercicio se conserva como exploración para la evolución futura. No sustituye ahora los filtros de estado ni amplía el alcance funcional.
- El contenido y el contador de pasos de la maqueta son ejemplos visuales; no definen un nuevo modelo de proyectos, hitos o progreso.
- Revisar primero la agenda y después aplicar el mismo lenguaje visual al asistente y a la propuesta de tareas.

Esta elección establece una dirección de diseño. La maqueta todavía no está implementada en la aplicación Android.

### Ampliación confirmada: dos apariencias seleccionables

Después de explorar las tres temáticas inmersivas, Ignasi decide conservar **Serena original en oscuro** y la propuesta de **inspiración élfica**, con el nombre de trabajo «Refugio élfico». Quiere alternar entre ambas desde Configuración y que la app recuerde su elección. Consola e Imperio no forman parte de esta primera implementación.

Ambas apariencias comparten tareas, subtareas, conversación y acciones. El tema élfico incluye la ambientación inmersiva de la referencia, además de sus colores: ilustración, ornamentación y tipografía. Hay que convertir la maqueta en componentes nativos y preparar sus recursos decorativos por separado.

El [plan de implementación](../../tasks/plan.md) recoge la preferencia persistente, el selector, la adaptación de las pantallas y la comprobación en el móvil. A 18 de octubre, ya existen Ajustes con selección claro/oscuro, la paleta oscura de Serena, DM Sans local y las formas compartidas. La nueva distribución Agenda / Asistente y el estilo élfico siguen pendientes.

## Motivación

Ignasi está organizando distintos proyectos de su vida mediante conversaciones separadas con ChatGPT/Codex: practicar entrevistas de ingeniería, desarrollar ProcrastinApp, preparar el porfolio y obtener el carnet de conducir. Cada conversación conserva un contexto y permite retomar un objetivo sin empezar desde cero.

La dirección a largo plazo es llevar esa forma de trabajar al móvil y hacerla más visual: saber qué proyectos están activos, en qué punto está cada uno, qué se ha conseguido y cuál es el siguiente paso. La agenda y las tareas existentes son la base para avanzar esos proyectos.

El usuario inicial es Ignasi. La siguiente versión debe hacer fiable y agradable la planificación de un objetivo individual, resultar útil en su vida diaria y servir como proyecto de porfolio. No se presupone conexión o importación de sus chats actuales de ChatGPT/Codex.

## Ideas de evolución futura que deben quedar conservadas

- Explicar a la IA la situación personal, los objetivos y los frentes abiertos; recibir una propuesta de pilares o proyectos de vida.
- Tener varios espacios o subpestañas, cada uno dedicado a un proyecto, con su propio contexto.
- Ver una barra de progreso por proyecto, tanto en la vista general como en su detalle.
- Dividir los proyectos en hitos, objetivos intermedios, tareas y subtareas.
- Poder retomar un proyecto viendo dónde se dejó y qué acción viene después.
- Incorporar una gamificación ligera que haga visible y satisfactorio el avance.
- Conservar una sección de **Miscelánea** para tareas puntuales, como comprar el pan, que no necesitan pertenecer a un proyecto.
- Dar al producto una presentación visual clara y cuidada. El diseño y la experiencia son una parte central de esta evolución.
- Ofrecer una opción **Ver diagrama** para visualizar el plan propuesto como mapa mental o diagrama de pasos, fuera de los mensajes del chat.

### Visualización del plan mediante un diagrama — idea registrada el 19 de octubre de 2026

Ignasi quiere poder consultar una propuesta de forma más visual después de que el asistente la haya generado. Esta opción también tiene sentido para un único objetivo; no depende de implementar antes la organización global de proyectos de vida.

- Partir de la propuesta existente y representar el objetivo, sus pasos y sus subtareas como un mapa mental o un diagrama.
- Ofrecer una acción explícita, por ejemplo **Ver diagrama**, que abra una vista dedicada. El diagrama no debe aparecer incrustado dentro de un mensaje de la conversación.
- Explorar también la posibilidad de solicitar esa visualización al asistente; el acceso concreto queda pendiente de diseño.
- Ejemplo ilustrativo: **Carnet de conducir → Teoría / Práctica → tareas de cada bloque**.
- Mantener la correspondencia con el mismo plan que se está revisando, en lugar de generar una planificación distinta al visualizarlo.

Estado: idea futura, sin implementación en el rediseño actual de Serena. El tipo de diagrama, la ubicación de la acción y la tecnología se decidirán cuando se retome esta funcionalidad.

## Dirección actual: pulir el flujo de un objetivo

La decisión es mejorar el producto actual antes de ampliar su alcance. Su recorrido principal ya encaja con el caso de uso: contar un objetivo al asistente, recibir una propuesta de tareas y subtareas, revisarla, guardarla en la agenda y avanzar en ella.

El trabajo inmediato debe cuidar:

- **Conversación útil:** entender el resultado que se busca y pedir aclaraciones cuando falten datos relevantes, en lugar de inventar una planificación precisa.
- **Desglose comprensible:** pasos concretos, subtareas ordenadas, prioridades y notas que ayuden a actuar sin abrumar.
- **Fechas y horas revisables:** utilizar la disponibilidad, las preferencias y los plazos que indique el usuario; presentar los horarios como una propuesta modificable.
- **Revisión antes de guardar:** mostrar de forma clara qué se propone y facilitar correcciones de títulos, subtareas y fechas.
- **Guardado y edición fiables:** conservar los datos originales cuando falle una operación y evitar pérdidas o duplicados.
- **Seguimiento claro:** encontrar las tareas en la agenda, identificar el siguiente paso y completar o retomar acciones fácilmente.
- **Diseño y experiencia:** rediseñar el asistente, la propuesta y la agenda con una jerarquía visual coherente, estados de carga y error claros y acciones visibles.
- **Recordatorios:** cerrar los problemas de permisos, cancelación y reprogramación ya detectados.

El modelo actual permite guardar fechas con hora en los campos de tareas. Eso no equivale todavía a un calendario completo con duración de bloques, disponibilidad y detección de solapamientos. Esa precisión adicional tendrá que especificarse si se decide incorporarla.

Se pueden diseñar componentes reutilizables pensando en la evolución futura, sin exigir ahora un nuevo modelo completo de proyectos ni una clasificación global de la vida.

## Dirección futura: organizar todos los proyectos de vida

El paso macro amplía el mismo mecanismo del asistente: a partir de una conversación sobre la vida y sus objetivos, proponer varios proyectos y después desglosar cada uno. La IA propone la clasificación; el usuario puede corregirla y confirmarla. Esta capacidad se añadirá cuando el flujo de un objetivo esté suficientemente pulido.

Conviene distinguir dos conceptos:

| Concepto | Ejemplo | Qué significa el avance |
|---|---|---|
| Área o pilar continuo | Carrera, salud, aprendizaje | Reúne proyectos; no tiene un final ni un porcentaje de «vida completada». |
| Proyecto con un resultado | Conseguir el carnet, publicar el porfolio | Tiene hitos que permiten mostrar un progreso hacia ese resultado. |

**Propuesta para la primera etapa de esa evolución futura:** empezar con proyectos de vida directamente, sin exigir una clasificación adicional por áreas. Los ejemplos de Ignasi encajan en este nivel. Si agruparlos por pilares aporta claridad, se puede añadir esa capa posteriormente.

## Experiencia propuesta para la evolución futura

### Vista general: Mis proyectos

Cada proyecto tiene una tarjeta con nombre, resultado deseado, barra y porcentaje, hitos completados, siguiente paso y estado. Ejemplos: Carnet de conducir, Entrevistas de ingeniería, Porfolio y ProcrastinApp.

La información principal debe responder a «¿en qué punto estoy?» y «¿qué puedo hacer ahora?». Un proyecto sin planificación muestra «Sin hitos definidos» en lugar de inventar un porcentaje.

### Espacio propio de cada proyecto

Cada proyecto dispone de un espacio independiente con:

- Resumen del objetivo, situación actual y último avance confirmado.
- Barra de progreso e hitos pendientes y completados.
- Tareas y subtareas relacionadas, con las fechas y prioridades existentes.
- Notas y decisiones útiles para retomar el trabajo.
- Conversación con el asistente vinculada a ese proyecto.

Debe ser fácil cambiar de proyecto mediante subpestañas o un selector. La forma exacta de navegación se decidirá al diseñar las pantallas; el requisito es que el contexto de cada proyecto sea reconocible y accesible. Muchas pestañas pequeñas no deberían dificultar su uso en el móvil.

### Asistente para organizar y avanzar

La conversación inicial recoge los frentes de la vida del usuario. La IA propone nombres de proyectos, resultados, hitos y primeras acciones. La propuesta se puede editar y confirmar antes de guardarla.

Dentro de un proyecto, el asistente utiliza su objetivo, hitos y avances confirmados para ayudar a continuar. Puede sugerir nuevos pasos y resumir el estado, sin mezclar la conversación ni modificar otro proyecto por error.

Hablar con el asistente significa inicialmente conversar mediante texto, como permite la base actual. La entrada de voz queda como una posible evolución.

### Agenda y Miscelánea

La agenda conserva las tareas fechadas y permite reconocer a qué proyecto pertenece cada una. Miscelánea contiene tareas sin proyecto y no necesita una barra global. Una tarea puede asignarse después a un proyecto conservando sus datos y subtareas.

## Barras de progreso y gamificación: idea conservada para la evolución

La barra representa el avance del **plan del proyecto**, con una explicación visible como «1 de 2 hitos completados». No mide automáticamente esfuerzo, tiempo invertido ni rendimiento personal.

Ejemplo solicitado:

| Proyecto: Carnet de conducir | Estado | Peso inicial propuesto |
|---|---|---|
| Aprobar la teoría | Completado | 1 |
| Aprobar la práctica | Pendiente | 1 |

Con estos dos hitos de igual peso, el proyecto muestra **50 %**. Al completar ambos, muestra **100 %**. Esta división es una elección del plan; no afirma que teoría y práctica requieran el mismo esfuerzo real.

Reglas propuestas para cuando se implemente este alcance:

1. Calcular el porcentaje en la app a partir de los hitos confirmados, no aceptar un porcentaje inventado por la IA.
2. Empezar con hitos de igual peso. Los pesos editables son una decisión pendiente, no un requisito inicial de esta evolución.
3. Completar diez tareas de estudio no equivale a aprobar la teoría. El usuario confirma el hito; la app puede sugerir revisarlo cuando sus tareas estén terminadas.
4. Mostrar el cumplimiento de tareas por separado del progreso por hitos. Evitar contar una tarea y sus subtareas varias veces en la misma medida.
5. Si se desmarca un hito, recalcular el progreso. Si se añaden o eliminan hitos, explicar que ha cambiado el plan y puede cambiar el porcentaje.
6. Un proyecto sin hitos no tiene un porcentaje calculable. Un proyecto pausado conserva su progreso.
7. Mostrar avances con transiciones de la barra y pequeños reconocimientos al completar hitos. Rachas, puntos y niveles quedan para más adelante.

Una barra sencilla para el objetivo individual podría valorarse como mejora visual del flujo actual. No implica implementar antes toda la organización global ni asumir que el contador de subtareas mide por sí solo el avance real del objetivo.

## Qué existe para el flujo actual y qué falta para el futuro

Esta valoración se basa en la copia actual del repositorio, commit `6294394`.

El flujo individual ya tiene conversación con IA, extracción de tareas y subtareas, aprobación antes de guardar, fechas y horas, agenda, persistencia y acciones de completado. La prioridad es mejorar su calidad y su presentación.

La siguiente tabla describe las adaptaciones de la evolución futura, no requisitos nuevos de la siguiente versión:

| Necesidad futura | Base actual aprovechable | Adaptación necesaria |
|---|---|---|
| Descomponer objetivos en acciones | `Task` contiene tareas, subtareas, notas, fechas, prioridad y completado. | Vincularlas a proyectos e hitos; distinguir un hito de una tarea cotidiana. |
| Persistencia local | Room guarda `TaskEntity` con relación padre-hijo. | Añadir el modelo de proyectos e hitos y una migración que conserve los datos. |
| Proponer organización con IA | `AssistantRepository` procesa respuestas y extrae propuestas de tareas. | Extender las propuestas a proyectos e hitos, validar su estructura y confirmar los cambios. |
| Retomar una conversación | `ChatAIService` y `MessageStorageImpl` guardan una conversación. | Separar conversaciones y contexto por proyecto. Actualmente el almacenamiento utiliza una única clave de mensajes. |
| Calcular avance | `Task` cuenta subtareas directas y completadas. | Definir progreso por hitos y probar el cálculo; estos contadores no son una barra de proyecto ya implementada. |
| Navegar entre proyectos | Compose ya tiene navegación entre agenda y asistente. | Añadir vista general y espacios por proyecto, junto con el rediseño visual. |
| Próximos pasos y recordatorios | Existen fechas, filtros y alarmas. | Conservarlos al vincular tareas a proyectos y cerrar las carencias de recordatorios detectadas en la revisión. |

**Conclusión técnica:** hay una base útil y el flujo de un objetivo ya está implementado. Los proyectos como entidades propias, hitos explícitos, conversaciones independientes y barras de proyecto todavía no existen. Se incorporarán después; no es necesario construirlos para pulir lo que ya funciona.

## Mapa provisional de capacidades futuras

Este mapa conserva la posible estructura del alcance macro. No fija módulos técnicos, un calendario ni tareas para el trabajo inmediato.

| Capacidad | Responsabilidad | Dependencia |
|---|---|---|
| `life-projects` | Proyectos, hitos, vinculación de tareas y conservación de datos existentes. | Base actual de tareas. |
| `project-progress` | Progreso determinista por hitos y explicación del porcentaje. | `life-projects`. |
| `project-workspace` | Vista general, espacios o subpestañas y siguiente paso visible. | `life-projects`, `project-progress`. |
| `project-assistant` | Propuestas revisables y contexto de conversación por proyecto. | `life-projects`. |

Dentro de esa evolución, la base de proyectos precede a las demás capacidades. La navegación y el asistente deberán respetar ese modelo común. El orden y las tareas concretas se definirán cuando se retome ese alcance.

## Alcance de la siguiente versión: pulir lo existente

- Conversación centrada en un objetivo y propuesta de tareas y subtareas útil y comprensible.
- Fechas y horas válidas, explicables y corregibles antes de aceptar el plan.
- Revisión, guardado, edición y completado fiables.
- Rediseño visual del asistente, de la propuesta de tareas y de la agenda.
- Estados de carga, errores y reintento claros, junto con los recordatorios pendientes de cerrar.
- Un recorrido de demostración con el carnet de conducir y una demo reproducible para el porfolio.

La vista global de proyectos, el análisis de la vida completa, las conversaciones separadas por proyecto y el modelo de hitos quedan registrados como trabajo posterior.

## Alcance previsto para la evolución macro

- Proyectos editables manualmente y propuestas de creación mediante el asistente.
- Hitos con estado pendiente, en curso o completado, y tareas relacionadas.
- Vista general, espacio propio por proyecto y barra de progreso explicable.
- Contexto y conversación independientes para cada proyecto.
- Miscelánea y continuidad de la agenda existente.
- Edición manual y consulta del progreso sin depender de una llamada a la IA.
- Datos de ejemplo para poder demostrar el recorrido sin credenciales.

Dentro de esa futura evolución, quedarían para una etapa adicional la agrupación por áreas continuas, sincronización en la nube, importación de chats externos, colaboración, voz y sistemas extensos de puntos o rachas.

## Condiciones de calidad para el trabajo actual

- Guardar y editar propuestas de forma atómica, conservando los datos originales y la propuesta si falla el guardado.
- Validar fechas y horas; una fecha inválida no debe convertirse silenciosamente en «ahora».
- Mantener accesibles las tareas y subtareas existentes durante el rediseño.
- Para compartir una versión con IA real, resolver la gestión de credenciales detectada en la revisión; una demo sin credenciales permite mostrar el recorrido mientras tanto.

## Conservación de datos al implementar la evolución macro

- Las tareas existentes deben quedar accesibles inicialmente en Miscelánea y poder reasignarse conservando su identidad y subtareas.
- La base de datos actual está en versión 1 y su configuración permite una migración destructiva. Esta evolución necesita una migración explícita y probada que no borre datos.
- El índice único actual de tareas utiliza título, fecha y notificación de forma global. Habrá que revisar esa regla para permitir tareas equivalentes en proyectos distintos.
- Las modificaciones propuestas por la IA deben mantener las garantías de guardado y conservación de datos establecidas para el flujo individual.
- La información persistida del proyecto debe ser la fuente de su estado; una conversación no sustituye a los datos de hitos y tareas.
- Antes de enviar contexto personal a la IA, el usuario debe poder conocer qué contexto se utiliza. Para compartir una versión con IA real se debe resolver la gestión de credenciales detectada en la revisión.
- Los detalles de esquema y contratos de la evolución macro se especificarán cuando se retome esa capacidad.

## Criterios para demostrar el flujo individual pulido

1. Explicar «quiero sacarme el carnet de conducir» y recibir una propuesta con pasos y subtareas comprensibles.
2. Dar una disponibilidad y un plazo, y comprobar que la propuesta los tiene en cuenta o solicita los datos necesarios.
3. Identificar y corregir fechas, horas o pasos de la propuesta antes de guardarla.
4. Guardar el plan y localizar sus tareas en la agenda con su jerarquía reconocible.
5. Editar una propuesta o un plan guardado sin perder las tareas originales cuando se provoque un fallo de guardado.
6. Completar una subtarea, cerrar y abrir la app, y recuperar correctamente su estado.
7. Encontrar errores y opciones de reintento comprensibles cuando la IA o la red fallen.
8. Verificar el recorrido en el móvil Android real, incluyendo claridad visual, navegación y recordatorios.

## Criterios conservados para la experiencia macro futura

1. Crear los cuatro proyectos de ejemplo y reconocerlos en la vista general.
2. Abrir Carnet de conducir y ver sus hitos, su siguiente paso y su conversación.
3. Confirmar teoría completada y ver 50 % con la explicación «1 de 2 hitos completados».
4. Confirmar práctica completada y ver 100 %; desmarcarla y volver a 50 %.
5. Cerrar y abrir la app y recuperar proyectos, avance, notas y conversaciones.
6. Cambiar al proyecto Porfolio y comprobar que no hereda mensajes ni avances del carnet.
7. Revisar y corregir una propuesta de IA antes de que se guarde.
8. Guardar «comprar el pan» en Miscelánea y poder consultarlo en la agenda.
9. Consultar y actualizar manualmente proyectos y progreso sin conexión.
10. Migrar una base de datos con tareas y subtareas previas y comprobar que todas siguen disponibles.
11. Provocar un fallo de guardado de una propuesta y comprobar que los datos originales se conservan.

## Decisiones abiertas del trabajo actual

- Qué información mínima debe pedir el asistente para proponer fechas y horas útiles.
- Cómo presentar y editar el objetivo y sus subtareas con claridad.
- Cómo aplicar Serena en oscuro al asistente y a la propuesta, manteniendo la coherencia con la agenda.
- Si una barra sencilla de avance del objetivo aporta valor en este primer pulido y qué mediría exactamente.

## Decisiones abiertas de la evolución macro

- Nombre de los espacios: «Proyectos de vida», «Proyectos», «Pilares» u otro.
- Forma visual de las subpestañas o selector de proyectos y relación con la agenda.
- Si los hitos tendrán pesos editables en una evolución posterior.
- Cómo revisar el avance de objetivos abiertos, como practicar entrevistas, sin imponerles un final artificial. Una propuesta es convertirlos en un proyecto acotado y mantener la práctica continua como actividad.
- Qué contexto conservar y enviar por proyecto, y cómo mostrarlo de forma comprensible.
- Qué dirección visual y tono de gamificación resultan motivadores sin aumentar la presión.

El siguiente trabajo inmediato se centra en pulir el aspecto visual con Serena en oscuro, empezando por la agenda. El recorrido existente de un objetivo, usando el carnet de conducir como ejemplo, sigue siendo la base funcional. La vista general de proyectos y sus espacios independientes se diseñarán cuando se retome la evolución macro.
