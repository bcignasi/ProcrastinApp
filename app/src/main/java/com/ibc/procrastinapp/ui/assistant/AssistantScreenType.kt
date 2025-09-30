/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import androidx.annotation.StringRes
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.ui.common.DropdownMenuOption

/**
 * Tipos de vistas disponibles para la lista de tareas.
 * Usa el interfaz DropdownMenuOption para generalizar su funcionamiento
 * @property titleResId Código de recurso del título que se muestra en la interfaz de usuario
 */
enum class AssistantScreenType(@StringRes override val titleResId: Int) : DropdownMenuOption {
    MESSAGES(R.string.assistant_type_Messages),
    TASKS(R.string.assistant_type_Tasks),
    ;

//    companion object {
//        /**
//         * Obtiene el siguiente tipo de lista en la secuencia.
//         * Útil para implementaciones de rotación entre tipos.
//         */
//        fun AssistantScreenType.next(): AssistantScreenType {
//            val values = entries.toTypedArray()
//            val nextIndex = (ordinal + 1) % values.size
//            return values[nextIndex]
//        }
//    }
}