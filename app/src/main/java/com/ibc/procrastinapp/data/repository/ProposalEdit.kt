package com.ibc.procrastinapp.data.repository

import com.ibc.procrastinapp.data.model.Task

sealed interface ProjectDestination {
    data object Keep : ProjectDestination
    data object Unassigned : ProjectDestination
    data class Existing(val id: Long, val name: String) : ProjectDestination
    data class Create(val name: String) : ProjectDestination
}

/** Bind draft IDs only to originals in this edit. Keep recorded progress even if a reply omits it. */
internal fun mergeEditedTasks(
    draft: List<Task>,
    originals: List<Task>
): List<Task> {
    val remaining = originals.associateBy { it.id }.toMutableMap()
    val reserved = draft.map { it.id }.toSet()
    val result = draft.map { goal ->
        val original = if (draft.size == 1 && originals.size == 1) originals.single()
            else remaining[goal.id] ?: findOriginal(goal, remaining.values, reserved)
        if (original != null) requireNotNull(remaining.remove(original.id))
        mergeGoal(goal, original)
    }
    return result + remaining.values.mapNotNull { it.completedHistory(emptySet()) }
}

private fun findOriginal(task: Task, siblings: Collection<Task>, reserved: Set<Long>): Task? {
    val matches = siblings.filter { it.title == task.title && it.id !in reserved }
    return matches.singleOrNull { it.scheduledDate == task.scheduledDate } ?: matches.singleOrNull()
}

private fun List<Task>.descendants(): Sequence<Task> = asSequence().flatMap {
    sequenceOf(it) + it.subtasks.descendants()
}

private fun mergeGoal(goal: Task, original: Task?): Task {
    val available = original?.subtasks.orEmpty().descendants().associateBy { it.id }.toMutableMap()
    val references = goal.subtasks.descendants().map { it.id }.filter { it in available }.toList()
    val reserved = references.toSet()
    require(reserved.size == references.size) { "A session appears twice in the draft" }

    fun merge(task: Task, before: Task?): Task {
        val siblings = before?.subtasks.orEmpty()
        val children = task.subtasks.map { child ->
            val match = available[child.id] ?: findOriginal(child, siblings.filter { it.id in available }, reserved)
            if (match != null) available.remove(match.id)
            merge(child, match)
        }
        val history = siblings.filter { it.id in available }.mapNotNull { it.completedHistory(reserved) }
        return task.copy(id = before?.id ?: 0, completed = before?.completed ?: task.completed,
            projectId = before?.projectId, subtasks = children + history)
    }
    return merge(goal, original)
}

private fun Task.completedHistory(reserved: Set<Long>, keepAll: Boolean = false): Task? {
    if (id in reserved) return null
    val keep = completed || keepAll
    val history = subtasks.mapNotNull { it.completedHistory(reserved, keep) }
    return if (keep || history.isNotEmpty()) copy(subtasks = history) else null
}
