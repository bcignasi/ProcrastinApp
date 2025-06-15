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
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // ===== Operaciones CRUD básicas =====

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

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
          AND parentTaskId IS NULL
        ORDER BY deadline ASC
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