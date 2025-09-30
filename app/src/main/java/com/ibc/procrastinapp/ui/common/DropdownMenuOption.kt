/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import androidx.annotation.StringRes

/**
 * Definición de las opciones del menú desplegable
 * Esto es para generalizar su funcionamiento tanto en
 * la lista de tareas como en el asistente
 */
interface DropdownMenuOption {
    @get:StringRes // Buena práctica para indicar que es un ID de recurso de string
    val titleResId: Int // Cambia 'title: String' por 'titleResId:Int'

}