/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist

import com.ibc.procrastinapp.ui.common.DropdownMenuOption

/**
 * Tipos de vistas disponibles para la lista de tareas.
 * Usa el interfaz DropdownMenuOption para generalizar su funcionamiento
 * @property title Título que se muestra en la interfaz de usuario
 */
enum class TaskListQueryType(override val title: String) : DropdownMenuOption {
    ALL("Todas las pendientes"),
    PAST("Tareas postergadas"),
    NOW("Tareas inmediatas"),
    FUTURE("Tareas futuras"),
    PRIORITY("Tareas prioritarias"),
    NOTIFICATION("Con notificaciones"),
    NO_DATE("Tareas sin fecha"),
    COMPLETED("Tareas completadas"),
    ;

//    companion object {
//        /**
//         * Obtiene el siguiente tipo de lista en la secuencia.
//         * Útil para implementaciones de rotación entre tipos.
//         */
//        fun TaskListQueryType.next(): TaskListQueryType {
//            val values = entries.toTypedArray()
//            val nextIndex = (ordinal + 1) % values.size
//            return values[nextIndex]
//        }
//    }
}