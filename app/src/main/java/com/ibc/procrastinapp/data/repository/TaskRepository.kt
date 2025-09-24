/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.repository

import androidx.room.withTransaction
import com.ibc.procrastinapp.data.alarm.AlarmScheduler
import com.ibc.procrastinapp.data.local.AppDatabase
import com.ibc.procrastinapp.data.local.TaskDao
import com.ibc.procrastinapp.data.local.TaskEntity
import com.ibc.procrastinapp.data.mapper.TaskMapper.toEntity
import com.ibc.procrastinapp.data.mapper.TaskMapper.toTask
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.tasklist.TaskListQueryType
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskRepository(
    private val taskDao: TaskDao,
    private val database: AppDatabase,
    private val alarmScheduler: AlarmScheduler
) {
    /**
     * Selector del filtro para la consulta
     */
    fun getTasks(filter: TaskListQueryType = TaskListQueryType.PAST): Flow<List<Task>> {
        return when (filter) {
            TaskListQueryType.ALL -> getAllTasksFlow()
            TaskListQueryType.PAST -> getPastTasksFlow()
            TaskListQueryType.NOW -> getNowTasksFlow()
            TaskListQueryType.FUTURE -> getFutureTasksFlow()
            TaskListQueryType.PRIORITY -> getPriorityTasksFlow()
            TaskListQueryType.NOTIFICATION -> getNotificationTasksFlow()
            TaskListQueryType.NO_DATE -> getNoDateTasksFlow()
            TaskListQueryType.COMPLETED -> getCompletedTasksFlow()
        }
    }

    /**
     * TODAS LAS TAREAS PENDIENTES POSTERGADAS (PAST)
     * En la QUERY se recibe un parámetro "now"
     * En la consulta se compara deadline, y si no existe,
     */
    private fun getPastTasksFlow(includeHierarchy: Boolean = true): Flow<List<Task>> {
        val now = getNowDate()
        return processTaskQuery(taskDao.getPastTasks(now), includeHierarchy)
    }


    /**
     * TODAS LAS TAREAS PENDIENTES INMEDIATAS (NOW)
     * Obtiene todas las tareas con término inmediato (hoy, mañana, pasado mañana)
     * "now" cuenta desde las 00:00
     */
    private fun getNowTasksFlow(includeHierarchy: Boolean = true): Flow<List<Task>> {
        val now = getNowDate()
        val pastTomorrow = getRelativeDate(2)
        Logger.d("IBC-TaskRepository", "getNowTasksFlow: now=$now, pastTomorrow=$pastTomorrow")
        return processTaskQuery(taskDao.getNowTasks(now, pastTomorrow), includeHierarchy)
    }


    /**
     * TODAS LAS TAREAS PENDIENTES FUTURAS (FUTURE)
     * Obtiene todas las tareas pendientes con fecha futura o sin fecha.
     * getNowDate() no tiene en cuenta la hora; así, son futuras todas
     * las tareas de hoy desde las 00:00 (para dar margen a los retrasos de hoy)
     */
    private fun getFutureTasksFlow(includeHierarchy: Boolean = true): Flow<List<Task>> {
        val thirdDay = getRelativeDate(2)
        Logger.d("IBC-TaskRepository", "getFutureTasksFlow: thirdDay=$thirdDay")
        return processTaskQuery(taskDao.getFutureTasks(thirdDay), includeHierarchy)
    }

    /**
     * TODAS LAS TAREAS CON PRIORIDAD ALTA O URGENTE (PRIORITY)
     */
    private fun getPriorityTasksFlow(includeHierarchy: Boolean = true): Flow<List<Task>> =
        processTaskQuery(taskDao.getPriorityTasks(), includeHierarchy)

    /**
     * TODAS LAS TAREAS CON NOTIFICACIÓN (NOTIFICATION) FUTURAS PENDIENTES
     */
    private fun getNotificationTasksFlow(): Flow<List<Task>> {
        val now = getNowDate()
        return processTaskQuery(taskDao.getNotificationTasks(now), false)
    }


    /**
     * TODAS LAS TAREAS SIN FECHA (NO_DATE)
     */
    private fun getNoDateTasksFlow(includeHierarchy: Boolean = true): Flow<List<Task>> =
        processTaskQuery(taskDao.getNoDateTasks(), includeHierarchy)


    /**
     * TODAS LAS TAREAS COMPLETADAS (COMPLETED)
     */
    private fun getCompletedTasksFlow(includeHierarchy: Boolean = true): Flow<List<Task>> =
        processTaskQuery(taskDao.getCompletedTasks(), includeHierarchy)

    /**
     * Obtiene todas las tareas pendientes. (ALL)
     */
    fun getAllTasksFlow(includeHierarchy: Boolean = true): Flow<List<Task>> =
        processTaskQuery(taskDao.getPendingTasks(), includeHierarchy)


    // ===== Métodos CRUD para Tasks =====

    /**
     * Obtiene una tarea específica por su ID, incluyendo toda su jerarquía de subtareas.
     *
     * @param id ID de la tarea a recuperar
     * @return La tarea completa con todas sus subtareas o null si no existe
     */
    suspend fun getTask(id: Long): Task? {
        val entity = taskDao.getTaskByIdImmediate(id) ?: return null
        return buildTaskWithSubtasks(entity)
    }

    /**
     * Guarda una tarea y todas sus subtareas.
     *
     * @return ID de la tarea guardada, o -1 si falló
     */
    suspend fun saveTask(task: Task): Long {
        var taskId: Long = -1

        Logger.d("IBC-TaskRepository", "saveTask: task=$task")

        database.withTransaction {
            taskId = saveTaskWithSubtasks(task, null)
        }

        Logger.d("IBC-TaskRepository", "saveTask: taskId=$taskId")

        return taskId
    }

    /**
     * Actualiza una tarea existente.
     */
    suspend fun updateTask(task: Task) {
        // Si es una tarea simple sin jerarquía, hacemos un update simple
        if (task.subtasks.isEmpty()) {
            taskDao.updateTask(task.toEntity())
        } else {
            // Si tiene subtareas, eliminamos toda la jerarquía y la volvemos a crear
            database.withTransaction {
                taskDao.deleteTaskById(task.id)
                saveTaskWithSubtasks(task, null)
            }
        }
    }

    /**
     * Marca una tarea como completada o pendiente.
     */
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) {
        val task = taskDao.getTaskByIdImmediate(taskId) ?: return
        taskDao.updateTask(task.copy(completed = completed))
    }

    /**
     * Elimina una tarea y todas sus subtareas.
     */
    suspend fun deleteTask(taskId: Long) {

        // Eliminar la alarma asociada a la tarea
        alarmScheduler.cancel(taskId)

        // Eliminar la tarea y todas sus subtareas
        taskDao.deleteTaskById(taskId)
    }

    /**
     * Recibe una lista de tareas y las marca como completadas
     */
    suspend fun completeTasks(taskIds: List<Long>) {
        // Actualizar cada tarea seleccionada como completada
        taskIds.forEach { id ->
            val task = getTask(id) ?: return@forEach
            val updatedTask = task.copy(completed = true)
            updateTask(updatedTask)
        }
    }

    /**
     * Recibe una lista de tareas y las elimina
     */
    suspend fun deleteTasks(taskIds: List<Long>) {
        // Actualizar cada tarea seleccionada como completada
        taskIds.forEach { id ->
            deleteTask(id)
        }
    }


    // ===== Métodos privados auxiliares =====

    /**
     * Construye recursivamente una tarea con todas sus subtareas.
     */
    private suspend fun buildTaskWithSubtasks(entity: TaskEntity): Task {
        val subtasks = taskDao.getSubtasksImmediate(entity.id)
            .map { buildTaskWithSubtasks(it) }

        return entity.toTask().copy(subtasks = subtasks)
    }

    /**
     * Guarda recursivamente una tarea y sus subtareas.
     */
    private suspend fun saveTaskWithSubtasks(task: Task, parentId: Long?): Long {

        Logger.d(
            "IBC-TaskRepository",
            "saveTaskWithSubtasks 1: task=${task.title}, parentId=$parentId"
        )

        val taskEntity = task.toEntity().copy(
            id = if (task.id <= 0) 0 else task.id,
            parentTaskId = parentId
        )

        Logger.d("IBC-TaskRepository", "saveTaskWithSubtasks 2: ->insert taskEntity=$taskEntity")

        val taskId = taskDao.insertTask(taskEntity)
        alarmScheduler.schedule(task.copy(id = taskId))


        Logger.d("IBC-TaskRepository", "saveTaskWithSubtasks 3: <-insert taskId=$taskId")


        // Guardamos recursivamente todas las subtareas
        for (subtask in task.subtasks) {

            Logger.d("IBC-TaskRepository", "saveTaskWithSubtasks 4 (for): subtask=${subtask.title}")

            saveTaskWithSubtasks(subtask, taskId)
        }

        Logger.d("IBC-TaskRepository", "saveTaskWithSubtasks 5: output taskId=$taskId")

        return taskId
    }

    /**
     * Función genérica para procesar cualquier consulta de tareas con opción de incluir jerarquía.
     */
    private fun processTaskQuery(
        query: Flow<List<TaskEntity>>,
        includeHierarchy: Boolean
    ): Flow<List<Task>> = flow {
        query.collect { rootEntities ->
            if (includeHierarchy) {
                // Versión con jerarquía completa
                val tasksWithHierarchy = rootEntities.map { entity ->
                    buildTaskWithSubtasks(entity)
                }

                Logger.d("IBC-TaskRepository", "processTaskQuery: output = $tasksWithHierarchy")

                emit(tasksWithHierarchy)
            } else {
                // Versión plana (solo tareas raíz)
                emit(rootEntities.map { it.toTask() })
            }
        }
    }


    // ===== Funciones de formato de fechas =====

    /**
     * Obtiene la fecha y hora actual en formato compatible con la base de datos.
     */
    private fun getNowDateTime(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    /**
     * Obtiene la fecha actual (sin hora) en formato compatible con la base de datos.
     */
    private fun getNowDate(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    /**
     * Obtiene una fecha y hora a partir de un número de días a partir de la fecha actual.
     * Fecha y hora respecto a la fecha y hora actual (now)
     */
    fun getRelativeDate(days: Int): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val targetDate = today.plusDays(days.toLong())
        return targetDate.atStartOfDay().format(formatter)
    }

}