package com.ibc.procrastinapp.data.service
/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */

/**
 * Interfaz que define un parser para mensajes del asistente IA.
 *
 * Esta interfaz permite abstraer el mecanismo de análisis de los mensajes
 * del asistente, separando la lógica de extracción de componentes (texto, JSON,
 * comentarios) de su uso. Esto facilita:
 *
 * 1. Cambiar la implementación del parser sin afectar al resto del código
 * 2. Realizar pruebas unitarias utilizando implementaciones simuladas
 * 3. Adaptar la aplicación a cambios en el formato de respuesta de la IA
 */
interface AssistantResponseParser {
    /**
     * Analiza un mensaje del asistente y extrae sus componentes principales.
     *
     * @param message El mensaje completo del asistente a analizar
     * @return Un objeto AssistantResponse con los componentes extraídos:
     *         - text: Texto principal sin incluir el JSON
     *         - json: Contenido JSON encontrado en el mensaje (si existe)
     *         - commentary: Comentario extraído del JSON (si existe)
     */
    fun parse(message: String): AssistantResponse
}
