PROMPTS ENVIADOS AL ASISTENTE IA
--------------------------------

Hola, tengo muchas tareas para mañana, a ver si me las distribuyes por horas. Tengo que ir a
despertar a Ignasi, hacer la comida, comer con Mary, hacer la siesta, ir a la biblioteca y pasear
al perro.

Añade a esa lista que quiero sacarme el carnet de conducir en septiembre, hazme una distribución
de subtareas para esta tarea.

RESPUESTA DE LA IA (FORMATO JSON)
----------------------------------

{
  "comentario": "He añadido la tarea de sacarte el carnet de conducir en julio a tu lista.
                 Aquí te propongo una distribución de subtareas para esa tarea.
                 Si deseas modificar algo, házmelo saber.",
  "propuesta": {
    "tasks": [
      {
        "title": "Despertar a Ignasi",
        "notify": "2025-04-01T07:30:00",
        "notes": "Hora tentativa: 8:00 AM"
      },
      {
        "title": "Hacer la comida",
        "notify": "2025-04-01T11:00:00",
        "notes": "Preparar la comida del día"
      },
      {
        "title": "Comer con Mary",
        "notify": "2025-04-01T12:45:00",
        "notes": "Hora tentativa: 13:00 PM"
      },
      {
        "title": "Hacer la siesta",
        "notify": "2025-04-01T15:00:00",
        "notes": "Descansar un rato después de comer"
      },
      {
        "title": "Ir a la biblioteca",
        "notify": "2025-04-01T16:30:00"
      },
      {
        "title": "Pasear al perro",
        "notify": "2025-04-01T18:00:00"
      },
      {
        "title": "Sacarme el carnet de conducir en julio",
        "deadline": "2025-07-31",
        "priority": "alta",
        "notify": "2025-07-01",
        "subtasks": [
          {
            "title": "Inscribirme en la autoescuela",
            "deadline": "2025-05-15",
            "notify": "2025-05-01"
          },
          {
            "title": "Estudiar para el examen teórico",
            "deadline": "2025-07-10",
            "periodicity": "1 hora diaria",
            "notify": "2025-07-01"
          },
          {
            "title": "Practicar conducción con instructor",
            "deadline": "2025-07-25",
            "periodicity": "según calendario",
            "notify": "2025-07-01"
          }
        ]
      }
    ]
  }
}
