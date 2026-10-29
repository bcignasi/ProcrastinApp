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
import kotlinx.coroutines.CancellationException
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
    fun getTasks(
        filter: TaskListQueryType = TaskListQueryType.PAST,
        date: LocalDate = LocalDate.now()
    ): Flow<List<Task>> {
        return when (filter) {
            TaskListQueryType.TODAY -> getTodayTasksFlow(date)
            TaskListQueryType.ALL -> getAllTasksFlow()
            TaskListQueryType.PAST -> getPastTasksFlow(date)
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
    private fun getPastTasksFlow(date: LocalDate, includeHierarchy: Boolean = true): Flow<List<Task>> {
        return processTaskQuery(taskDao.getPastTasks(date.toString()), includeHierarchy)
    }

    /** Filas del día sin adjuntar hijos de otras fechas ni duplicar los que ya coinciden. */
    private fun getTodayTasksFlow(date: LocalDate): Flow<List<Task>> =
        processTaskQuery(
            taskDao.getTodayTasks(
                fromDate = date.toString(),
                untilDate = date.plusDays(1).toString()
            ),
            includeHierarchy = false
        )


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

    /** La edición desde una sesión abre el objetivo completo para conservar su jerarquía. */
    suspend fun getRootTaskIds(taskIds: List<Long>): List<Long> = database.withTransaction {
        taskIds.mapNotNull { id ->
            var entity = taskDao.getTaskByIdImmediate(id) ?: return@mapNotNull null
            val visited = mutableSetOf(entity.id)
            while (entity.parentTaskId != null) {
                val parent = taskDao.getTaskByIdImmediate(entity.parentTaskId) ?: return@mapNotNull null
                if (!visited.add(parent.id)) return@mapNotNull null
                entity = parent
            }
            entity.id
        }.distinct()
    }

    /**
     * Guarda una tarea y todas sus subtareas.
     *
     * @return ID de la tarea guardada, o -1 si falló
     */
    suspend fun saveTask(task: Task): Long {
        val alarms = mutableListOf<Task>()
        val taskId = database.withTransaction {
            insertTaskWithSubtasks(task, null, alarms)
        }
        alarms.forEach(alarmScheduler::schedule)
        return taskId
    }

    /** Reemplaza el conjunto completo o conserva todos los datos originales ante un error. */
    suspend fun saveProposal(originalTaskIds: List<Long>, proposal: List<Task>): ProposalSaveResult {
        require(proposal.isNotEmpty()) { "Cannot save an empty proposal" }
        val removedIds = mutableSetOf<Long>()
        val alarms = mutableListOf<Task>()
        val rootIds = database.withTransaction {
            originalTaskIds.distinct().forEach { id ->
                val entity = requireNotNull(taskDao.getTaskByIdImmediate(id)) { "Original task no longer exists" }
                require(entity.parentTaskId == null) { "Edit the complete goal, not an isolated child" }
                collectTaskIds(buildTaskWithSubtasks(entity), removedIds)
            }
            originalTaskIds.distinct().forEach { taskDao.deleteTaskById(it) }
            proposal.map { insertTaskWithSubtasks(it, null, alarms) }
        }

        // Android no participa en la transacción de Room. Un fallo aquí no significa que los datos no se guardaron.
        var reminderFailures = 0
        fun updateReminder(action: () -> Unit) {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                reminderFailures++
            }
        }
        removedIds.forEach { id -> updateReminder { alarmScheduler.cancel(id) } }
        alarms.filter { it.notify.isNotBlank() && !it.completed }.forEach { task ->
            updateReminder { alarmScheduler.schedule(task) }
        }
        return ProposalSaveResult(rootIds, reminderFailures)
    }

    private fun collectTaskIds(task: Task, ids: MutableSet<Long>) {
        ids.add(task.id)
        task.subtasks.forEach { collectTaskIds(it, ids) }
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
            val alarms = mutableListOf<Task>()
            database.withTransaction {
                taskDao.deleteTaskById(task.id)
                insertTaskWithSubtasks(task, null, alarms)
            }
            alarms.forEach(alarmScheduler::schedule)
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
        deleteTasks(listOf(taskId))
    }

    /**
     * Recibe una lista de tareas y las marca como completadas
     */
    suspend fun completeTasks(taskIds: List<Long>) {
        // No reconstruir subárboles: perderían su parentTaskId al guardarse como raíces.
        taskIds.forEach { id -> setTaskCompleted(id, true) }
    }

    /**
     * Recibe una lista de tareas y las elimina
     */
    suspend fun deleteTasks(taskIds: List<Long>) {
        val removedIds = mutableSetOf<Long>()
        database.withTransaction {
            // Recoger toda la jerarquía antes de que el borrado en cascada la elimine.
            taskIds.distinct().forEach { id ->
                removedIds.add(id)
                taskDao.getTaskByIdImmediate(id)?.let { entity ->
                    collectTaskIds(buildTaskWithSubtasks(entity), removedIds)
                }
            }
            taskIds.distinct().forEach { taskDao.deleteTaskById(it) }
        }
        // Un fallo de base de datos conserva también los avisos originales.
        removedIds.forEach(alarmScheduler::cancel)
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
    private suspend fun insertTaskWithSubtasks(task: Task, parentId: Long?, alarms: MutableList<Task>): Long {
        val taskEntity = task.toEntity().copy(
            id = if (task.id <= 0) 0 else task.id,
            parentTaskId = parentId
        )

        val taskId = taskDao.insertTask(taskEntity)
        alarms.add(task.copy(id = taskId, subtasks = emptyList()))
        for (subtask in task.subtasks) {
            insertTaskWithSubtasks(subtask, taskId, alarms)
        }
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
                // Versión plana: únicamente las entidades que cumplen el filtro.
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

data class ProposalSaveResult(val rootIds: List<Long>, val reminderFailures: Int = 0)
