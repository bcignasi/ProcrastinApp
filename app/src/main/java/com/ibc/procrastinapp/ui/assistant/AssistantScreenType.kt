/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import com.ibc.procrastinapp.ui.common.DropdownMenuOption

/**
 * Tipos de vistas disponibles para la lista de tareas.
 * Usa el interfaz DropdownMenuOption para generalizar su funcionamiento
 * @property title Título que se muestra en la interfaz de usuario
 */
enum class AssistantScreenType(override val title: String) : DropdownMenuOption {
    MESSAGES("IA Assistant (Messages)"),
    TASKS("IA Assistant (Tasks)"),
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