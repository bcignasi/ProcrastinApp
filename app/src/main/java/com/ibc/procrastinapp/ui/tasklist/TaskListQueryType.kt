/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist

import androidx.annotation.StringRes
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.ui.common.DropdownMenuOption

/**
 * Tipos de vistas disponibles para la lista de tareas.
 * Usa el interfaz DropdownMenuOption para generalizar su funcionamiento
 * @property titleResId Código de recurso del título que se muestra en la interfaz de usuario
 */
enum class TaskListQueryType(@StringRes override val titleResId: Int) : DropdownMenuOption {
    ALL(R.string.task_type_All),
    PAST(R.string.task_type_Past),
    NOW(R.string.task_type_Now),
    FUTURE(R.string.task_type_Future),
    PRIORITY(R.string.task_type_Priority),
    NOTIFICATION(R.string.task_type_Notification),
    NO_DATE(R.string.task_type_NoDate),
    COMPLETED(R.string.task_type_Completed),
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