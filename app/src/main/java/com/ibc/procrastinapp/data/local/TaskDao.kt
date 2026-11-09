/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM projects ORDER BY name, id")
    fun getProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT id FROM projects WHERE name = :name")
    suspend fun findProjectId(name: String): Long?

    @Query("SELECT name FROM projects WHERE id = :id")
    suspend fun getProjectName(id: Long): String?

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    // Compare only rows with the same scheduling key, following their parents to the project.
    @Query("""
        WITH RECURSIVE lineage(candidate, parentId, project) AS (
            SELECT 0, :parentId, :projectId
            UNION ALL
            SELECT 1, parentTaskId, projectId FROM tasks
            WHERE id != :id AND title = :title AND deadline = :deadline AND notify = :notify AND scheduledDate = :day
            UNION ALL
            SELECT l.candidate, t.parentTaskId, t.projectId FROM tasks t JOIN lineage l ON t.id = l.parentId
        )
        SELECT EXISTS(SELECT 1 FROM lineage WHERE candidate = 1 AND parentId IS NULL
            AND project IS (SELECT project FROM lineage WHERE candidate = 0 AND parentId IS NULL))
    """)
    suspend fun hasMatchingTask(id: Long, title: String, deadline: String, notify: String, day: String,
                                parentId: Long?, projectId: Long?): Boolean

    // ===== Operaciones CRUD básicas =====

    @Transaction
    suspend fun insertTask(task: TaskEntity): Long {
        checkUniqueInProject(task)
        return insertRow(task)
    }

    @Transaction
    suspend fun updateTask(task: TaskEntity) {
        checkUniqueInProject(task)
        updateRow(task)
    }

    private suspend fun checkUniqueInProject(task: TaskEntity) {
        require(task.parentTaskId == null || task.projectId == null)
        require(!hasMatchingTask(task.id, task.title, task.deadline, task.notify, task.scheduledDate,
            task.parentTaskId, task.projectId)) { "Duplicate task in the same project" }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRow(task: TaskEntity): Long

    @Update
    suspend fun updateRow(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    // ===== Consultas básicas =====

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdImmediate(id: Long): TaskEntity?

    // ===== Listas principales =====

    /** Actividades del día a cualquier profundidad; el aviso solo sustituye un día sin planificar. */
    @Query("""
        SELECT * FROM tasks
        WHERE completed = 0
          AND (scheduledDate = :fromDate OR
               (scheduledDate = "" AND notify != "" AND notify >= :fromDate AND notify < :untilDate))
        ORDER BY CASE WHEN notify >= :fromDate AND notify < :untilDate THEN notify ELSE :fromDate END ASC,
                 priority DESC, id ASC
    """)
    fun getTodayTasks(fromDate: String, untilDate: String): Flow<List<TaskEntity>>

    /**
     * TODAS LAS TAREAS PENDIENTES (ALL)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE completed = 0
            AND parentTaskId IS NULL 
        ORDER BY deadline ASC
    """)
    fun getPendingTasks(): Flow<List<TaskEntity>>

    /** Todas las metas, con independencia de su estado; sus hijos se cargan en el repositorio. */
    @Query("SELECT * FROM tasks WHERE parentTaskId IS NULL ORDER BY completed ASC, id ASC")
    fun getGoals(): Flow<List<TaskEntity>>

    /**
     * TAREAS POSTERGADAS (PAST)
     * Todas las pendientes con fecha deadline anterior a la fecha actual
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE completed = 0 
          AND parentTaskId IS NULL
          AND (
              deadline != "" AND deadline < :now
          )
        ORDER BY 
          deadline DESC,
          priority DESC
      """)
    fun getPastTasks(now: String): Flow<List<TaskEntity>>


    /**
     * TAREAS PENDIENTES INMEDIATAS (NOW)
     * Obtiene todas las tareas con término inmediato (hoy, mañana, pasado mañana)
     */
    @Query("""
    SELECT * FROM tasks
    WHERE completed = 0
      AND parentTaskId IS NULL
      AND (
        deadline != "" AND deadline >= :now AND deadline < :pastTomorrow
      )
    ORDER BY 
        COALESCE(deadline, notify) ASC,
        priority DESC
""")
    fun getNowTasks(
        now: String,  // "yyyy-MM-dd HH:mm"
        pastTomorrow: String     // "yyyy-MM-dd HH:mm"
    ): Flow<List<TaskEntity>>


    /**
     * TAREAS PENDIENTES FUTURAS (FUTURE)
     * Obtiene todas las tareas pendientes futuras (fromDate)
     * - Las tareas con fecha límite (deadline o notify) en el futuro
     * - Las tareas sin fecha límite pero con prioridad TODO: esto es discutible. Lo quito por ahora
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE completed = 0 
          AND parentTaskId IS NULL
          AND (
              deadline != "" AND deadline >= :fromDate
          )
        ORDER BY 
          deadline ASC,
          priority DESC
      """)
    fun getFutureTasks(fromDate: String): Flow<List<TaskEntity>>


    /**
     * TAREAS CON PRIORIDAD ALTA O URGENTE (PRIORITY)
      */
    @Query("""
        SELECT * FROM tasks 
        WHERE priority > 1 AND completed = 0 AND parentTaskId IS NULL
        ORDER BY deadline ASC
    """)
    fun getPriorityTasks(): Flow<List<TaskEntity>>



    /**
     * TAREAS CON NOTIFICACIÓN (NOTIFICATION) FUTURAS PENDIENTES
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE completed = 0 
          AND parentTaskId IS NULL
          AND notify != "" AND notify >= :now
          ORDER BY notify ASC
    """)
    fun getNotificationTasks(
        now: String  // "yyyy-MM-dd HH:mm"
    ): Flow<List<TaskEntity>>

    /**
     * TAREAS PENDIENTES SIN FECHA (NO_DATE)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE parentTaskId IS NULL AND deadline = "" AND completed = 0
        ORDER BY priority DESC, title
    """)
    fun getNoDateTasks(): Flow<List<TaskEntity>>


    /**
     * TAREAS COMPLETADAS (COMPLETED)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE completed = 1 
        ORDER BY scheduledDate ASC, deadline ASC, id ASC
    """)
    fun getCompletedTasks(): Flow<List<TaskEntity>>


    // ===== Gestión de tareas jerárquicas =====

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY priority DESC")
    fun getSubtasks(parentId: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE parentTaskId = :parentId
    """)
    suspend fun getSubtasksImmediate(parentId: Long): List<TaskEntity>




}
