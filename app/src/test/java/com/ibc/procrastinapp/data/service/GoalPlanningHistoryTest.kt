package com.ibc.procrastinapp.data.service

import android.app.Application
import com.ibc.procrastinapp.data.ai.GoalPlanningPrompt
import com.ibc.procrastinapp.data.ai.Message
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class GoalPlanningHistoryTest {
    @Test
    fun experimentStartsEmptyAndPersistsSeparatelyFromLegacyHistory() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val legacy = MessageStorageImpl(context)
        val previous = listOf(Message.userMessage("Conversación original"))
        legacy.saveMessages(previous)
        val experiment = MessageStorageImpl(context, GoalPlanningPrompt.HISTORY_KEY)

        assertNull(experiment.loadMessages())
        val newConversation = listOf(Message.userMessage("Mi nueva meta"))
        experiment.saveMessages(newConversation)

        assertEquals(previous, legacy.loadMessages())
        assertEquals(newConversation,
            MessageStorageImpl(context, GoalPlanningPrompt.HISTORY_KEY).loadMessages())
    }
}
