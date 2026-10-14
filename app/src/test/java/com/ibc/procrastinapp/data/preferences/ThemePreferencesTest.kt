package com.ibc.procrastinapp.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ThemePreferencesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun closeStores() = runBlocking {
        scopes.forEach { it.coroutineContext.job.cancelAndJoin() }
    }

    @Test
    fun noSelectionKeepsFollowingThePhoneTheme() = runTest {
        val preferences = preferences(File(temporaryFolder.root, "theme.preferences_pb"))

        assertNull(preferences.darkTheme.first())
    }

    @Test
    fun canSwitchFromDarkToLight() = runTest {
        val preferences = preferences(File(temporaryFolder.root, "theme.preferences_pb"))

        preferences.setDarkTheme(true)
        assertEquals(true, preferences.darkTheme.first())

        preferences.setDarkTheme(false)
        assertEquals(false, preferences.darkTheme.first())
    }

    @Test
    fun selectionSurvivesReopeningTheStore() = runTest {
        val file = File(temporaryFolder.root, "theme.preferences_pb")
        for (darkTheme in listOf(true, false)) {
            preferences(file).setDarkTheme(darkTheme)
            scopes.last().coroutineContext.job.cancelAndJoin()

            val reopenedPreferences = preferences(file)
            assertEquals(darkTheme, reopenedPreferences.darkTheme.first())
            scopes.last().coroutineContext.job.cancelAndJoin()
        }
    }

    private fun preferences(file: File): ThemePreferences {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scopes += scope
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        return ThemePreferences(store)
    }
}
