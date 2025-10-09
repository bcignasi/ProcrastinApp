package com.ibc.procrastinapp.data.alarm

import com.ibc.procrastinapp.data.model.Task

interface AlarmScheduler {

    fun schedule(task: Task)
    fun cancel(idTask: Long)


}