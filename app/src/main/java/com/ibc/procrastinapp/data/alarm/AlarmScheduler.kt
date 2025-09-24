package com.ibc.procrastinapp.data.alarm

import com.ibc.procrastinapp.data.model.Task
import java.time.LocalDateTime

interface AlarmScheduler {

    fun schedule(task: Task)
    fun cancel(idTask: Long)


}