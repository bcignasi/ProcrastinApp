/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// data/local/AppDatabase.kt
@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN scheduledDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX index_tasks_title_deadline_notify")
                db.execSQL("CREATE UNIQUE INDEX index_tasks_title_deadline_notify_scheduledDate " +
                    "ON tasks(title, deadline, notify, scheduledDate)")
            }
        }
    }
}
