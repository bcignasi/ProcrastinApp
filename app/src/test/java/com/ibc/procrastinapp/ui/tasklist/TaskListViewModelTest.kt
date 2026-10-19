package com.ibc.procrastinapp.ui.tasklist

import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val day = LocalDate.of(2026, 10, 19)
    private var currentDate = day
    private val todayTasks = MutableStateFlow(listOf(Task(id = 1, title = "Hoy")))
    private val overdueTasks = MutableStateFlow(listOf(Task(id = 2, title = "Atrasada")))
    private val pendingTasks = MutableStateFlow(listOf(Task(id = 3, title = "Sin fecha")))
    private val doneTasks = MutableStateFlow(listOf(Task(id = 4, title = "Hecha", completed = true)))
    private val tomorrowTasks = MutableStateFlow(listOf(Task(id = 5, title = "Mañana")))
    private val repository = mockk<TaskRepository>()
    private lateinit var viewModel: TaskListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.getTasks(any(), any()) } answers {
            when (firstArg<TaskListQueryType>()) {
                TaskListQueryType.TODAY -> if (secondArg<LocalDate>() == day) todayTasks else tomorrowTasks
                TaskListQueryType.PAST -> overdueTasks
                TaskListQueryType.ALL -> pendingTasks
                TaskListQueryType.COMPLETED -> doneTasks
                else -> MutableStateFlow(emptyList())
            }
        }
        viewModel = TaskListViewModel(repository) { currentDate }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsWithTodayAndKeepsOverdueInASeparateSection() = runTest(dispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()

        assertEquals(todayTasks.value, viewModel.uiState.value.tasks)
        assertEquals(overdueTasks.value, viewModel.uiState.value.overdueTasks)
        assertEquals(TaskListQueryType.TODAY, viewModel.uiState.value.filter)

        todayTasks.value = emptyList()
        runCurrent()
        assertTrue(viewModel.uiState.value.tasks.isEmpty())
        assertEquals(overdueTasks.value, viewModel.uiState.value.overdueTasks)
    }

    @Test
    fun changingTabsUsesTheChosenListAndClearsHiddenSelections() = runTest(dispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        viewModel.toggleTaskSelection(2)
        runCurrent()
        assertEquals(setOf(2L), viewModel.uiState.value.selectedTaskIds)

        viewModel.setTaskFilter(TaskListQueryType.ALL)
        runCurrent()
        assertEquals(pendingTasks.value, viewModel.uiState.value.tasks)
        assertTrue(viewModel.uiState.value.overdueTasks.isEmpty())
        assertTrue(viewModel.uiState.value.selectedTaskIds.isEmpty())

        viewModel.setTaskFilter(TaskListQueryType.COMPLETED)
        runCurrent()
        assertEquals(doneTasks.value, viewModel.uiState.value.tasks)
        assertEquals(TaskListQueryType.COMPLETED, viewModel.uiState.value.filter)
    }

    @Test
    fun dateAndTodayQueryRefreshAfterMidnightWithoutDatabaseWrites() = runTest(dispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        assertEquals(day, viewModel.uiState.value.date)

        currentDate = day.plusDays(1)
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(currentDate, viewModel.uiState.value.date)
        assertEquals(tomorrowTasks.value, viewModel.uiState.value.tasks)
    }
}
